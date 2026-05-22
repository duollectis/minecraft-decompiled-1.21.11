package net.minecraft.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.predicate.component.ComponentsPredicate;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Предикат для проверки блока по набору условий: тип блока, состояние, NBT и компоненты.
 * Все поля опциональны — отсутствующее условие считается выполненным.
 */
public record BlockPredicate(
		Optional<RegistryEntryList<Block>> blocks,
		Optional<StatePredicate> state,
		Optional<NbtPredicate> nbt,
		ComponentsPredicate components
) {

	public static final Codec<BlockPredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("blocks").forGetter(BlockPredicate::blocks),
					StatePredicate.CODEC.optionalFieldOf("state").forGetter(BlockPredicate::state),
					NbtPredicate.CODEC.optionalFieldOf("nbt").forGetter(BlockPredicate::nbt),
					ComponentsPredicate.CODEC.forGetter(BlockPredicate::components)
			)
			.apply(instance, BlockPredicate::new)
	);
	public static final PacketCodec<RegistryByteBuf, BlockPredicate> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.optional(PacketCodecs.registryEntryList(RegistryKeys.BLOCK)), BlockPredicate::blocks,
			PacketCodecs.optional(StatePredicate.PACKET_CODEC), BlockPredicate::state,
			PacketCodecs.optional(NbtPredicate.PACKET_CODEC), BlockPredicate::nbt,
			ComponentsPredicate.PACKET_CODEC, BlockPredicate::components,
			BlockPredicate::new
	);

	/**
	 * Проверяет блок в мире по всем условиям предиката.
	 * Возвращает {@code false}, если позиция не загружена.
	 */
	public boolean test(ServerWorld world, BlockPos pos) {
		if (!world.isPosLoaded(pos)) {
			return false;
		}

		if (!testState(world.getBlockState(pos))) {
			return false;
		}

		if (nbt.isPresent() || !components.isEmpty()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);

			if (nbt.isPresent() && !testNbt(world, blockEntity, nbt.get())) {
				return false;
			}

			if (!components.isEmpty() && !testComponents(blockEntity, components)) {
				return false;
			}
		}

		return true;
	}

	public boolean test(CachedBlockPosition pos) {
		if (!testState(pos.getBlockState())) {
			return false;
		}

		return nbt.isEmpty() || testNbt(pos.getWorld(), pos.getBlockEntity(), nbt.get());
	}

	private boolean testState(BlockState state) {
		if (blocks.isPresent() && !state.isIn(blocks.get())) {
			return false;
		}

		return this.state.isEmpty() || this.state.get().test(state);
	}

	private static boolean testNbt(WorldView world, @Nullable BlockEntity blockEntity, NbtPredicate nbtPredicate) {
		return blockEntity != null
				&& nbtPredicate.test(blockEntity.createNbtWithIdentifyingData(world.getRegistryManager()));
	}

	private static boolean testComponents(@Nullable BlockEntity blockEntity, ComponentsPredicate components) {
		return blockEntity != null && components.test((ComponentsAccess) blockEntity.createComponentMap());
	}

	public boolean hasNbt() {
		return nbt.isPresent();
	}

	public static class Builder {

		private Optional<RegistryEntryList<Block>> blocks = Optional.empty();
		private Optional<StatePredicate> state = Optional.empty();
		private Optional<NbtPredicate> nbt = Optional.empty();
		private ComponentsPredicate components = ComponentsPredicate.EMPTY;

		private Builder() {
		}

		public static BlockPredicate.Builder create() {
			return new BlockPredicate.Builder();
		}

		public BlockPredicate.Builder blocks(RegistryEntryLookup<Block> blockRegistry, Block... blocks) {
			return this.blocks(blockRegistry, Arrays.asList(blocks));
		}

		public BlockPredicate.Builder blocks(RegistryEntryLookup<Block> blockRegistry, Collection<Block> blocks) {
			this.blocks = Optional.of(RegistryEntryList.of(Block::getRegistryEntry, blocks));
			return this;
		}

		public BlockPredicate.Builder tag(RegistryEntryLookup<Block> blockRegistry, TagKey<Block> tag) {
			this.blocks = Optional.of(blockRegistry.getOrThrow(tag));
			return this;
		}

		public BlockPredicate.Builder nbt(NbtCompound nbt) {
			this.nbt = Optional.of(new NbtPredicate(nbt));
			return this;
		}

		public BlockPredicate.Builder state(StatePredicate.Builder state) {
			this.state = state.build();
			return this;
		}

		public BlockPredicate.Builder components(ComponentsPredicate components) {
			this.components = components;
			return this;
		}

		public BlockPredicate build() {
			return new BlockPredicate(blocks, state, nbt, components);
		}
	}
}
