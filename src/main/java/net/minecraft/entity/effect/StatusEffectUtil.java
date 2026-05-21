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
 * {@code StatusEffectUtil}.
 */
public final class StatusEffectUtil {

	public static Text getDurationText(StatusEffectInstance effect, float multiplier, float tickRate) {
		if (effect.isInfinite()) {
			return Text.translatable("effect.duration.infinite");
		}
		else {
			int i = MathHelper.floor(effect.getDuration() * multiplier);
			return Text.literal(StringHelper.formatTicks(i, tickRate));
		}
	}

	public static boolean hasHaste(LivingEntity entity) {
		return entity.hasStatusEffect(StatusEffects.HASTE) || entity.hasStatusEffect(StatusEffects.CONDUIT_POWER);
	}

	public static int getHasteAmplifier(LivingEntity entity) {
		int i = 0;
		int j = 0;
		if (entity.hasStatusEffect(StatusEffects.HASTE)) {
			i = entity.getStatusEffect(StatusEffects.HASTE).getAmplifier();
		}

		if (entity.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
			j = entity.getStatusEffect(StatusEffects.CONDUIT_POWER).getAmplifier();
		}

		return Math.max(i, j);
	}

	public static boolean hasWaterBreathing(LivingEntity entity) {
		return entity.hasStatusEffect(StatusEffects.WATER_BREATHING)
				|| entity.hasStatusEffect(StatusEffects.CONDUIT_POWER)
				|| entity.hasStatusEffect(StatusEffects.BREATH_OF_THE_NAUTILUS);
	}

	/**
	 * Проверяет возможность increase air on land.
	 *
	 * @param entity entity
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public static boolean canIncreaseAirOnLand(LivingEntity entity) {
		return !entity.hasStatusEffect(StatusEffects.BREATH_OF_THE_NAUTILUS)
				|| entity.hasStatusEffect(StatusEffects.WATER_BREATHING)
				|| entity.hasStatusEffect(StatusEffects.CONDUIT_POWER);
	}

	public static List<ServerPlayerEntity> addEffectToPlayersWithinDistance(
			ServerWorld world,
			@Nullable Entity entity,
			Vec3d origin,
			double range,
			StatusEffectInstance statusEffectInstance,
			int duration
	) {
		RegistryEntry<StatusEffect> registryEntry = statusEffectInstance.getEffectType();
		List<ServerPlayerEntity> list = world.getPlayers(
				player -> player.interactionManager.isSurvivalLike()
						&& (entity == null || !entity.isTeammate(player))
						&& origin.isInRange(player.getEntityPos(), range)
						&& (
						!player.hasStatusEffect(registryEntry)
								|| player.getStatusEffect(registryEntry).getAmplifier()
								< statusEffectInstance.getAmplifier()
								|| player.getStatusEffect(registryEntry).isDurationBelow(duration - 1)
				)
		);
		list.forEach(player -> player.addStatusEffect(new StatusEffectInstance(statusEffectInstance), entity));
		return list;
	}
}
