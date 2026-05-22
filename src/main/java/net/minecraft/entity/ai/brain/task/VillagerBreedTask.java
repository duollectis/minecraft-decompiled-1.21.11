package net.minecraft.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Optional;

/**
 * Задача мозга жителя, реализующая процесс размножения.
 * Ищет свободный дом для ребёнка, создаёт дочернего жителя и устанавливает кулдауны размножения родителям.
 */
public class VillagerBreedTask extends MultiTickTask<VillagerEntity> {

	private static final float BREED_WALK_SPEED = 0.5F;
	private static final int BREED_COMPLETION_RANGE = 2;
	private static final int BREED_DURATION_MIN = 275;
	private static final int BREED_DURATION_VARIANCE = 50;
	private static final int PARENT_COOLDOWN_TICKS = 6000;
	private static final int CHILD_COOLDOWN_TICKS = -24000;
	private static final byte STATUS_HEARTS = 18;
	private static final byte STATUS_SMOKE = 12;
	private static final byte STATUS_NO_HOME = 13;
	private static final double MAX_BREED_DIST_SQ = 5.0;
	private static final int HEART_PARTICLE_CHANCE = 35;
	private static final int HOME_SEARCH_RADIUS = 48;

	private long breedEndTime;

	public VillagerBreedTask() {
		super(
				ImmutableMap.of(
						MemoryModuleType.BREED_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.VISIBLE_MOBS,
						MemoryModuleState.VALUE_PRESENT
				),
				350,
				350
		);
	}

	@Override
	protected boolean shouldRun(ServerWorld world, VillagerEntity entity) {
		return isReadyToBreed(entity);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity entity, long time) {
		return time <= breedEndTime && isReadyToBreed(entity);
	}

	@Override
	protected void run(ServerWorld world, VillagerEntity entity, long time) {
		PassiveEntity partner = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.BREED_TARGET).get();
		TargetUtil.lookAtAndWalkTowardsEachOther(entity, partner, BREED_WALK_SPEED, BREED_COMPLETION_RANGE);
		world.sendEntityStatus(partner, STATUS_HEARTS);
		world.sendEntityStatus(entity, STATUS_HEARTS);
		int duration = BREED_DURATION_MIN + entity.getRandom().nextInt(BREED_DURATION_VARIANCE);
		breedEndTime = time + duration;
	}

	@Override
	protected void keepRunning(ServerWorld world, VillagerEntity entity, long time) {
		VillagerEntity partner = (VillagerEntity) entity.getBrain()
		                                                .getOptionalRegisteredMemory(MemoryModuleType.BREED_TARGET)
		                                                .get();

		if (entity.squaredDistanceTo(partner) > MAX_BREED_DIST_SQ) {
			return;
		}

		TargetUtil.lookAtAndWalkTowardsEachOther(entity, partner, BREED_WALK_SPEED, BREED_COMPLETION_RANGE);

		if (time >= breedEndTime) {
			entity.eatForBreeding();
			partner.eatForBreeding();
			goHome(world, entity, partner);
		} else if (entity.getRandom().nextInt(HEART_PARTICLE_CHANCE) == 0) {
			world.sendEntityStatus(partner, STATUS_SMOKE);
			world.sendEntityStatus(entity, STATUS_SMOKE);
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, VillagerEntity entity, long time) {
		entity.getBrain().forget(MemoryModuleType.BREED_TARGET);
	}

	private void goHome(ServerWorld world, VillagerEntity first, VillagerEntity second) {
		Optional<BlockPos> homePos = getReachableHome(world, first);

		if (homePos.isEmpty()) {
			world.sendEntityStatus(second, STATUS_NO_HOME);
			world.sendEntityStatus(first, STATUS_NO_HOME);
			return;
		}

		Optional<VillagerEntity> child = createChild(world, first, second);

		if (child.isPresent()) {
			setChildHome(world, child.get(), homePos.get());
		} else {
			world.getPointOfInterestStorage().releaseTicket(homePos.get());
			world.getSubscriptionTracker().onPoiUpdated(homePos.get());
		}
	}

	private boolean isReadyToBreed(VillagerEntity villager) {
		Brain<VillagerEntity> brain = villager.getBrain();
		Optional<PassiveEntity> breedTarget = brain.getOptionalRegisteredMemory(MemoryModuleType.BREED_TARGET)
		                                           .filter(target -> target.getType() == EntityType.VILLAGER);

		if (breedTarget.isEmpty()) {
			return false;
		}

		return TargetUtil.canSee(brain, MemoryModuleType.BREED_TARGET, EntityType.VILLAGER)
				&& villager.isReadyToBreed()
				&& breedTarget.get().isReadyToBreed();
	}

	private Optional<BlockPos> getReachableHome(ServerWorld world, VillagerEntity villager) {
		return world.getPointOfInterestStorage()
		            .getPosition(
				            poiType -> poiType.matchesKey(PointOfInterestTypes.HOME),
				            (poiType, pos) -> canReachHome(villager, pos, poiType),
				            villager.getBlockPos(),
				            HOME_SEARCH_RADIUS
		            );
	}

	private boolean canReachHome(VillagerEntity villager, BlockPos pos, RegistryEntry<PointOfInterestType> poiType) {
		Path path = villager.getNavigation().findPathTo(pos, poiType.value().searchDistance());
		return path != null && path.reachesTarget();
	}

	private Optional<VillagerEntity> createChild(ServerWorld world, VillagerEntity parent, VillagerEntity partner) {
		VillagerEntity child = parent.createChild(world, partner);

		if (child == null) {
			return Optional.empty();
		}

		parent.setBreedingAge(PARENT_COOLDOWN_TICKS);
		partner.setBreedingAge(PARENT_COOLDOWN_TICKS);
		child.setBreedingAge(CHILD_COOLDOWN_TICKS);
		child.refreshPositionAndAngles(parent.getX(), parent.getY(), parent.getZ(), 0.0F, 0.0F);
		world.spawnEntityAndPassengers(child);
		world.sendEntityStatus(child, STATUS_SMOKE);
		return Optional.of(child);
	}

	private void setChildHome(ServerWorld world, VillagerEntity child, BlockPos pos) {
		child.getBrain().remember(MemoryModuleType.HOME, GlobalPos.create(world.getRegistryKey(), pos));
	}
}
