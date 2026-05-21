package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.component.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.debug.DebugTrackable;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * {@code BlockEntity}.
 */
public abstract class BlockEntity implements DebugTrackable, RenderDataBlockEntity, AttachmentTarget {

	private static final Codec<BlockEntityType<?>> TYPE_CODEC = Registries.BLOCK_ENTITY_TYPE.getCodec();
	private static final Logger LOGGER = LogUtils.getLogger();
	private final BlockEntityType<?> type;
	protected @Nullable World world;
	protected final BlockPos pos;
	protected boolean removed;
	private BlockState cachedState;
	private ComponentMap components = ComponentMap.EMPTY;

	public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		this.type = type;
		this.pos = pos.toImmutable();
		this.validateSupports(state);
		this.cachedState = state;
	}

	private void validateSupports(BlockState state) {
		if (!this.supports(state)) {
			throw new IllegalStateException(
					"Invalid block entity " + this.getNameForReport() + " state at " + this.pos + ", got " + state);
		}
	}

	/**
	 * Supports.
	 *
	 * @param state state
	 *
	 * @return boolean — результат операции
	 */
	public boolean supports(BlockState state) {
		return this.type.supports(state);
	}

	/**
	 * Pos from nbt.
	 *
	 * @param chunkPos chunk pos
	 * @param nbt nbt
	 *
	 * @return BlockPos — результат операции
	 */
	public static BlockPos posFromNbt(ChunkPos chunkPos, NbtCompound nbt) {
		int i = nbt.getInt("x", 0);
		int j = nbt.getInt("y", 0);
		int k = nbt.getInt("z", 0);
		int l = ChunkSectionPos.getSectionCoord(i);
		int m = ChunkSectionPos.getSectionCoord(k);
		if (l != chunkPos.x || m != chunkPos.z) {
			LOGGER.warn("Block entity {} found in a wrong chunk, expected position from chunk {}", nbt, chunkPos);
			i = chunkPos.getOffsetX(ChunkSectionPos.getLocalCoord(i));
			k = chunkPos.getOffsetZ(ChunkSectionPos.getLocalCoord(k));
		}

		return new BlockPos(i, j, k);
	}

	public @Nullable World getWorld() {
		return this.world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public boolean hasWorld() {
		return this.world != null;
	}

	/**
	 * Читает data.
	 *
	 * @param view view
	 */
	protected void readData(ReadView view) {
	}

	/**
	 * Read.
	 *
	 * @param view view
	 */
	public final void read(ReadView view) {
		this.readData(view);
		this.components = view.<ComponentMap>read("components", ComponentMap.CODEC).orElse(ComponentMap.EMPTY);
	}

	/**
	 * Читает componentless data.
	 *
	 * @param view view
	 */
	public final void readComponentlessData(ReadView view) {
		this.readData(view);
	}

	/**
	 * Записывает data.
	 *
	 * @param view view
	 */
	protected void writeData(WriteView view) {
	}

	/**
	 * Создаёт nbt with identifying data.
	 *
	 * @param registries registries
	 *
	 * @return NbtCompound — результат операции
	 */
	public final NbtCompound createNbtWithIdentifyingData(RegistryWrapper.WrapperLookup registries) {
		NbtCompound var4;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this.getReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
			this.writeFullData(nbtWriteView);
			var4 = nbtWriteView.getNbt();
		}

		return var4;
	}

	/**
	 * Записывает full data.
	 *
	 * @param view view
	 */
	public void writeFullData(WriteView view) {
		this.writeDataWithoutId(view);
		this.writeIdentifyingData(view);
	}

	/**
	 * Записывает data with id.
	 *
	 * @param view view
	 */
	public void writeDataWithId(WriteView view) {
		this.writeDataWithoutId(view);
		this.writeId(view);
	}

	/**
	 * Создаёт nbt.
	 *
	 * @param registries registries
	 *
	 * @return NbtCompound — результат операции
	 */
	public final NbtCompound createNbt(RegistryWrapper.WrapperLookup registries) {
		NbtCompound var4;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this.getReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
			this.writeDataWithoutId(nbtWriteView);
			var4 = nbtWriteView.getNbt();
		}

		return var4;
	}

	/**
	 * Записывает data without id.
	 *
	 * @param data data
	 */
	public void writeDataWithoutId(WriteView data) {
		this.writeData(data);
		data.put("components", ComponentMap.CODEC, this.components);
	}

	/**
	 * Создаёт componentless nbt.
	 *
	 * @param registries registries
	 *
	 * @return NbtCompound — результат операции
	 */
	public final NbtCompound createComponentlessNbt(RegistryWrapper.WrapperLookup registries) {
		NbtCompound var4;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this.getReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
			this.writeComponentlessData(nbtWriteView);
			var4 = nbtWriteView.getNbt();
		}

		return var4;
	}

	/**
	 * Записывает componentless data.
	 *
	 * @param view view
	 */
	public void writeComponentlessData(WriteView view) {
		this.writeData(view);
	}

	private void writeId(WriteView view) {
		writeId(view, this.getType());
	}

	/**
	 * Записывает id.
	 *
	 * @param view view
	 * @param type type
	 */
	public static void writeId(WriteView view, BlockEntityType<?> type) {
		view.put("id", TYPE_CODEC, type);
	}

	private void writeIdentifyingData(WriteView view) {
		this.writeId(view);
		view.putInt("x", this.pos.getX());
		view.putInt("y", this.pos.getY());
		view.putInt("z", this.pos.getZ());
	}

	public static @Nullable BlockEntity createFromNbt(
			BlockPos pos,
			BlockState state,
			NbtCompound nbt,
			RegistryWrapper.WrapperLookup registries
	) {
		BlockEntityType<?> blockEntityType = nbt.<BlockEntityType<?>>get("id", TYPE_CODEC).orElse(null);
		if (blockEntityType == null) {
			LOGGER.error("Skipping block entity with invalid type: {}", nbt.get("id"));
			return null;
		}
		else {
			BlockEntity blockEntity;
			try {
				blockEntity = blockEntityType.instantiate(pos, state);
			}
			catch (Throwable var12) {
				LOGGER.error(
						"Failed to create block entity {} for block {} at position {} ",
						new Object[]{blockEntityType, pos, state, var12}
				);
				return null;
			}

			try {
				BlockEntity var7;
				try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
						blockEntity.getReporterContext(),
						LOGGER
				)
				) {
					blockEntity.read(NbtReadView.create(logging, registries, nbt));
					var7 = blockEntity;
				}

				return var7;
			}
			catch (Throwable var11) {
				LOGGER.error(
						"Failed to load data for block entity {} for block {} at position {}",
						new Object[]{blockEntityType, pos, state, var11}
				);
				return null;
			}
		}
	}

	/**
	 * Mark dirty.
	 */
	public void markDirty() {
		if (this.world != null) {
			markDirty(this.world, this.pos, this.cachedState);
		}
	}

	/**
	 * Mark dirty.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 */
	protected static void markDirty(World world, BlockPos pos, BlockState state) {
		world.markDirty(pos);
		if (!state.isAir()) {
			world.updateComparators(pos, state.getBlock());
		}
	}

	public BlockPos getPos() {
		return this.pos;
	}

	public BlockState getCachedState() {
		return this.cachedState;
	}

	/**
	 * To update packet.
	 *
	 * @return @Nullable Packet — результат операции
	 */
	public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
		return null;
	}

	/**
	 * To initial chunk data nbt.
	 *
	 * @param registries registries
	 *
	 * @return NbtCompound — результат операции
	 */
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return new NbtCompound();
	}

	public boolean isRemoved() {
		return this.removed;
	}

	/**
	 * Mark removed.
	 */
	public void markRemoved() {
		this.removed = true;
	}

	/**
	 * Проверяет возможность cel removal.
	 */
	public void cancelRemoval() {
		this.removed = false;
	}

	/**
	 * Обрабатывает событие block replaced.
	 *
	 * @param pos pos
	 * @param oldState old state
	 */
	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
		if (this instanceof Inventory inventory && this.world != null) {
			ItemScatterer.spawn(this.world, pos, inventory);
		}
	}

	/**
	 * Обрабатывает событие synced block event.
	 *
	 * @param type type
	 * @param data data
	 *
	 * @return boolean — результат операции
	 */
	public boolean onSyncedBlockEvent(int type, int data) {
		return false;
	}

	/**
	 * Populate crash report.
	 *
	 * @param crashReportSection crash report section
	 */
	public void populateCrashReport(CrashReportSection crashReportSection) {
		crashReportSection.add("Name", this::getNameForReport);
		crashReportSection.add("Cached block", this.getCachedState()::toString);
		if (this.world == null) {
			crashReportSection.add("Block location", () -> this.pos + " (world missing)");
		}
		else {
			crashReportSection.add("Actual block", this.world.getBlockState(this.pos)::toString);
			CrashReportSection.addBlockLocation(crashReportSection, this.world, this.pos);
		}
	}

	public String getNameForReport() {
		return Registries.BLOCK_ENTITY_TYPE.getId(this.getType()) + " // " + this.getClass().getCanonicalName();
	}

	public BlockEntityType<?> getType() {
		return this.type;
	}

	@Deprecated
	public void setCachedState(BlockState state) {
		this.validateSupports(state);
		this.cachedState = state;
	}

	/**
	 * Читает components.
	 *
	 * @param components components
	 */
	protected void readComponents(ComponentsAccess components) {
	}

	/**
	 * Читает components.
	 *
	 * @param stack stack
	 */
	public final void readComponents(ItemStack stack) {
		this.readComponents(stack.getDefaultComponents(), stack.getComponentChanges());
	}

	/**
	 * Читает components.
	 *
	 * @param defaultComponents default components
	 * @param components components
	 */
	public final void readComponents(ComponentMap defaultComponents, ComponentChanges components) {
		final Set<ComponentType<?>> set = new HashSet<>();
		set.add(DataComponentTypes.BLOCK_ENTITY_DATA);
		set.add(DataComponentTypes.BLOCK_STATE);
		final ComponentMap componentMap = MergedComponentMap.create(defaultComponents, components);
		this.readComponents(new ComponentsAccess() {
			@Override
			public <T> @Nullable T get(ComponentType<? extends T> type) {
				set.add(type);
				return componentMap.get(type);
			}

			@Override
			public <T> T getOrDefault(ComponentType<? extends T> type, T fallback) {
				set.add(type);
				return componentMap.getOrDefault(type, fallback);
			}
		});
		ComponentChanges componentChanges = components.withRemovedIf(set::contains);
		this.components = componentChanges.toAddedRemovedPair().added();
	}

	/**
	 * Добавляет components.
	 *
	 * @param builder builder
	 */
	protected void addComponents(ComponentMap.Builder builder) {
	}

	@Deprecated
	/**
	 * Удаляет from copied stack data.
	 *
	 * @param view view
	 */
	public void removeFromCopiedStackData(WriteView view) {
	}

	/**
	 * Создаёт component map.
	 *
	 * @return ComponentMap — результат операции
	 */
	public final ComponentMap createComponentMap() {
		ComponentMap.Builder builder = ComponentMap.builder();
		builder.addAll(this.components);
		this.addComponents(builder);
		return builder.build();
	}

	public ComponentMap getComponents() {
		return this.components;
	}

	public void setComponents(ComponentMap components) {
		this.components = components;
	}

	/**
	 * Try parse custom name.
	 *
	 * @param view view
	 * @param key key
	 *
	 * @return @Nullable Text — результат операции
	 */
	public static @Nullable Text tryParseCustomName(ReadView view, String key) {
		return view.<Text>read(key, TextCodecs.CODEC).orElse(null);
	}

	public ErrorReporter.Context getReporterContext() {
		return new BlockEntity.ReporterContext(this);
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
	}

	/**
	 * {@code ReporterContext}.
	 */
	record ReporterContext(BlockEntity blockEntity) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return this.blockEntity.getNameForReport() + "@" + this.blockEntity.getPos();
		}
	}
}
