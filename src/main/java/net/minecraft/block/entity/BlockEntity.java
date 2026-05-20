package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.MergedComponentMap;
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
         throw new IllegalStateException("Invalid block entity " + this.getNameForReport() + " state at " + this.pos + ", got " + state);
      }
   }

   public boolean supports(BlockState state) {
      return this.type.supports(state);
   }

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

   protected void readData(ReadView view) {
   }

   public final void read(ReadView view) {
      this.readData(view);
      this.components = view.<ComponentMap>read("components", ComponentMap.CODEC).orElse(ComponentMap.EMPTY);
   }

   public final void readComponentlessData(ReadView view) {
      this.readData(view);
   }

   protected void writeData(WriteView view) {
   }

   public final NbtCompound createNbtWithIdentifyingData(RegistryWrapper.WrapperLookup registries) {
      NbtCompound var4;
      try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this.getReporterContext(), LOGGER)) {
         NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
         this.writeFullData(nbtWriteView);
         var4 = nbtWriteView.getNbt();
      }

      return var4;
   }

   public void writeFullData(WriteView view) {
      this.writeDataWithoutId(view);
      this.writeIdentifyingData(view);
   }

   public void writeDataWithId(WriteView view) {
      this.writeDataWithoutId(view);
      this.writeId(view);
   }

   public final NbtCompound createNbt(RegistryWrapper.WrapperLookup registries) {
      NbtCompound var4;
      try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this.getReporterContext(), LOGGER)) {
         NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
         this.writeDataWithoutId(nbtWriteView);
         var4 = nbtWriteView.getNbt();
      }

      return var4;
   }

   public void writeDataWithoutId(WriteView data) {
      this.writeData(data);
      data.put("components", ComponentMap.CODEC, this.components);
   }

   public final NbtCompound createComponentlessNbt(RegistryWrapper.WrapperLookup registries) {
      NbtCompound var4;
      try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this.getReporterContext(), LOGGER)) {
         NbtWriteView nbtWriteView = NbtWriteView.create(logging, registries);
         this.writeComponentlessData(nbtWriteView);
         var4 = nbtWriteView.getNbt();
      }

      return var4;
   }

   public void writeComponentlessData(WriteView view) {
      this.writeData(view);
   }

   private void writeId(WriteView view) {
      writeId(view, this.getType());
   }

   public static void writeId(WriteView view, BlockEntityType<?> type) {
      view.put("id", TYPE_CODEC, type);
   }

   private void writeIdentifyingData(WriteView view) {
      this.writeId(view);
      view.putInt("x", this.pos.getX());
      view.putInt("y", this.pos.getY());
      view.putInt("z", this.pos.getZ());
   }

   public static @Nullable BlockEntity createFromNbt(BlockPos pos, BlockState state, NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      BlockEntityType<?> blockEntityType = nbt.<BlockEntityType<?>>get("id", TYPE_CODEC).orElse(null);
      if (blockEntityType == null) {
         LOGGER.error("Skipping block entity with invalid type: {}", nbt.get("id"));
         return null;
      } else {
         BlockEntity blockEntity;
         try {
            blockEntity = blockEntityType.instantiate(pos, state);
         } catch (Throwable var12) {
            LOGGER.error("Failed to create block entity {} for block {} at position {} ", new Object[]{blockEntityType, pos, state, var12});
            return null;
         }

         try {
            BlockEntity var7;
            try (ErrorReporter.Logging logging = new ErrorReporter.Logging(blockEntity.getReporterContext(), LOGGER)) {
               blockEntity.read(NbtReadView.create(logging, registries, nbt));
               var7 = blockEntity;
            }

            return var7;
         } catch (Throwable var11) {
            LOGGER.error("Failed to load data for block entity {} for block {} at position {}", new Object[]{blockEntityType, pos, state, var11});
            return null;
         }
      }
   }

   public void markDirty() {
      if (this.world != null) {
         markDirty(this.world, this.pos, this.cachedState);
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
      this.removed = true;
   }

   public void cancelRemoval() {
      this.removed = false;
   }

   public void onBlockReplaced(BlockPos pos, BlockState oldState) {
      if (this instanceof Inventory inventory && this.world != null) {
         ItemScatterer.spawn(this.world, pos, inventory);
      }
   }

   public boolean onSyncedBlockEvent(int type, int data) {
      return false;
   }

   public void populateCrashReport(CrashReportSection crashReportSection) {
      crashReportSection.add("Name", this::getNameForReport);
      crashReportSection.add("Cached block", this.getCachedState()::toString);
      if (this.world == null) {
         crashReportSection.add("Block location", () -> this.pos + " (world missing)");
      } else {
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

   protected void readComponents(ComponentsAccess components) {
   }

   public final void readComponents(ItemStack stack) {
      this.readComponents(stack.getDefaultComponents(), stack.getComponentChanges());
   }

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

   protected void addComponents(ComponentMap.Builder builder) {
   }

   @Deprecated
   public void removeFromCopiedStackData(WriteView view) {
   }

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

   public static @Nullable Text tryParseCustomName(ReadView view, String key) {
      return view.<Text>read(key, TextCodecs.CODEC).orElse(null);
   }

   public ErrorReporter.Context getReporterContext() {
      return new BlockEntity.ReporterContext(this);
   }

   @Override
   public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
   }

   record ReporterContext(BlockEntity blockEntity) implements ErrorReporter.Context {
      @Override
      public String getName() {
         return this.blockEntity.getNameForReport() + "@" + this.blockEntity.getPos();
      }
   }
}
