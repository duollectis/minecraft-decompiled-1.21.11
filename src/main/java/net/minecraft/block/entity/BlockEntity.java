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
 * Базовый класс для всех блок-сущностей (Block Entities) в Minecraft.
 * <p>
 * Блок-сущность — это объект, прикреплённый к конкретному блоку в мире и хранящий
 * дополнительные данные (инвентарь, состояние, прогресс крафта и т.д.).
 * Жизненный цикл управляется через {@link BlockEntityType}, сериализацию NBT
 * и систему компонентов {@link ComponentMap}.
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

	public boolean supports(BlockState state) {
		return type.supports(state);
	}

	/**
	 * Восстанавливает позицию блок-сущности из NBT с коррекцией при несовпадении чанка.
	 * <p>
	 * Если координаты X/Z из NBT указывают на другой чанк (артефакт повреждённых данных),
	 * позиция принудительно корректируется до ближайшей валидной точки внутри {@code chunkPos}.
	 */
	public static BlockPos posFromNbt(ChunkPos chunkPos, NbtCompound nbt) {
		int x = nbt.getInt("x", 0);
		int y = nbt.getInt("y", 0);
		int z = nbt.getInt("z", 0);
		int chunkX = ChunkSectionPos.getSectionCoord(x);
		int chunkZ = ChunkSectionPos.getSectionCoord(z);

		if (chunkX != chunkPos.x || chunkZ != chunkPos.z) {
			LOGGER.warn("Block entity {} found in a wrong chunk, expected position from chunk {}", nbt, chunkPos);
			x = chunkPos.getOffsetX(ChunkSectionPos.getLocalCoord(x));
			z = chunkPos.getOffsetZ(ChunkSectionPos.getLocalCoord(z));
		}

		return new BlockPos(x, y, z);
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

	protected void readData(ReadView view) {
	}

	public final void read(ReadView view) {
		readData(view);
		components = view.<ComponentMap>read("components", ComponentMap.CODEC).orElse(ComponentMap.EMPTY);
	}

	public final void readComponentlessData(ReadView view) {
		readData(view);
	}

	protected void writeData(WriteView view) {
	}

	public final NbtCompound createNbtWithIdentifyingData(RegistryWrapper.WrapperLookup registries) {
		NbtCompound result;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(getReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
			writeFullData(nbtWriteView);
			result = nbtWriteView.getNbt();
		}

		return result;
	}

	public void writeFullData(WriteView view) {
		writeDataWithoutId(view);
		writeIdentifyingData(view);
	}

	public void writeDataWithId(WriteView view) {
		writeDataWithoutId(view);
		writeId(view);
	}

	public final NbtCompound createNbt(RegistryWrapper.WrapperLookup registries) {
		NbtCompound result;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(getReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
			writeDataWithoutId(nbtWriteView);
			result = nbtWriteView.getNbt();
		}

		return result;
	}

	public void writeDataWithoutId(WriteView data) {
		writeData(data);
		data.put("components", ComponentMap.CODEC, components);
	}

	public final NbtCompound createComponentlessNbt(RegistryWrapper.WrapperLookup registries) {
		NbtCompound result;
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(getReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
			writeComponentlessData(nbtWriteView);
			result = nbtWriteView.getNbt();
		}

		return result;
	}

	public void writeComponentlessData(WriteView view) {
		writeData(view);
	}

	private void writeId(WriteView view) {
		writeId(view, getType());
	}

	public static void writeId(WriteView view, BlockEntityType<?> type) {
		view.put("id", TYPE_CODEC, type);
	}

	private void writeIdentifyingData(WriteView view) {
		writeId(view);
		view.putInt("x", pos.getX());
		view.putInt("y", pos.getY());
		view.putInt("z", pos.getZ());
	}

	/**
	 * Создаёт блок-сущность из NBT-данных с полной обработкой ошибок.
	 * <p>
	 * Сначала определяет тип по ключу {@code "id"}, затем инстанцирует объект
	 * и десериализует его данные. При любой ошибке возвращает {@code null} и логирует причину,
	 * чтобы не ронять загрузку чанка из-за одного повреждённого блока.
	 */
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

		BlockEntity blockEntity;
		try {
			blockEntity = blockEntityType.instantiate(pos, state);
		} catch (Throwable instantiationError) {
			LOGGER.error(
					"Failed to create block entity {} for block {} at position {} ",
					blockEntityType, pos, state, instantiationError
			);
			return null;
		}

		try {
			try (ErrorReporter.Logging logging = new ErrorReporter.Logging(blockEntity.getReporterContext(), LOGGER)) {
				blockEntity.read(NbtReadView.create(logging, registries, nbt));
			}

			return blockEntity;
		} catch (Throwable readError) {
			LOGGER.error(
					"Failed to load data for block entity {} for block {} at position {}",
					blockEntityType, pos, state, readError
			);
			return null;
		}
	}

	public void markDirty() {
		if (world != null) {
			markDirty(world, pos, cachedState);
		}
	}

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

	public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
		return null;
	}

	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return new NbtCompound();
	}

	public boolean isRemoved() {
		return this.removed;
	}

	public void markRemoved() {
		removed = true;
	}

	public void cancelRemoval() {
		removed = false;
	}

	public void onBlockReplaced(BlockPos pos, BlockState oldState) {
		if (this instanceof Inventory inventory && world != null) {
			ItemScatterer.spawn(world, pos, inventory);
		}
	}

	public boolean onSyncedBlockEvent(int type, int data) {
		return false;
	}

	public void populateCrashReport(CrashReportSection crashReportSection) {
		crashReportSection.add("Name", this::getNameForReport);
		crashReportSection.add("Cached block", this.getCachedState()::toString);

		if (world == null) {
			crashReportSection.add("Block location", () -> pos + " (world missing)");
		} else {
			crashReportSection.add("Actual block", world.getBlockState(pos)::toString);
			CrashReportSection.addBlockLocation(crashReportSection, world, pos);
		}
	}

	public String getNameForReport() {
		return Registries.BLOCK_ENTITY_TYPE.getId(this.getType()) + " // " + this.getClass().getCanonicalName();
	}

	public BlockEntityType<?> getType() {
		return type;
	}

	/**
	 * @deprecated Используй только при необходимости принудительной смены типа блока без пересоздания сущности.
	 */
	@Deprecated
	public void setCachedState(BlockState state) {
		validateSupports(state);
		cachedState = state;
	}

	protected void readComponents(ComponentsAccess components) {
	}

	public final void readComponents(ItemStack stack) {
		readComponents(stack.getDefaultComponents(), stack.getComponentChanges());
	}

	/**
	 * Применяет компоненты из ItemStack к блок-сущности, отслеживая какие типы были прочитаны.
	 * <p>
	 * Компоненты {@code BLOCK_ENTITY_DATA} и {@code BLOCK_STATE} исключаются из финального
	 * {@link ComponentMap}, чтобы не дублировать данные, уже хранящиеся в NBT и состоянии блока.
	 */
	public final void readComponents(ComponentMap defaultComponents, ComponentChanges components) {
		final Set<ComponentType<?>> readTypes = new HashSet<>();
		readTypes.add(DataComponentTypes.BLOCK_ENTITY_DATA);
		readTypes.add(DataComponentTypes.BLOCK_STATE);

		final ComponentMap componentMap = MergedComponentMap.create(defaultComponents, components);
		readComponents(new ComponentsAccess() {
			@Override
			public <T> @Nullable T get(ComponentType<? extends T> type) {
				readTypes.add(type);
				return componentMap.get(type);
			}

			@Override
			public <T> T getOrDefault(ComponentType<? extends T> type, T fallback) {
				readTypes.add(type);
				return componentMap.getOrDefault(type, fallback);
			}
		});

		ComponentChanges filteredChanges = components.withRemovedIf(readTypes::contains);
		this.components = filteredChanges.toAddedRemovedPair().added();
	}

	protected void addComponents(ComponentMap.Builder builder) {
	}

	/**
	 * @deprecated Метод-заглушка для совместимости; переопределяй при необходимости очистки данных при копировании стека.
	 */
	@Deprecated
	public void removeFromCopiedStackData(WriteView view) {
	}

	public final ComponentMap createComponentMap() {
		ComponentMap.Builder builder = ComponentMap.builder();
		builder.addAll(components);
		addComponents(builder);
		return builder.build();
	}

	public ComponentMap getComponents() {
		return components;
	}

	public void setComponents(ComponentMap components) {
		this.components = components;
	}

	public static @Nullable Text tryParseCustomName(ReadView view, String key) {
		return view.<Text>read(key, TextCodecs.CODEC).orElse(null);
	}

	public ErrorReporter.Context getReporterContext() {
		return new ReporterContext(this);
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
	}

	record ReporterContext(BlockEntity blockEntity) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return blockEntity.getNameForReport() + "@" + blockEntity.getPos();
		}
	}
}
