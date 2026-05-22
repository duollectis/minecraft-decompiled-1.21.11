package net.minecraft.entity.effect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Утилитарные методы для работы со статусными эффектами.
 *
 * <p>Содержит хелперы для проверки наличия эффектов, получения их параметров
 * и массового применения к игрокам в радиусе.</p>
 */
public final class StatusEffectUtil {

	private StatusEffectUtil() {
	}

	/**
	 * Возвращает локализованный текст длительности эффекта для отображения в UI.
	 *
	 * @param effect     экземпляр эффекта
	 * @param multiplier множитель длительности (например, для замедленного времени)
	 * @param tickRate   количество тиков в секунду
	 * @return текст «∞» для бесконечных эффектов, иначе форматированное время
	 */
	public static Text getDurationText(StatusEffectInstance effect, float multiplier, float tickRate) {
		if (effect.isInfinite()) {
			return Text.translatable("effect.duration.infinite");
		}

		int scaledDuration = MathHelper.floor(effect.getDuration() * multiplier);
		return Text.literal(StringHelper.formatTicks(scaledDuration, tickRate));
	}

	/**
	 * Проверяет, обладает ли сущность каким-либо эффектом ускорения добычи
	 * (Haste или Conduit Power).
	 */
	public static boolean hasHaste(LivingEntity entity) {
		return entity.hasStatusEffect(StatusEffects.HASTE)
				|| entity.hasStatusEffect(StatusEffects.CONDUIT_POWER);
	}

	/**
	 * Возвращает эффективный уровень усиления ускорения добычи.
	 * Учитывает оба источника (Haste и Conduit Power) и берёт максимальный.
	 */
	public static int getHasteAmplifier(LivingEntity entity) {
		int hasteAmplifier = entity.hasStatusEffect(StatusEffects.HASTE)
				? entity.getStatusEffect(StatusEffects.HASTE).getAmplifier()
				: 0;
		int conduitAmplifier = entity.hasStatusEffect(StatusEffects.CONDUIT_POWER)
				? entity.getStatusEffect(StatusEffects.CONDUIT_POWER).getAmplifier()
				: 0;
		return Math.max(hasteAmplifier, conduitAmplifier);
	}

	/**
	 * Проверяет, может ли сущность дышать под водой через любой из трёх эффектов:
	 * Water Breathing, Conduit Power или Breath of the Nautilus.
	 */
	public static boolean hasWaterBreathing(LivingEntity entity) {
		return entity.hasStatusEffect(StatusEffects.WATER_BREATHING)
				|| entity.hasStatusEffect(StatusEffects.CONDUIT_POWER)
				|| entity.hasStatusEffect(StatusEffects.BREATH_OF_THE_NAUTILUS);
	}

	/**
	 * Определяет, может ли сущность восстанавливать воздух на суше.
	 *
	 * <p>Breath of the Nautilus блокирует восстановление воздуха на суше,
	 * если только не активен Water Breathing или Conduit Power.</p>
	 */
	public static boolean canIncreaseAirOnLand(LivingEntity entity) {
		return !entity.hasStatusEffect(StatusEffects.BREATH_OF_THE_NAUTILUS)
				|| entity.hasStatusEffect(StatusEffects.WATER_BREATHING)
				|| entity.hasStatusEffect(StatusEffects.CONDUIT_POWER);
	}

	/**
	 * Применяет статусный эффект ко всем подходящим игрокам в радиусе от точки.
	 *
	 * <p>Игрок считается подходящим, если он:
	 * <ul>
	 *   <li>находится в режиме выживания;</li>
	 *   <li>не является союзником источника эффекта;</li>
	 *   <li>находится в пределах {@code range} блоков от {@code origin};</li>
	 *   <li>не имеет эффекта того же типа с равным или большим уровнем и достаточной длительностью.</li>
	 * </ul>
	 * </p>
	 *
	 * @param world                серверный мир
	 * @param entity               источник эффекта (союзники исключаются), может быть {@code null}
	 * @param origin               центр области применения
	 * @param range                радиус применения в блоках
	 * @param statusEffectInstance применяемый эффект (копируется для каждого игрока)
	 * @param duration             минимальная длительность для перезаписи существующего эффекта
	 * @return список игроков, которым был применён эффект
	 */
	public static List<ServerPlayerEntity> addEffectToPlayersWithinDistance(
			ServerWorld world,
			@Nullable Entity entity,
			Vec3d origin,
			double range,
			StatusEffectInstance statusEffectInstance,
			int duration
	) {
		RegistryEntry<StatusEffect> effectType = statusEffectInstance.getEffectType();
		List<ServerPlayerEntity> affected = world.getPlayers(
				player -> player.interactionManager.isSurvivalLike()
						&& (entity == null || !entity.isTeammate(player))
						&& origin.isInRange(player.getEntityPos(), range)
						&& (
						!player.hasStatusEffect(effectType)
								|| player.getStatusEffect(effectType).getAmplifier() < statusEffectInstance.getAmplifier()
								|| player.getStatusEffect(effectType).isDurationBelow(duration - 1)
				)
		);
		affected.forEach(player -> player.addStatusEffect(new StatusEffectInstance(statusEffectInstance), entity));
		return affected;
	}
}
