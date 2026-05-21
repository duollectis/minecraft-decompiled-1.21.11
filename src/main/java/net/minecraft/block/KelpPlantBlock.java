package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

/**
 * {@code KelpPlantBlock}.
 */
public class KelpPlantBlock extends AbstractPlantBlock implements FluidFillable {

	public static final MapCodec<KelpPlantBlock> CODEC = createCodec(KelpPlantBlock::new);

	@Override
	public MapCodec<KelpPlantBlock> getCodec() {
		return CODEC;
	}

	public KelpPlantBlock(AbstractBlock.Settings settings) {
		super(settings, Direction.UP, VoxelShapes.fullCube(), true);
	}

	@Override
	protected AbstractPlantStemBlock getStem() {
		return (AbstractPlantStemBlock) Blocks.KELP;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return Fluids.WATER.getStill(false);
	}

	@Override
	protected boolean canAttachTo(BlockState state) {
		return this.getStem().canAttachTo(state);
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
}
