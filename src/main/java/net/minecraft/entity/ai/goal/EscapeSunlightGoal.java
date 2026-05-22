package net.minecraft.entity.ai.goal;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель побега от солнечного света: ищет затенённую позицию, если моб горит под открытым небом.
 * Не активируется при наличии шлема или активной цели атаки.
 */
public class EscapeSunlightGoal extends Goal {

	private static final int SHADE_SEARCH_ATTEMPTS = 10;
	private static final int SHADE_HORIZONTAL_RANGE = 10;
	private static final int SHADE_VERTICAL_RANGE = 3;

	protected final PathAwareEntity mob;
	private double targetX;
	private double targetY;
	private double targetZ;
	private final double speed;
	private final World world;

	public EscapeSunlightGoal(PathAwareEntity mob, double speed) {
		this.mob = mob;
		this.speed = speed;
		this.world = mob.getEntityWorld();
		setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (mob.getTarget() != null) {
			return false;
		}

		if (!world.isDay()) {
			return false;
		}

		if (!mob.isOnFire()) {
			return false;
		}

		if (!world.isSkyVisible(mob.getBlockPos())) {
			return false;
		}

		if (!mob.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
			return false;
		}

		return targetShadedPos();
	}

	protected boolean targetShadedPos() {
		Vec3d shadePos = locateShadedPos();
		if (shadePos == null) {
			return false;
		}

		targetX = shadePos.x;
		targetY = shadePos.y;
		targetZ = shadePos.z;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return !mob.getNavigation().isIdle();
	}

	@Override
	public void start() {
		mob.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
	}

	protected @Nullable Vec3d locateShadedPos() {
		Random random = mob.getRandom();
		BlockPos origin = mob.getBlockPos();

		for (int attempt = 0; attempt < SHADE_SEARCH_ATTEMPTS; attempt++) {
			BlockPos candidate = origin.add(
					random.nextInt(20) - SHADE_HORIZONTAL_RANGE,
					random.nextInt(6) - SHADE_VERTICAL_RANGE,
					random.nextInt(20) - SHADE_HORIZONTAL_RANGE
			);
			if (!world.isSkyVisible(candidate) && mob.getPathfindingFavor(candidate) < 0.0F) {
				return Vec3d.ofBottomCenter(candidate);
			}
		}

		return null;
	}
}
