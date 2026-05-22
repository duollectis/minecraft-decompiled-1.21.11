package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Твёрдый коралловый блок (не растение). Без воды рядом запускает таймер смерти
 * и превращается в мёртвый коралловый блок через {@value #DEATH_DELAY_MIN}–{@value #DEATH_DELAY_MAX} тиков.
 */
public class CoralBlockBlock extends Block {

	/** Минимальная задержка (в тиках) до гибели кораллового блока без воды. */
	private static final int DEATH_DELAY_MIN = 60;
	/** Максимальная задержка (в тиках) до гибели кораллового блока без воды. */
	private static final int DEATH_DELAY_MAX = 40;

	public static final MapCodec<Block> DEAD_FIELD = Registries.BLOCK.getCodec().fieldOf("dead");
	public static final MapCodec<CoralBlockBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(DEAD_FIELD.forGetter(block -> block.deadCoralBlock), createSettingsCodec())
					.apply(instance, CoralBlockBlock::new)
	);
	private final Block deadCoralBlock;

	public CoralBlockBlock(Block deadCoralBlock, AbstractBlock.Settings settings) {
		super(settings);
		this.deadCoralBlock = deadCoralBlock;
	}

	@Override
	public MapCodec<CoralBlockBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!isInWater(world, pos)) {
			world.setBlockState(pos, deadCoralBlock.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		if (!isInWater(world, pos)) {
			tickView.scheduleBlockTick(pos, this, DEATH_DELAY_MIN + random.nextInt(DEATH_DELAY_MAX));
		}

		return super.getStateForNeighborUpdate(
				state,
				world,
				tickView,
				pos,
				direction,
				neighborPos,
				neighborState,
				random
		);
	}

	protected boolean isInWater(BlockView world, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			FluidState fluidState = world.getFluidState(pos.offset(direction));
			if (fluidState.isIn(FluidTags.WATER)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		if (!isInWater(ctx.getWorld(), ctx.getBlockPos())) {
			ctx.getWorld().scheduleBlockTick(
					ctx.getBlockPos(),
					this,
					DEATH_DELAY_MIN + ctx.getWorld().getRandom().nextInt(DEATH_DELAY_MAX)
			);
		}

		return getDefaultState();
	}
}
