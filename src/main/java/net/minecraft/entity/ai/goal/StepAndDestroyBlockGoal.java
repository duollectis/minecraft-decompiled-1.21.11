package net.minecraft.entity.ai.goal;

import net.minecraft.block.Block;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

/**
 * Цель уничтожения блока путём топтания: моб подходит к целевому блоку и
 * прыгает на нём, пока не разрушит его (при включённом {@code DO_MOB_GRIEFING}).
 */
public class StepAndDestroyBlockGoal extends MoveToTargetPosGoal {

	private static final int MAX_COOLDOWN = 20;
	private static final int DESTROY_TICKS = 60;
	private static final int STEP_INTERVAL = 2;
	private static final int TICK_INTERVAL = 6;
	private static final int POOF_COUNT = 20;
	private static final double PARTICLE_SPREAD = 0.02;
	private static final double PARTICLE_SPEED = 0.15;
	private static final double JUMP_UP_VELOCITY = 0.3;
	private static final double JUMP_DOWN_VELOCITY = -0.3;
	private static final double EGG_PARTICLE_SPREAD = 0.08;

	private final Block targetBlock;
	private final MobEntity stepAndDestroyMob;
	private int counter;

	public StepAndDestroyBlockGoal(Block targetBlock, PathAwareEntity mob, double speed, int maxYDifference) {
		super(mob, speed, 24, maxYDifference);
		this.targetBlock = targetBlock;
		this.stepAndDestroyMob = mob;
	}

	@Override
	public boolean canStart() {
		if (!getServerWorld(stepAndDestroyMob).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
			return false;
		}

		if (cooldown > 0) {
			cooldown--;
			return false;
		}

		if (findTargetPos()) {
			cooldown = toGoalTicks(MAX_COOLDOWN);
			return true;
		}

		cooldown = getInterval(mob);
		return false;
	}

	@Override
	public void stop() {
		super.stop();
		stepAndDestroyMob.fallDistance = 1.0;
	}

	@Override
	public void start() {
		super.start();
		counter = 0;
	}

	public void tickStepping(WorldAccess world, BlockPos pos) {
	}

	public void onDestroyBlock(World world, BlockPos pos) {
	}

	@Override
	public void tick() {
		super.tick();
		World world = stepAndDestroyMob.getEntityWorld();
		BlockPos blockPos = stepAndDestroyMob.getBlockPos();
		BlockPos targetPos = tweakToProperPos(blockPos, world);
		Random random = stepAndDestroyMob.getRandom();
		if (!hasReached() || targetPos == null) {
			return;
		}

		if (counter > 0) {
			Vec3d velocity = stepAndDestroyMob.getVelocity();
			stepAndDestroyMob.setVelocity(velocity.x, JUMP_UP_VELOCITY, velocity.z);
			if (!world.isClient()) {
				((ServerWorld) world).spawnParticles(
						new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.EGG)),
						targetPos.getX() + 0.5,
						targetPos.getY() + 0.7,
						targetPos.getZ() + 0.5,
						3,
						(random.nextFloat() - 0.5) * EGG_PARTICLE_SPREAD,
						(random.nextFloat() - 0.5) * EGG_PARTICLE_SPREAD,
						(random.nextFloat() - 0.5) * EGG_PARTICLE_SPREAD,
						(float) PARTICLE_SPEED
				);
			}
		}

		if (counter % STEP_INTERVAL == 0) {
			Vec3d velocity = stepAndDestroyMob.getVelocity();
			stepAndDestroyMob.setVelocity(velocity.x, JUMP_DOWN_VELOCITY, velocity.z);
			if (counter % TICK_INTERVAL == 0) {
				tickStepping(world, this.targetPos);
			}
		}

		if (counter > DESTROY_TICKS) {
			world.removeBlock(targetPos, false);
			if (!world.isClient()) {
				for (int i = 0; i < POOF_COUNT; i++) {
					double dx = random.nextGaussian() * PARTICLE_SPREAD;
					double dy = random.nextGaussian() * PARTICLE_SPREAD;
					double dz = random.nextGaussian() * PARTICLE_SPREAD;
					((ServerWorld) world).spawnParticles(
							ParticleTypes.POOF,
							targetPos.getX() + 0.5,
							targetPos.getY(),
							targetPos.getZ() + 0.5,
							1,
							dx,
							dy,
							dz,
							(float) PARTICLE_SPEED
					);
				}

				onDestroyBlock(world, targetPos);
			}
		}

		counter++;
	}

	private @Nullable BlockPos tweakToProperPos(BlockPos pos, BlockView world) {
		if (world.getBlockState(pos).isOf(targetBlock)) {
			return pos;
		}

		BlockPos[] neighbors = {pos.down(), pos.west(), pos.east(), pos.north(), pos.south(), pos.down().down()};
		for (BlockPos neighbor : neighbors) {
			if (world.getBlockState(neighbor).isOf(targetBlock)) {
				return neighbor;
			}
		}

		return null;
	}

	@Override
	protected boolean isTargetPos(WorldView world, BlockPos pos) {
		Chunk chunk = world.getChunk(
				ChunkSectionPos.getSectionCoord(pos.getX()),
				ChunkSectionPos.getSectionCoord(pos.getZ()),
				ChunkStatus.FULL,
				false
		);
		return chunk != null
				&& chunk.getBlockState(pos).isOf(targetBlock)
				&& chunk.getBlockState(pos.up()).isAir()
				&& chunk.getBlockState(pos.up(2)).isAir();
	}
}
