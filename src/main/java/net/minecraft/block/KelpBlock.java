package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

/**
 * Стебель ламинарии (kelp), растущий вверх в воде.
 * <p>Является стеблевой частью растения: при росте порождает новый стебель выше,
 * а верхушка превращается в {@link Blocks#KELP_PLANT}. Не может расти на магме.</p>
 */
public class KelpBlock extends AbstractPlantStemBlock implements FluidFillable {

	public static final MapCodec<KelpBlock> CODEC = createCodec(KelpBlock::new);
	private static final double GROWTH_CHANCE = 0.14;
	private static final VoxelShape SHAPE = Block.createColumnShape(16.0, 0.0, 9.0);

	@Override
	public MapCodec<KelpBlock> getCodec() {
		return CODEC;
	}

	public KelpBlock(AbstractBlock.Settings settings) {
		super(settings, Direction.UP, SHAPE, true, GROWTH_CHANCE);
	}

	@Override
	protected boolean chooseStemState(BlockState state) {
		return state.isOf(Blocks.WATER);
	}

	@Override
	protected Block getPlant() {
		return Blocks.KELP_PLANT;
	}

	@Override
	protected boolean canAttachTo(BlockState state) {
		return !state.isOf(Blocks.MAGMA_BLOCK);
	}

	@Override
	public boolean canFillWithFluid(
			@Nullable LivingEntity filler,
			BlockView world,
			BlockPos pos,
			BlockState state,
			Fluid fluid
	) {
		return false;
	}

	@Override
	public boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
		return false;
	}

	@Override
	protected int getGrowthLength(Random random) {
		return 1;
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		return fluidState.isIn(FluidTags.WATER) && fluidState.getLevel() == 8 ? super.getPlacementState(ctx) : null;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return Fluids.WATER.getStill(false);
	}
}
