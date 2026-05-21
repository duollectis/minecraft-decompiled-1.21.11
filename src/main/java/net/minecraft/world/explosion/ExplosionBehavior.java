package net.minecraft.world.explosion;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import java.util.Optional;

/**
 * {@code ExplosionBehavior}.
 */
public class ExplosionBehavior {

	public Optional<Float> getBlastResistance(
			Explosion explosion,
			BlockView world,
			BlockPos pos,
			BlockState blockState,
			FluidState fluidState
	) {
		return blockState.isAir() && fluidState.isEmpty()
		       ? Optional.empty()
		       : Optional.of(Math.max(blockState.getBlock().getBlastResistance(), fluidState.getBlastResistance()));
	}

	/**
	 * Проверяет возможность destroy block.
	 *
	 * @param explosion explosion
	 * @param world world
	 * @param pos pos
	 * @param state state
	 * @param power power
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
		return true;
	}

	/**
	 * Определяет, следует ли damage.
	 *
	 * @param explosion explosion
	 * @param entity entity
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldDamage(Explosion explosion, Entity entity) {
		return true;
	}

	public float getKnockbackModifier(Entity entity) {
		return 1.0F;
	}

	/**
	 * Вычисляет damage.
	 *
	 * @param explosion explosion
	 * @param entity entity
	 * @param amount amount
	 *
	 * @return float — результат операции
	 */
	public float calculateDamage(Explosion explosion, Entity entity, float amount) {
		float f = explosion.getPower() * 2.0F;
		Vec3d vec3d = explosion.getPosition();
		double d = Math.sqrt(entity.squaredDistanceTo(vec3d)) / f;
		double e = (1.0 - d) * amount;
		return (float) ((e * e + e) / 2.0 * 7.0 * f + 1.0);
	}
}
