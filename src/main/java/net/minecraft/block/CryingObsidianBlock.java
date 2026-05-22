package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Плачущий обсидиан — декоративный блок, испускающий частицы слёз по случайным граням.
 * Используется как компонент возрождающего якоря.
 */
public class CryingObsidianBlock extends Block {

	public static final MapCodec<CryingObsidianBlock> CODEC = createCodec(CryingObsidianBlock::new);

	@Override
	public MapCodec<CryingObsidianBlock> getCodec() {
		return CODEC;
	}

	public CryingObsidianBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(5) != 0) {
			return;
		}

		Direction direction = Direction.random(random);

		if (direction == Direction.UP) {
			return;
		}

		BlockPos neighborPos = pos.offset(direction);
		BlockState neighborState = world.getBlockState(neighborPos);

		if (state.isOpaque() && neighborState.isSideSolidFullSquare(world, neighborPos, direction.getOpposite())) {
			return;
		}

		double x = direction.getOffsetX() == 0 ? random.nextDouble() : 0.5 + direction.getOffsetX() * 0.6;
		double y = direction.getOffsetY() == 0 ? random.nextDouble() : 0.5 + direction.getOffsetY() * 0.6;
		double z = direction.getOffsetZ() == 0 ? random.nextDouble() : 0.5 + direction.getOffsetZ() * 0.6;

		world.addParticleClient(
			ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
			pos.getX() + x,
			pos.getY() + y,
			pos.getZ() + z,
			0.0,
			0.0,
			0.0
		);
	}
}
