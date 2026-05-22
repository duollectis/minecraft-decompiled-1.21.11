package net.minecraft.entity.damage;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Источник урона: инкапсулирует тип урона, атакующую сущность, снаряд-посредник
 * и опциональную позицию. Используется для формирования сообщений о смерти,
 * расчёта масштабирования по сложности и применения эффектов.
 */
public class DamageSource {

	private final RegistryEntry<DamageType> type;
	private final @Nullable Entity attacker;
	private final @Nullable Entity source;
	private final @Nullable Vec3d position;

	public DamageSource(RegistryEntry<DamageType> type, @Nullable Entity source, @Nullable Entity attacker) {
		this(type, source, attacker, null);
	}

	public DamageSource(RegistryEntry<DamageType> type, Vec3d position) {
		this(type, null, null, position);
	}

	public DamageSource(RegistryEntry<DamageType> type, @Nullable Entity attacker) {
		this(type, attacker, attacker);
	}

	public DamageSource(RegistryEntry<DamageType> type) {
		this(type, null, null, null);
	}

	private DamageSource(
		RegistryEntry<DamageType> type,
		@Nullable Entity source,
		@Nullable Entity attacker,
		@Nullable Vec3d position
	) {
		this.type = type;
		this.attacker = attacker;
		this.source = source;
		this.position = position;
	}

	@Override
	public String toString() {
		return "DamageSource (" + getType().msgId() + ")";
	}

	public float getExhaustion() {
		return getType().exhaustion();
	}

	public boolean isDirect() {
		return attacker == source;
	}

	public @Nullable Entity getSource() {
		return source;
	}

	public @Nullable Entity getAttacker() {
		return attacker;
	}

	public @Nullable ItemStack getWeaponStack() {
		return source != null ? source.getWeaponStack() : null;
	}

	/**
	 * Формирует локализованное сообщение о смерти с учётом атакующего,
	 * оружия с кастомным именем и наличия последнего противника.
	 *
	 * @param killed погибшая сущность
	 * @return текст сообщения о смерти
	 */
	public Text getDeathMessage(LivingEntity killed) {
		String baseKey = "death.attack." + getType().msgId();

		if (attacker == null && source == null) {
			LivingEntity primeAdversary = killed.getPrimeAdversary();
			return primeAdversary != null
				? Text.translatable(baseKey + ".player", killed.getDisplayName(), primeAdversary.getDisplayName())
				: Text.translatable(baseKey, killed.getDisplayName());
		}

		Text attackerName = attacker == null ? source.getDisplayName() : attacker.getDisplayName();
		ItemStack weapon = attacker instanceof LivingEntity living ? living.getMainHandStack() : ItemStack.EMPTY;

		return !weapon.isEmpty() && weapon.contains(DataComponentTypes.CUSTOM_NAME)
			? Text.translatable(baseKey + ".item", killed.getDisplayName(), attackerName, weapon.toHoverableText())
			: Text.translatable(baseKey, killed.getDisplayName(), attackerName);
	}

	public String getName() {
		return getType().msgId();
	}

	public boolean isScaledWithDifficulty() {
		return switch (getType().scaling()) {
			case NEVER -> false;
			case WHEN_CAUSED_BY_LIVING_NON_PLAYER ->
				attacker instanceof LivingEntity && !(attacker instanceof PlayerEntity);
			case ALWAYS -> true;
		};
	}

	public boolean isSourceCreativePlayer() {
		return getAttacker() instanceof PlayerEntity player && player.getAbilities().creativeMode;
	}

	/**
	 * Возвращает позицию источника урона: сначала проверяется явно заданная позиция,
	 * затем позиция сущности-снаряда, иначе {@code null}.
	 *
	 * @return позиция или {@code null}
	 */
	public @Nullable Vec3d getPosition() {
		return position != null
			? position
			: (source != null ? source.getEntityPos() : null);
	}

	public @Nullable Vec3d getStoredPosition() {
		return position;
	}

	public boolean isIn(TagKey<DamageType> tag) {
		return type.isIn(tag);
	}

	public boolean isOf(RegistryKey<DamageType> typeKey) {
		return type.matchesKey(typeKey);
	}

	public DamageType getType() {
		return type.value();
	}

	public RegistryEntry<DamageType> getTypeRegistryEntry() {
		return type;
	}
}
