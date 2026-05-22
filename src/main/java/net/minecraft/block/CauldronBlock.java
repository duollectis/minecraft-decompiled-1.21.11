package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;

/**
 * Пустой котёл. Может наполняться дождём (5% шанс), снегом (10% шанс),
 * а также водой или лавой через капельный камень.
 */
public class CauldronBlock extends AbstractCauldronBlock {

	public static final MapCodec<CauldronBlock> CODEC = createCodec(CauldronBlock::new);
	private static final float FILL_WITH_RAIN_CHANCE = 0.05F;
	private static final float FILL_WITH_SNOW_CHANCE = 0.1F;

	@Override
	public MapCodec<CauldronBlock> getCodec() {
		return CODEC;
	}

	public CauldronBlock(AbstractBlock.Settings settings) {
		super(settings, CauldronBehavior.EMPTY_CAULDRON_BEHAVIOR);
	}

	@Override
	public boolean isFull(BlockState state) {
		return false;
	}

	/**
	 * Проверяет, может ли котёл наполниться осадками в данный тик.
	 * Дождь даёт 5% шанс, снег — 10%. Другие типы осадков не заполняют котёл.
	 */
	protected static boolean canFillWithPrecipitation(World world, Biome.Precipitation precipitation) {
		if (precipitation == Biome.Precipitation.RAIN) {
			return world.getRandom().nextFloat() < FILL_WITH_RAIN_CHANCE;
		}

		return precipitation == Biome.Precipitation.SNOW
			? world.getRandom().nextFloat() < FILL_WITH_SNOW_CHANCE
			: false;
	}

	@Override
	public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
		if (canFillWithPrecipitation(world, precipitation)) {
			if (precipitation == Biome.Precipitation.RAIN) {
				world.setBlockState(pos, Blocks.WATER_CAULDRON.getDefaultState());
				world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
			}
			else if (precipitation == Biome.Precipitation.SNOW) {
				world.setBlockState(pos, Blocks.POWDER_SNOW_CAULDRON.getDefaultState());
				world.emitGameEvent(null, GameEvent.BLOCK_CHANGE, pos);
			}
		}
	}

	@Override
	protected boolean canBeFilledByDripstone(Fluid fluid) {
		return true;
	}

	@Override
	protected void fillFromDripstone(BlockState state, World world, BlockPos pos, Fluid fluid) {
		if (fluid == Fluids.WATER) {
			BlockState blockState = Blocks.WATER_CAULDRON.getDefaultState();
			world.setBlockState(pos, blockState);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
			world.syncWorldEvent(1047, pos, 0);
		}
		else if (fluid == Fluids.LAVA) {
			BlockState blockState = Blocks.LAVA_CAULDRON.getDefaultState();
			world.setBlockState(pos, blockState);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
			world.syncWorldEvent(1046, pos, 0);
		}
	}
}
