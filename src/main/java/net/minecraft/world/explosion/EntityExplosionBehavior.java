package net.minecraft.world.explosion;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.util.Optional;

/**
 * Поведение взрыва, привязанное к конкретной сущности-источнику.
 * Делегирует расчёт сопротивления блоков и разрешение разрушения
 * непосредственно сущности, позволяя ей применять собственные модификаторы
 * (например, зачарования или атрибуты).
 */
public class EntityExplosionBehavior extends ExplosionBehavior {

	private final Entity entity;

	public EntityExplosionBehavior(Entity entity) {
		this.entity = entity;
	}

	/**
	 * Вычисляет сопротивление блока взрыву с учётом модификаторов сущности-источника.
	 * Базовое сопротивление передаётся в {@link Entity#getEffectiveExplosionResistance},
	 * которая может его скорректировать (например, через зачарования брони).
	 */
	@Override
	public Optional<Float> getBlastResistance(
			Explosion explosion,
			BlockView world,
			BlockPos pos,
			BlockState blockState,
			FluidState fluidState
	) {
		return super.getBlastResistance(explosion, world, pos, blockState, fluidState)
			.map(resistance -> entity.getEffectiveExplosionResistance(
				explosion,
				world,
				pos,
				blockState,
				fluidState,
				resistance
			));
	}

	@Override
	public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
		return entity.canExplosionDestroyBlock(explosion, world, pos, state, power);
	}
}
