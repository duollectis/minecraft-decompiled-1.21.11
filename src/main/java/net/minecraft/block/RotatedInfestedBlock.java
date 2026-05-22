package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;

/**
 * Заражённый блок с поддержкой вращения по оси (например, заражённый булыжник-столб).
 * Ось размещения определяется стороной, с которой игрок устанавливает блок.
 */
public class RotatedInfestedBlock extends InfestedBlock {

	public static final MapCodec<RotatedInfestedBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							Registries.BLOCK.getCodec().fieldOf("host").forGetter(InfestedBlock::getRegularBlock),
							createSettingsCodec()
					)
					.apply(instance, RotatedInfestedBlock::new)
	);

	@Override
	public MapCodec<RotatedInfestedBlock> getCodec() {
		return CODEC;
	}

	public RotatedInfestedBlock(Block block, AbstractBlock.Settings settings) {
		super(block, settings);
		setDefaultState(getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return PillarBlock.changeRotation(state, rotation);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(PillarBlock.AXIS);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(PillarBlock.AXIS, ctx.getSide().getAxis());
	}
}
