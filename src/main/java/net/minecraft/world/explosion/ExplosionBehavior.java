package net.minecraft.world.explosion;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import java.util.Optional;

/**
 * Базовое поведение взрыва, определяющее правила разрушения блоков,
 * урона сущностям и отбрасывания. Может быть переопределено для
 * специализированных типов взрывов.
 */
public class ExplosionBehavior {

	/**
	 * Возвращает сопротивление блока взрыву в данной позиции.
	 * Если блок является воздухом и жидкость отсутствует — возвращает {@link Optional#empty()},
	 * что означает полное отсутствие сопротивления (луч проходит насквозь).
	 *
	 * @param explosion  текущий взрыв
	 * @param world      мир, в котором происходит взрыв
	 * @param pos        позиция проверяемого блока
	 * @param blockState состояние блока
	 * @param fluidState состояние жидкости в блоке
	 * @return сопротивление взрыву, или {@link Optional#empty()} если блок не сопротивляется
	 */
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
	 * Определяет, может ли взрыв разрушить блок в данной позиции.
	 *
	 * @param explosion текущий взрыв
	 * @param world     мир, в котором происходит взрыв
	 * @param pos       позиция блока
	 * @param state     состояние блока
	 * @param power     оставшаяся мощность луча взрыва в данной точке
	 * @return {@code true} если блок должен быть разрушен
	 */
	public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
		return true;
	}

	/**
	 * Определяет, должна ли сущность получить урон от взрыва.
	 *
	 * @param explosion текущий взрыв
	 * @param entity    проверяемая сущность
	 * @return {@code true} если сущность должна получить урон
	 */
	public boolean shouldDamage(Explosion explosion, Entity entity) {
		return true;
	}

	/**
	 * Возвращает множитель отбрасывания для сущности.
	 * Значение {@code 1.0} означает стандартное отбрасывание.
	 *
	 * @param entity сущность, для которой вычисляется множитель
	 * @return множитель отбрасывания
	 */
	public float getKnockbackModifier(Entity entity) {
		return 1.0F;
	}

	/**
	 * Вычисляет итоговый урон от взрыва для сущности с учётом расстояния
	 * и коэффициента полученного урона (exposure).
	 * <p>
	 * Формула: {@code ((exposure * (1 - dist) + exposure²) / 2) * 7 * diameter + 1},
	 * где {@code diameter = power * 2}.
	 *
	 * @param explosion текущий взрыв
	 * @param entity    сущность, получающая урон
	 * @param exposure  коэффициент воздействия взрыва (0.0–1.0), вычисленный через рейкаст
	 * @return итоговый урон
	 */
	public float calculateDamage(Explosion explosion, Entity entity, float exposure) {
		float diameter = explosion.getPower() * 2.0F;
		Vec3d explosionPos = explosion.getPosition();
		double distance = Math.sqrt(entity.squaredDistanceTo(explosionPos)) / diameter;
		double normalizedExposure = (1.0 - distance) * exposure;
		return (float) ((normalizedExposure * normalizedExposure + normalizedExposure) / 2.0 * 7.0 * diameter + 1.0);
	}
}
