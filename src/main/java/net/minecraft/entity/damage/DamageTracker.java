package net.minecraft.entity.damage;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Urls;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Отслеживает историю урона, нанесённого живой сущности, для формирования
 * корректного сообщения о смерти. Хранит записи о последних событиях урона
 * в пределах кулдауна и определяет наиболее значимое падение.
 */
public class DamageTracker {

	public static final int DAMAGE_COOLDOWN = 100;
	public static final int ATTACK_DAMAGE_COOLDOWN = 300;

	private static final Style INTENTIONAL_GAME_DESIGN_LINK_STYLE = Style.EMPTY
		.withClickEvent(new ClickEvent.OpenUrl(Urls.INTENTIONAL_GAME_DESIGN_ISSUE))
		.withHoverEvent(new HoverEvent.ShowText(Text.literal("MCPE-28723")));

	private final List<DamageRecord> recentDamage = new ArrayList<>();
	private final LivingEntity entity;

	private int ageOnLastDamage;
	private int ageOnLastAttacked;
	private int ageOnLastUpdate;
	private boolean recentlyAttacked;
	private boolean hasDamage;

	public DamageTracker(LivingEntity entity) {
		this.entity = entity;
	}

	/**
	 * Регистрирует событие урона: добавляет запись в историю, обновляет таймеры
	 * и при необходимости инициирует боевой режим сущности.
	 *
	 * @param damageSource источник урона
	 * @param damage количество нанесённого урона
	 */
	public void onDamage(DamageSource damageSource, float damage) {
		update();

		FallLocation fallLocation = FallLocation.fromEntity(entity);
		DamageRecord record = new DamageRecord(damageSource, damage, fallLocation, (float) entity.fallDistance);
		recentDamage.add(record);
		ageOnLastDamage = entity.age;
		hasDamage = true;

		if (recentlyAttacked || !entity.isAlive() || !isAttackerLiving(damageSource)) {
			return;
		}

		recentlyAttacked = true;
		ageOnLastAttacked = entity.age;
		ageOnLastUpdate = ageOnLastAttacked;
		entity.enterCombat();
	}

	/**
	 * Формирует локализованное сообщение о смерти с учётом типа последнего урона,
	 * наиболее значимого падения и специальных случаев (intentional game design).
	 *
	 * @return текст сообщения о смерти
	 */
	public Text getDeathMessage() {
		if (recentDamage.isEmpty()) {
			return Text.translatable("death.attack.generic", entity.getDisplayName());
		}

		DamageRecord lastRecord = recentDamage.get(recentDamage.size() - 1);
		DamageSource lastSource = lastRecord.damageSource();
		DamageRecord biggestFall = getBiggestFall();
		DeathMessageType messageType = lastSource.getType().deathMessageType();

		if (messageType == DeathMessageType.FALL_VARIANTS && biggestFall != null) {
			return getFallDeathMessage(biggestFall, lastSource.getAttacker());
		}

		if (messageType == DeathMessageType.INTENTIONAL_GAME_DESIGN) {
			String key = "death.attack." + lastSource.getName();
			Text link = Texts
				.bracketed(Text.translatable(key + ".link"))
				.fillStyle(INTENTIONAL_GAME_DESIGN_LINK_STYLE);
			return Text.translatable(key + ".message", entity.getDisplayName(), link);
		}

		return lastSource.getDeathMessage(entity);
	}

	public int getTimeSinceLastAttack() {
		return recentlyAttacked
			? entity.age - ageOnLastAttacked
			: ageOnLastUpdate - ageOnLastAttacked;
	}

	/**
	 * Сбрасывает историю урона, если истёк кулдаун. Завершает боевой режим,
	 * если сущность была атакована живым существом.
	 */
	public void update() {
		int cooldown = recentlyAttacked ? ATTACK_DAMAGE_COOLDOWN : DAMAGE_COOLDOWN;
		if (!hasDamage || (entity.isAlive() && entity.age - ageOnLastDamage <= cooldown)) {
			return;
		}

		boolean wasAttacked = recentlyAttacked;
		hasDamage = false;
		recentlyAttacked = false;
		ageOnLastUpdate = entity.age;

		if (wasAttacked) {
			entity.endCombat();
		}

		recentDamage.clear();
	}

