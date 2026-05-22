package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Цель полёта: ищет позицию над землёй, при неудаче — твёрдую поверхность без штрафа.
 */
public class FlyGoal extends WanderAroundFarGoal {

	private static final int FLY_HORIZONTAL_RANGE = 8;
	private static final int FLY_VERTICAL_RANGE = 7;
	private static final int SOLID_VERTICAL_RANGE = 4;
	private static final int SOLID_VERTICAL_OFFSET = -2;
	private static final float HALF_PI = (float) (Math.PI / 2);

	public FlyGoal(PathAwareEntity mob, double speed) {
		super(mob, speed);
	}

	@Override
	protected @Nullable Vec3d getWanderTarget() {
		Vec3d rotationVec = mob.getRotationVec(0.0F);
		Vec3d aboveGround = AboveGroundTargeting.find(
				mob,
				FLY_HORIZONTAL_RANGE,
				FLY_VERTICAL_RANGE,
				rotationVec.x,
				rotationVec.z,
				HALF_PI,
				3,
				1
		);
		return aboveGround != null
				? aboveGround
				: NoPenaltySolidTargeting.find(
						mob,
						FLY_HORIZONTAL_RANGE,
						SOLID_VERTICAL_RANGE,
						SOLID_VERTICAL_OFFSET,
						rotationVec.x,
						rotationVec.z,
						HALF_PI
				);
	}
}
