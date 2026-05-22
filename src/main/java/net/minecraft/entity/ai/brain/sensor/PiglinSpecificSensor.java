package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сенсор специфической логики пиглина.
 * Собирает данные о ближайших врагах, хоглинах, игроках без золота, держателях золота,
 * зомбифицированных пиглинах и отпугивателях (душевые костры и т.д.).
 */
public class PiglinSpecificSensor extends Sensor<LivingEntity> {

	private static final int REPELLENT_SEARCH_RADIUS_XZ = 8;
	private static final int REPELLENT_SEARCH_RADIUS_Y = 4;

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(
				MemoryModuleType.VISIBLE_MOBS,
				MemoryModuleType.MOBS,
				MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
				MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD,
				MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM,
				MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN,
				MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
				MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,
				MemoryModuleType.NEARBY_ADULT_PIGLINS,
				MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,
				MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,
				MemoryModuleType.NEAREST_REPELLENT
		);
	}

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
		Brain<?> brain = entity.getBrain();
		brain.remember(MemoryModuleType.NEAREST_REPELLENT, findPiglinRepellent(world, entity));

		Optional<MobEntity> nearestNemesis = Optional.empty();
		Optional<HoglinEntity> huntableHoglin = Optional.empty();
		Optional<HoglinEntity> babyHoglin = Optional.empty();
		Optional<PiglinEntity> babyPiglin = Optional.empty();
		Optional<LivingEntity> zombified = Optional.empty();
		Optional<PlayerEntity> targetablePlayer = Optional.empty();
		Optional<PlayerEntity> goldHolder = Optional.empty();
		int adultHoglinCount = 0;
		List<AbstractPiglinEntity> visibleAdultPiglins = new ArrayList<>();
		List<AbstractPiglinEntity> nearbyAdultPiglins = new ArrayList<>();
		LivingTargetCache visibleMobs = brain
				.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
				.orElse(LivingTargetCache.empty());

		for (LivingEntity mob : visibleMobs.iterate(m -> true)) {
			if (mob instanceof HoglinEntity hoglin) {
				if (hoglin.isBaby() && babyHoglin.isEmpty()) {
					babyHoglin = Optional.of(hoglin);
				} else if (hoglin.isAdult()) {
					adultHoglinCount++;

					if (huntableHoglin.isEmpty() && hoglin.canBeHunted()) {
						huntableHoglin = Optional.of(hoglin);
					}
				}
			} else if (mob instanceof PiglinBruteEntity brute) {
				visibleAdultPiglins.add(brute);
			} else if (mob instanceof PiglinEntity piglin) {
				if (piglin.isBaby() && babyPiglin.isEmpty()) {
					babyPiglin = Optional.of(piglin);
				} else if (piglin.isAdult()) {
					visibleAdultPiglins.add(piglin);
				}
			} else if (mob instanceof PlayerEntity player) {
				if (targetablePlayer.isEmpty() && !PiglinBrain.isWearingPiglinSafeArmor(player) && entity.canTarget(mob)) {
					targetablePlayer = Optional.of(player);
				}

				if (goldHolder.isEmpty() && !player.isSpectator() && PiglinBrain.isGoldHoldingPlayer(player)) {
					goldHolder = Optional.of(player);
				}
			} else if (nearestNemesis.isEmpty()
					&& (mob instanceof WitherSkeletonEntity || mob instanceof WitherEntity)) {
				nearestNemesis = Optional.of((MobEntity) mob);
			} else if (zombified.isEmpty() && PiglinBrain.isZombified(mob.getType())) {
				zombified = Optional.of(mob);
			}
		}

		for (LivingEntity mob : brain.getOptionalRegisteredMemory(MemoryModuleType.MOBS).orElse(ImmutableList.of())) {
			if (mob instanceof AbstractPiglinEntity piglin && piglin.isAdult()) {
				nearbyAdultPiglins.add(piglin);
			}
		}

		brain.remember(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, nearestNemesis);
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, huntableHoglin);
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, babyHoglin);
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, zombified);
		brain.remember(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, targetablePlayer);
		brain.remember(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, goldHolder);
		brain.remember(MemoryModuleType.NEARBY_ADULT_PIGLINS, nearbyAdultPiglins);
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, visibleAdultPiglins);
		brain.remember(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, visibleAdultPiglins.size());
		brain.remember(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, adultHoglinCount);
	}

	private static Optional<BlockPos> findPiglinRepellent(ServerWorld world, LivingEntity entity) {
		return BlockPos.findClosest(
				entity.getBlockPos(),
				REPELLENT_SEARCH_RADIUS_XZ,
				REPELLENT_SEARCH_RADIUS_Y,
				pos -> isPiglinRepellent(world, pos)
		);
	}

	private static boolean isPiglinRepellent(ServerWorld world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		boolean isRepellent = state.isIn(BlockTags.PIGLIN_REPELLENTS);
		return isRepellent && state.isOf(Blocks.SOUL_CAMPFIRE)
				? CampfireBlock.isLitCampfire(state)
				: isRepellent;
	}
}
