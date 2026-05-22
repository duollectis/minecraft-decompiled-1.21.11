package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

/**
 * Блок яйца дракона. При взаимодействии или атаке телепортируется в случайную
 * позицию в радиусе 16 блоков, оставляя за собой частицы портала.
 */
public class DragonEggBlock extends FallingBlock {

	public static final MapCodec<DragonEggBlock> CODEC = createCodec(DragonEggBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(14.0, 0.0, 16.0);
	private static final int TELEPORT_ATTEMPTS = 1000;
	private static final int PARTICLE_COUNT = 128;
	private static final int FALL_DELAY = 5;
	private static final int PURE_BLACK_COLOR = -16777216;

	@Override
	public MapCodec<DragonEggBlock> getCodec() {
		return CODEC;
	}

	public DragonEggBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		teleport(state, world, pos);
		return ActionResult.SUCCESS;
	}

	@Override
	protected void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		teleport(state, world, pos);
	}

	/**
	 * Телепортирует яйцо в случайную свободную позицию в радиусе 16 блоков по X/Z и 8 по Y.
	 * На клиенте отображает частицы портала вдоль траектории перемещения.
	 */
	private void teleport(BlockState state, World world, BlockPos pos) {
		WorldBorder worldBorder = world.getWorldBorder();

		for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
			BlockPos target = pos.add(
					world.random.nextInt(16) - world.random.nextInt(16),
					world.random.nextInt(8) - world.random.nextInt(8),
					world.random.nextInt(16) - world.random.nextInt(16)
			);

			if (world.getBlockState(target).isAir() == false
					|| worldBorder.contains(target) == false
					|| world.isOutOfHeightLimit(target)
			) {
				continue;
			}

			if (world.isClient()) {
				for (int particle = 0; particle < PARTICLE_COUNT; particle++) {
					double progress = world.random.nextDouble();
					float velX = (world.random.nextFloat() - 0.5F) * 0.2F;
					float velY = (world.random.nextFloat() - 0.5F) * 0.2F;
					float velZ = (world.random.nextFloat() - 0.5F) * 0.2F;
					double x = MathHelper.lerp(progress, target.getX(), pos.getX()) + (world.random.nextDouble() - 0.5) + 0.5;
					double y = MathHelper.lerp(progress, target.getY(), pos.getY()) + world.random.nextDouble() - 0.5;
					double z = MathHelper.lerp(progress, target.getZ(), pos.getZ()) + (world.random.nextDouble() - 0.5) + 0.5;

					world.addParticleClient(ParticleTypes.PORTAL, x, y, z, velX, velY, velZ);
				}
			} else {
				world.setBlockState(target, state, Block.NOTIFY_LISTENERS);
				world.removeBlock(pos, false);
			}

			return;
		}
	}

	@Override
	protected int getFallDelay() {
		return FALL_DELAY;
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	@Override
	public int getColor(BlockState state, BlockView world, BlockPos pos) {
		return PURE_BLACK_COLOR;
	}
}
