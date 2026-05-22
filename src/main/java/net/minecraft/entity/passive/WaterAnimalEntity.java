package net.minecraft.entity.passive;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Базовый класс для водных пассивных существ (рыбы, кальмары и т.д.).
 * Реализует логику дыхания: существо тонет вне воды и восстанавливает воздух в воде.
 */
public abstract class WaterAnimalEntity extends PassiveEntity {

	/** Глубина спавна относительно уровня моря (от -13 до 0 блоков). */
	private static final int SPAWN_DEPTH_OFFSET = 13;
	/** Максимальный запас воздуха в тиках. */
	private static final int MAX_AIR = 300;
	/** Урон от утопления за тик вне воды. */
	private static final float DROWN_DAMAGE = 2.0F;

	protected WaterAnimalEntity(EntityType<? extends WaterAnimalEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.WATER, 0.0F);
	}

	@Override
	public boolean canSpawn(WorldView world) {
		return world.doesNotIntersectEntities(this);
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 120;
	}

	@Override
	public int getExperienceToDrop(ServerWorld world) {
		return 1 + random.nextInt(3);
	}

	/**
	 * Обновляет запас воздуха: уменьшает вне воды (с уроном при исчерпании),
	 * восстанавливает до максимума в воде.
	 *
	 * @param airBefore запас воздуха до вызова {@code baseTick()}
	 */
	protected void tickBreathing(int airBefore) {
		if (isAlive() && !isTouchingWater()) {
			setAir(airBefore - 1);
			if (shouldDrown()) {
				setAir(0);
				serverDamage(getDamageSources().drown(), DROWN_DAMAGE);
			}
		} else {
			setAir(MAX_AIR);
		}
	}

	@Override
	public void baseTick() {
		int airBefore = getAir();
		super.baseTick();
		tickBreathing(airBefore);
	}

	@Override
	public boolean isPushedByFluids() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	/**
	 * Проверяет допустимость спавна водного существа по позиции.
	 * Существо должно находиться в диапазоне от (уровень моря - 13) до уровня моря,
	 * снизу должна быть вода, сверху — блок воды.
	 */
	public static boolean canSpawn(
		EntityType<? extends WaterAnimalEntity> type,
		WorldAccess world,
		SpawnReason reason,
		BlockPos pos,
		Random random
	) {
		int seaLevel = world.getSeaLevel();
		int minY = seaLevel - SPAWN_DEPTH_OFFSET;
		return pos.getY() >= minY
			&& pos.getY() <= seaLevel
			&& world.getFluidState(pos.down()).isIn(FluidTags.WATER)
			&& world.getBlockState(pos.up()).isOf(Blocks.WATER);
	}
}
