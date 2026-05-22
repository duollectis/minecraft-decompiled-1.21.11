package net.minecraft.entity.ai.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Function;

/**
 * Цель побега от опасности: при получении урона из опасного источника ищет
 * безопасную позицию или ближайшую воду (если моб горит).
 */
public class EscapeDangerGoal extends Goal {

	public static final int RANGE_Y = 1;

	protected final PathAwareEntity mob;
	protected final double speed;
	protected double targetX;
	protected double targetY;
	protected double targetZ;
	protected boolean active;
	private final Function<PathAwareEntity, TagKey<DamageType>> entityToDangerousDamageTypes;

	public EscapeDangerGoal(PathAwareEntity mob, double speed) {
		this(mob, speed, DamageTypeTags.PANIC_CAUSES);
	}

	public EscapeDangerGoal(PathAwareEntity mob, double speed, TagKey<DamageType> dangerousDamageTypes) {
		this(mob, speed, entity -> dangerousDamageTypes);
	}

	public EscapeDangerGoal(
			PathAwareEntity mob,
			double speed,
			Function<PathAwareEntity, TagKey<DamageType>> entityToDangerousDamageTypes
	) {
		this.mob = mob;
		this.speed = speed;
		this.entityToDangerousDamageTypes = entityToDangerousDamageTypes;
		setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (!isInDanger()) {
			return false;
		}

		if (mob.isOnFire()) {
			BlockPos waterPos = locateClosestWater(mob.getEntityWorld(), mob, 5);
			if (waterPos != null) {
				targetX = waterPos.getX();
				targetY = waterPos.getY();
				targetZ = waterPos.getZ();
				return true;
			}
		}

		return findTarget();
	}

	protected boolean isInDanger() {
		return mob.getRecentDamageSource() != null
				&& mob.getRecentDamageSource().isIn(entityToDangerousDamageTypes.apply(mob));
	}

	protected boolean findTarget() {
		Vec3d pos = NoPenaltyTargeting.find(mob, 5, 4);
		if (pos == null) {
			return false;
		}

		targetX = pos.x;
		targetY = pos.y;
		targetZ = pos.z;
		return true;
	}

	public boolean isActive() {
		return active;
	}

	@Override
	public void start() {
		mob.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
		active = true;
	}

	@Override
	public void stop() {
		active = false;
	}

	@Override
	public boolean shouldContinue() {
		return !mob.getNavigation().isIdle();
	}

	protected @Nullable BlockPos locateClosestWater(BlockView world, Entity entity, int rangeX) {
		BlockPos blockPos = entity.getBlockPos();
		return world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty()
				? BlockPos.findClosest(entity.getBlockPos(), rangeX, 1, pos -> world.getFluidState(pos).isIn(FluidTags.WATER))
						.orElse(null)
				: null;
	}
}