	private Text getFallDeathMessage(DamageRecord damageRecord, @Nullable Entity attacker) {
		DamageSource source = damageRecord.damageSource();

		if (source.isIn(DamageTypeTags.IS_FALL) || source.isIn(DamageTypeTags.ALWAYS_MOST_SIGNIFICANT_FALL)) {
			FallLocation fallLocation = Objects.requireNonNullElse(damageRecord.fallLocation(), FallLocation.GENERIC);
			return Text.translatable(fallLocation.getDeathMessageKey(), entity.getDisplayName());
		}

		Text attackerName = getDisplayName(attacker);
		Entity sourceAttacker = source.getAttacker();
		Text sourceAttackerName = getDisplayName(sourceAttacker);

		if (sourceAttackerName != null && !sourceAttackerName.equals(attackerName)) {
			return getAttackedFallDeathMessage(
				sourceAttacker,
				sourceAttackerName,
				"death.fell.assist.item",
				"death.fell.assist"
			);
		}

		return attackerName != null
			? getAttackedFallDeathMessage(attacker, attackerName, "death.fell.finish.item", "death.fell.finish")
			: Text.translatable("death.fell.killer", entity.getDisplayName());
	}

	private Text getAttackedFallDeathMessage(
		Entity attacker,
		Text attackerDisplayName,
		String itemDeathKey,
		String deathKey
	) {
		ItemStack weapon = attacker instanceof LivingEntity living ? living.getMainHandStack() : ItemStack.EMPTY;

		return !weapon.isEmpty() && weapon.contains(DataComponentTypes.CUSTOM_NAME)
			? Text.translatable(itemDeathKey, entity.getDisplayName(), attackerDisplayName, weapon.toHoverableText())
			: Text.translatable(deathKey, entity.getDisplayName(), attackerDisplayName);
	}

	/**
	 * Находит наиболее значимую запись о падении из истории урона.
	 * Приоритет отдаётся записи с наибольшей дистанцией падения (порог 5.0),
	 * затем — записи с наибольшим уроном при наличии места падения.
	 *
	 * @return наиболее значимая запись о падении или {@code null}
	 */
	private @Nullable DamageRecord getBiggestFall() {
		DamageRecord biggestFallRecord = null;
		DamageRecord biggestDamageRecord = null;
		float maxFallDistance = 0.0F;
		float maxDamage = 0.0F;

		for (int i = 0; i < recentDamage.size(); i++) {
			DamageRecord current = recentDamage.get(i);
			DamageRecord previous = i > 0 ? recentDamage.get(i - 1) : null;
			DamageSource source = current.damageSource();

			boolean alwaysSignificant = source.isIn(DamageTypeTags.ALWAYS_MOST_SIGNIFICANT_FALL);
			float fallDistance = alwaysSignificant ? Float.MAX_VALUE : current.fallDistance();

			if ((source.isIn(DamageTypeTags.IS_FALL) || alwaysSignificant)
				&& fallDistance > 0.0F
				&& (biggestFallRecord == null || fallDistance > maxFallDistance)
			) {
				biggestFallRecord = i > 0 ? previous : current;
				maxFallDistance = fallDistance;
			}

			if (current.fallLocation() != null && (biggestDamageRecord == null || current.damage() > maxDamage)) {
				biggestDamageRecord = current;
				maxDamage = current.damage();
			}
		}

		if (maxFallDistance > 5.0F && biggestFallRecord != null) {
			return biggestFallRecord;
		}

		return maxDamage > 5.0F && biggestDamageRecord != null ? biggestDamageRecord : null;
	}

	private static boolean isAttackerLiving(DamageSource damageSource) {
		return damageSource.getAttacker() instanceof LivingEntity;
	}

	private static @Nullable Text getDisplayName(@Nullable Entity entity) {
		return entity == null ? null : entity.getDisplayName();
	}
}
