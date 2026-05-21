package net.minecraft.block;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;

import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * {@code Oxidizable}.
 */
public interface Oxidizable extends Degradable<Oxidizable.OxidationLevel> {

	com.google.common.base.Supplier<BiMap<Block, Block>> OXIDATION_LEVEL_INCREASES_GUAVA = Suppliers.memoize(
			() -> ImmutableBiMap.<Block, Block>builder()
			                    .put(Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER)
			                    .put(Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER)
			                    .put(Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER)
			                    .put(Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER)
			                    .put(Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER)
			                    .put(Blocks.WEATHERED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER)
			                    .put(Blocks.CHISELED_COPPER, Blocks.EXPOSED_CHISELED_COPPER)
			                    .put(Blocks.EXPOSED_CHISELED_COPPER, Blocks.WEATHERED_CHISELED_COPPER)
			                    .put(Blocks.WEATHERED_CHISELED_COPPER, Blocks.OXIDIZED_CHISELED_COPPER)
			                    .put(Blocks.CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_SLAB)
			                    .put(Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_SLAB)
			                    .put(Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_SLAB)
			                    .put(Blocks.CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_STAIRS)
			                    .put(Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_STAIRS)
			                    .put(Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER_STAIRS)
			                    .put(Blocks.COPPER_DOOR, Blocks.EXPOSED_COPPER_DOOR)
			                    .put(Blocks.EXPOSED_COPPER_DOOR, Blocks.WEATHERED_COPPER_DOOR)
			                    .put(Blocks.WEATHERED_COPPER_DOOR, Blocks.OXIDIZED_COPPER_DOOR)
			                    .put(Blocks.COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR)
			                    .put(Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR)
			                    .put(Blocks.WEATHERED_COPPER_TRAPDOOR, Blocks.OXIDIZED_COPPER_TRAPDOOR)
			                    .putAll(Blocks.COPPER_BARS.getOxidizingMap())
			                    .put(Blocks.COPPER_GRATE, Blocks.EXPOSED_COPPER_GRATE)
			                    .put(Blocks.EXPOSED_COPPER_GRATE, Blocks.WEATHERED_COPPER_GRATE)
			                    .put(Blocks.WEATHERED_COPPER_GRATE, Blocks.OXIDIZED_COPPER_GRATE)
			                    .put(Blocks.COPPER_BULB, Blocks.EXPOSED_COPPER_BULB)
			                    .put(Blocks.EXPOSED_COPPER_BULB, Blocks.WEATHERED_COPPER_BULB)
			                    .put(Blocks.WEATHERED_COPPER_BULB, Blocks.OXIDIZED_COPPER_BULB)
			                    .putAll(Blocks.COPPER_LANTERNS.getOxidizingMap())
			                    .put(Blocks.COPPER_CHEST, Blocks.EXPOSED_COPPER_CHEST)
			                    .put(Blocks.EXPOSED_COPPER_CHEST, Blocks.WEATHERED_COPPER_CHEST)
			                    .put(Blocks.WEATHERED_COPPER_CHEST, Blocks.OXIDIZED_COPPER_CHEST)
			                    .put(Blocks.COPPER_GOLEM_STATUE, Blocks.EXPOSED_COPPER_GOLEM_STATUE)
			                    .put(Blocks.EXPOSED_COPPER_GOLEM_STATUE, Blocks.WEATHERED_COPPER_GOLEM_STATUE)
			                    .put(Blocks.WEATHERED_COPPER_GOLEM_STATUE, Blocks.OXIDIZED_COPPER_GOLEM_STATUE)
			                    .put(Blocks.LIGHTNING_ROD, Blocks.EXPOSED_LIGHTNING_ROD)
			                    .put(Blocks.EXPOSED_LIGHTNING_ROD, Blocks.WEATHERED_LIGHTNING_ROD)
			                    .put(Blocks.WEATHERED_LIGHTNING_ROD, Blocks.OXIDIZED_LIGHTNING_ROD)
			                    .putAll(Blocks.COPPER_CHAINS.getOxidizingMap())
			                    .build()
	);

	Supplier<BiMap<Block, Block>> OXIDATION_LEVEL_INCREASES = OXIDATION_LEVEL_INCREASES_GUAVA::get;

	com.google.common.base.Supplier<BiMap<Block, Block>> OXIDATION_LEVEL_DECREASES_GUAVA = Suppliers.memoize(
			() -> OXIDATION_LEVEL_INCREASES.get().inverse()
	);

	Supplier<BiMap<Block, Block>> OXIDATION_LEVEL_DECREASES = OXIDATION_LEVEL_DECREASES_GUAVA::get;

	static Optional<Block> getDecreasedOxidationBlock(Block block) {
		return Optional.ofNullable((Block) OXIDATION_LEVEL_DECREASES.get().get(block));
	}

	static Block getUnaffectedOxidationBlock(Block block) {
		Block block2 = block;

		for (Block block3 = (Block) OXIDATION_LEVEL_DECREASES.get().get(block);
		     block3 != null;
		     block3 = (Block) OXIDATION_LEVEL_DECREASES.get().get(block3)) {
			block2 = block3;
		}

		return block2;
	}

	static Optional<BlockState> getDecreasedOxidationState(BlockState state) {
		return getDecreasedOxidationBlock(state.getBlock()).map(block -> block.getStateWithProperties(state));
	}

	static Optional<Block> getIncreasedOxidationBlock(Block block) {
		return Optional.ofNullable((Block) OXIDATION_LEVEL_INCREASES.get().get(block));
	}

	static BlockState getUnaffectedOxidationState(BlockState state) {
		return getUnaffectedOxidationBlock(state.getBlock()).getStateWithProperties(state);
	}

	@Override
	default Optional<BlockState> getDegradationResult(BlockState state) {
		return getIncreasedOxidationBlock(state.getBlock()).map(block -> block.getStateWithProperties(state));
	}

	@Override
	default float getDegradationChanceMultiplier() {
		return this.getDegradationLevel() == Oxidizable.OxidationLevel.UNAFFECTED ? 0.75F : 1.0F;
	}

	/**
	 * {@code OxidationLevel}.
	 */
	public static enum OxidationLevel implements StringIdentifiable {
		UNAFFECTED("unaffected"),
		EXPOSED("exposed"),
		WEATHERED("weathered"),
		OXIDIZED("oxidized");

		public static final IntFunction<Oxidizable.OxidationLevel> indexMapper = ValueLists.createIndexToValueFunction(
				(Oxidizable.OxidationLevel level) -> level.ordinal(), values(), ValueLists.OutOfBoundsHandling.CLAMP
		);
		public static final Codec<Oxidizable.OxidationLevel>
				CODEC =
				StringIdentifiable.createCodec(Oxidizable.OxidationLevel::values);
		public static final PacketCodec<ByteBuf, Oxidizable.OxidationLevel>
				PACKET_CODEC =
				PacketCodecs.indexed(indexMapper, Enum::ordinal);
		private final String id;

		private OxidationLevel(final String id) {
			this.id = id;
		}

		@Override
		public String asString() {
			return this.id;
		}

		public Oxidizable.OxidationLevel getIncreased() {
			return indexMapper.apply(this.ordinal() + 1);
		}

		public Oxidizable.OxidationLevel getDecreased() {
			return indexMapper.apply(this.ordinal() - 1);
		}
	}
}
