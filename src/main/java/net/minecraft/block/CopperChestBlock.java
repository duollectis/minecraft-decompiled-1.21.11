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
 * Медный сундук с поддержкой окисления. При объединении двух сундуков в двойной
 * выбирается менее окисленный вариант; вощёные сундуки автоматически разворощиваются перед сравнением.
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
		BlockState neighborState = world.getBlockState(pos.offset(getFacing(state)));
		if (state.get(ChestBlock.CHEST_TYPE).equals(ChestType.SINGLE)
				|| !(state.getBlock() instanceof CopperChestBlock self)
				|| !(neighborState.getBlock() instanceof CopperChestBlock neighbor)
		) {
			return state;
		}

		BlockState selfState = state;
		BlockState neighborResolved = neighborState;
		if (self.isWaxed() != neighbor.isWaxed()) {
			selfState = getUnwaxed(self, state).orElse(state);
			neighborResolved = getUnwaxed(neighbor, neighborState).orElse(neighborState);
		}

		Block lessOxidized = self.oxidationLevel.ordinal() <= neighbor.oxidationLevel.ordinal()
				? selfState.getBlock()
				: neighborResolved.getBlock();
		return lessOxidized.getStateWithProperties(selfState);
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
		return oxidationLevel;
	}

	/**
	 * Создаёт состояние медного сундука, соответствующее степени окисления переданного медного блока.
	 * Используется при превращении медного блока в сундук (например, через диспенсер).
	 *
	 * @param block  исходный медный блок (может быть вощёным или окисленным)
	 * @param facing направление, в которое смотрит сундук
	 * @param world  мир для определения типа сундука (одиночный/двойной)
	 * @param pos    позиция нового сундука
	 * @return состояние медного сундука с правильным типом и направлением
	 */
	public static BlockState fromCopperBlock(Block block, Direction facing, World world, BlockPos pos) {
		CopperChestBlock chest = (CopperChestBlock) FROM_COPPER_BLOCK.getOrDefault(block, Blocks.COPPER_CHEST::asBlock).get();
		ChestType chestType = chest.getChestType(world, pos, facing);
		BlockState initial = chest.getDefaultState().with(FACING, facing).with(CHEST_TYPE, chestType);
		return getNewState(initial, world, pos);
	}

	public boolean isWaxed() {
		return true;
	}

	@Override
	public boolean keepBlockEntityWhenReplacedWith(BlockState state) {
		return state.isIn(BlockTags.COPPER_CHESTS);
	}
}
