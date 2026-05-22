package net.minecraft.entity.mob;

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
 * Базовый класс для водных мобов.
 */
public abstract class WaterCreatureEntity extends PathAwareEntity {

	public static final int MIN_AMBIENT_SOUND_DELAY = 120;
	private static final int MAX_AIR = 300;
	private static final float DROWN_DAMAGE = 2.0F;
	private static final int SPAWN_DEPTH_OFFSET = 13;

	protected WaterCreatureEntity(EntityType<? extends WaterCreatureEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.WATER, 0.0F);
	}

	@Override
	public boolean canSpawn(WorldView world) {
		return world.doesNotIntersectEntities(this);
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return MIN_AMBIENT_SOUND_DELAY;
	}

	@Override
	protected int getExperienceToDrop(ServerWorld world) {
		return 1 + random.nextInt(3);
	}

	/**
	 * Уменьшает запас воздуха вне воды и наносит урон утоплением при его исчерпании.
	 * Восстанавливает воздух до максимума при нахождении в воде.
	 */
	protected void tickWaterBreathingAir(ServerWorld world, int currentAir) {
		if (!isAlive() || isTouchingWater()) {
			setAir(MAX_AIR);
			return;
		}

		setAir(currentAir - 1);
		if (shouldDrown()) {
			setAir(0);
			damage(world, getDamageSources().drown(), DROWN_DAMAGE);
		}
	}

	@Override
	public void baseTick() {
		int airBeforeTick = getAir();
		super.baseTick();
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			tickWaterBreathingAir(serverWorld, airBeforeTick);
		}
	}

	@Override
	public boolean isPushedByFluids() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	public static boolean canSpawn(
			EntityType<? extends WaterCreatureEntity> type,
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
