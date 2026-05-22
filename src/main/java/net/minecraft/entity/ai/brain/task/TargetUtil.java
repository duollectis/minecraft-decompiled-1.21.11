package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.LookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Утилитарный класс с вспомогательными методами для задач мозга:
 * управление взглядом, навигацией, проверка видимости и дистанции до цели.
 */
public class TargetUtil {

	private static final int MAX_FIND_ATTEMPTS = 10;

	private TargetUtil() {
	}

	public static void lookAtAndWalkTowardsEachOther(
			LivingEntity first,
			LivingEntity second,
			float speed,
			int walkCompletionRange
	) {
		lookAtEachOther(first, second);
		walkTowardsEachOther(first, second, speed, walkCompletionRange);
	}

	public static boolean canSee(Brain<?> brain, LivingEntity target) {
		Optional<LivingTargetCache> optional = brain.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS);
		return optional.isPresent() && optional.get().contains(target);
	}

	public static boolean canSee(
			Brain<?> brain,
			MemoryModuleType<? extends LivingEntity> memoryModuleType,
			EntityType<?> entityType
	) {
		return canSee(brain, memoryModuleType, entity -> entity.getType() == entityType);
	}

	private static boolean canSee(
			Brain<?> brain,
			MemoryModuleType<? extends LivingEntity> memoryType,
			Predicate<LivingEntity> filter
	) {
		return brain
				.getOptionalRegisteredMemory(memoryType)
				.filter(filter)
				.filter(LivingEntity::isAlive)
				.filter(target -> canSee(brain, target))
				.isPresent();
	}

	private static void lookAtEachOther(LivingEntity first, LivingEntity second) {
		lookAt(first, second);
		lookAt(second, first);
	}

	/**
	 * Записывает в память {@code LOOK_TARGET} сущности цель взгляда на указанный живой объект.
	 * Используется для синхронизации направления взгляда через систему мозга, а не напрямую.
	 */
	public static void lookAt(LivingEntity entity, LivingEntity target) {
		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, new EntityLookTarget(target, true));
	}

	private static void walkTowardsEachOther(
			LivingEntity first,
			LivingEntity second,
			float speed,
			int completionRange
	) {
		walkTowards(first, second, speed, completionRange);
		walkTowards(second, first, speed, completionRange);
	}

	public static void walkTowards(LivingEntity entity, Entity target, float speed, int completionRange) {
		walkTowards(entity, new EntityLookTarget(target, true), speed, completionRange);
	}

	public static void walkTowards(LivingEntity entity, BlockPos target, float speed, int completionRange) {
		walkTowards(entity, new BlockPosLookTarget(target), speed, completionRange);
	}

	public static void walkTowards(LivingEntity entity, LookTarget target, float speed, int completionRange) {
		WalkTarget walkTarget = new WalkTarget(target, speed, completionRange);
		entity.getBrain().remember(MemoryModuleType.LOOK_TARGET, target);
		entity.getBrain().remember(MemoryModuleType.WALK_TARGET, walkTarget);
	}

	public static void give(LivingEntity entity, ItemStack stack, Vec3d targetLocation) {
		Vec3d velocityFactor = new Vec3d(0.3F, 0.3F, 0.3F);
		give(entity, stack, targetLocation, velocityFactor, 0.3F);
	}

	public static void give(
			LivingEntity entity,
			ItemStack stack,
			Vec3d targetLocation,
			Vec3d velocityFactor,
			float yOffset
	) {
		double eyeY = entity.getEyeY() - yOffset;
		ItemEntity itemEntity = new ItemEntity(entity.getEntityWorld(), entity.getX(), eyeY, entity.getZ(), stack);
		itemEntity.setThrower(entity);
		Vec3d velocity = targetLocation.subtract(entity.getEntityPos());
		velocity = velocity.normalize().multiply(velocityFactor.x, velocityFactor.y, velocityFactor.z);
		itemEntity.setVelocity(velocity);
		itemEntity.setToDefaultPickupDelay();
		entity.getEntityWorld().spawnEntity(itemEntity);
	}

	public static ChunkSectionPos getPosClosestToOccupiedPointOfInterest(
			ServerWorld world,
			ChunkSectionPos center,
			int radius
	) {
		int centerDistance = world.getOccupiedPointOfInterestDistance(center);
		return ChunkSectionPos.stream(center, radius)
		                      .filter(sectionPos -> world.getOccupiedPointOfInterestDistance(sectionPos) < centerDistance)
		                      .min(Comparator.comparingInt(world::getOccupiedPointOfInterestDistance))
		                      .orElse(center);
	}

	public static boolean isTargetWithinAttackRange(
			MobEntity mob,
			LivingEntity target,
			int rangedWeaponReachReduction
	) {
		if (mob.getMainHandStack().getItem() instanceof RangedWeaponItem rangedWeaponItem
				&& mob.canUseRangedWeapon(mob.getMainHandStack())) {
			int effectiveRange = rangedWeaponItem.getRange() - rangedWeaponReachReduction;
			return mob.isInRange(target, effectiveRange);
		}

		return mob.isInAttackRange(target);
	}

	public static boolean isNewTargetTooFar(LivingEntity source, LivingEntity target, double extraDistance) {
		Optional<LivingEntity> currentTarget = source.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET);

		if (currentTarget.isEmpty()) {
			return false;
		}

		double currentDistSq = source.squaredDistanceTo(currentTarget.get().getEntityPos());
		double newDistSq = source.squaredDistanceTo(target.getEntityPos());
		return newDistSq > currentDistSq + extraDistance * extraDistance;
	}

	public static boolean isVisibleInMemory(LivingEntity source, LivingEntity target) {
		Brain<?> brain = source.getBrain();

		if (!brain.hasMemoryModule(MemoryModuleType.VISIBLE_MOBS)) {
			return false;
		}

		return brain.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS).get().contains(target);
	}

	public static LivingEntity getCloserEntity(LivingEntity source, Optional<LivingEntity> first, LivingEntity second) {
		return first.isEmpty() ? second : getCloserEntity(source, first.get(), second);
	}

	public static LivingEntity getCloserEntity(LivingEntity source, LivingEntity first, LivingEntity second) {
		Vec3d firstPos = first.getEntityPos();
		Vec3d secondPos = second.getEntityPos();
		return source.squaredDistanceTo(firstPos) < source.squaredDistanceTo(secondPos) ? first : second;
	}

	public static Optional<LivingEntity> getEntity(LivingEntity entity, MemoryModuleType<UUID> uuidMemoryModule) {
		Optional<UUID> uuidOpt = entity.getBrain().getOptionalRegisteredMemory(uuidMemoryModule);
		return uuidOpt.<Entity>map(uuid -> entity.getEntityWorld().getEntity(uuid))
		              .map(target -> target instanceof LivingEntity livingEntity ? livingEntity : null);
	}

	public static @Nullable Vec3d find(PathAwareEntity entity, int horizontalRange, int verticalRange) {
		Vec3d candidate = NoPenaltyTargeting.find(entity, horizontalRange, verticalRange);
		int attempts = 0;

		while (candidate != null
				&& !entity.getEntityWorld().getBlockState(BlockPos.ofFloored(candidate)).canPathfindThrough(NavigationType.WATER)
				&& attempts++ < MAX_FIND_ATTEMPTS
		) {
			candidate = NoPenaltyTargeting.find(entity, horizontalRange, verticalRange);
		}

		return candidate;
	}

	public static boolean hasBreedTarget(LivingEntity entity) {
		return entity.getBrain().hasMemoryModule(MemoryModuleType.BREED_TARGET);
	}
}
