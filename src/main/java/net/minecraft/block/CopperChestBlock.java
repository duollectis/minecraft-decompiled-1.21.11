package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@code CopperChestBlock}.
 */
public class CopperChestBlock extends ChestBlock {

	public static final MapCodec<CopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Oxidizable.OxidationLevel.CODEC
							                    .fieldOf("weathering_state")
							                    .forGetter(CopperChestBlock::getOxidationLevel),
					                    Registries.SOUND_EVENT.getCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenSound),
					                    Registries.SOUND_EVENT.getCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseSound),
					                    createSettingsCodec()
			                    )
			                    .apply(instance, CopperChestBlock::new)
	);
	private static final Map<Block, Supplier<Block>> FROM_COPPER_BLOCK = Map.of(
			Blocks.COPPER_BLOCK,
			() -> Blocks.COPPER_CHEST,
			Blocks.EXPOSED_COPPER,
			() -> Blocks.EXPOSED_COPPER_CHEST,
			Blocks.WEATHERED_COPPER,
			() -> Blocks.WEATHERED_COPPER_CHEST,
			Blocks.OXIDIZED_COPPER,
			() -> Blocks.OXIDIZED_COPPER_CHEST,
			Blocks.WAXED_COPPER_BLOCK,
			() -> Blocks.COPPER_CHEST,
			Blocks.WAXED_EXPOSED_COPPER,
			() -> Blocks.EXPOSED_COPPER_CHEST,
			Blocks.WAXED_WEATHERED_COPPER,
			() -> Blocks.WEATHERED_COPPER_CHEST,
			Blocks.WAXED_OXIDIZED_COPPER,
			() -> Blocks.OXIDIZED_COPPER_CHEST
	);
	private final Oxidizable.OxidationLevel oxidationLevel;

	@Override
	public MapCodec<? extends CopperChestBlock> getCodec() {
		return CODEC;
	}

	public CopperChestBlock(
			Oxidizable.OxidationLevel oxidationLevel,
			SoundEvent openSound,
			SoundEvent closeSound,
			AbstractBlock.Settings settings
	) {
		super(() -> BlockEntityType.CHEST, openSound, closeSound, settings);
		this.oxidationLevel = oxidationLevel;
	}

	@Override
	public boolean canMergeWith(BlockState state) {
		return state.isIn(BlockTags.COPPER_CHESTS) && state.contains(ChestBlock.CHEST_TYPE);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState blockState = super.getPlacementState(ctx);
		return getNewState(blockState, ctx.getWorld(), ctx.getBlockPos());
	}

	private static BlockState getNewState(BlockState state, World world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos.offset(getFacing(state)));
		if (!state.get(ChestBlock.CHEST_TYPE).equals(ChestType.SINGLE)
				&& state.getBlock() instanceof CopperChestBlock copperChestBlock
				&& blockState.getBlock() instanceof CopperChestBlock copperChestBlock2) {
			BlockState blockState2 = state;
			BlockState blockState3 = blockState;
			if (copperChestBlock.isWaxed() != copperChestBlock2.isWaxed()) {
				blockState2 = getUnwaxed(copperChestBlock, state).orElse(state);
				blockState3 = getUnwaxed(copperChestBlock2, blockState).orElse(blockState);
			}

			Block block = copperChestBlock.oxidationLevel.ordinal() <= copperChestBlock2.oxidationLevel.ordinal()
			              ? blockState2.getBlock()
			              : blockState3.getBlock();
			return block.getStateWithProperties(blockState2);
		}
		else {
			return state;
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
		BlockState
				blockState =
				super.getStateForNeighborUpdate(
						state,
						world,
						tickView,
						pos,
						direction,
						neighborPos,
						neighborState,
						random
				);
		if (this.canMergeWith(neighborState)) {
			ChestType chestType = blockState.get(ChestBlock.CHEST_TYPE);
			if (!chestType.equals(ChestType.SINGLE) && getFacing(blockState) == direction) {
				return neighborState.getBlock().getStateWithProperties(blockState);
			}
		}

		return blockState;
	}

	private static Optional<BlockState> getUnwaxed(CopperChestBlock block, BlockState state) {
		return !block.isWaxed()
		       ? Optional.of(state)
		       : Optional.ofNullable((Block) HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().get(state.getBlock()))
		                 .map(waxedState -> ((Block) waxedState).getStateWithProperties(state));
	}

	public Oxidizable.OxidationLevel getOxidationLevel() {
		return this.oxidationLevel;
	}

	public static BlockState fromCopperBlock(Block block, Direction facing, World world, BlockPos pos) {
		CopperChestBlock
				copperChestBlock =
				(CopperChestBlock) FROM_COPPER_BLOCK.getOrDefault(block, Blocks.COPPER_CHEST::asBlock).get();
		ChestType chestType = copperChestBlock.getChestType(world, pos, facing);
		BlockState blockState = copperChestBlock.getDefaultState().with(FACING, facing).with(CHEST_TYPE, chestType);
		return getNewState(blockState, world, pos);
	}

	public boolean isWaxed() {
		return true;
	}

	@Override
	public boolean keepBlockEntityWhenReplacedWith(BlockState state) {
		return state.isIn(BlockTags.COPPER_CHESTS);
	}
}
