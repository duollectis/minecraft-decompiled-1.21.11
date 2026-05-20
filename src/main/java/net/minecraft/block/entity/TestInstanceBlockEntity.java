package net.minecraft.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataWriter;
import net.minecraft.data.dev.NbtProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.TestCommand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTestState;
import net.minecraft.test.RuntimeTestInstances;
import net.minecraft.test.TestAttemptConfig;
import net.minecraft.test.TestInstance;
import net.minecraft.test.TestInstanceUtil;
import net.minecraft.test.TestManager;
import net.minecraft.test.TestRunContext;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.path.PathUtil;

public class TestInstanceBlockEntity extends BlockEntity implements BeamEmitter, StructureBoxRendering {
   private static final Text INVALID_TEST_TEXT = Text.translatable("test_instance_block.invalid_test");
   private static final List<BeamEmitter.BeamSegment> CLEARED_BEAM_SEGMENTS = List.of();
   private static final List<BeamEmitter.BeamSegment> RUNNING_BEAM_SEGMENTS = List.of(new BeamEmitter.BeamSegment(ColorHelper.getArgb(128, 128, 128)));
   private static final List<BeamEmitter.BeamSegment> SUCCESS_BEAM_SEGMENTS = List.of(new BeamEmitter.BeamSegment(ColorHelper.getArgb(0, 255, 0)));
   private static final List<BeamEmitter.BeamSegment> REQUIRED_FAIL_BEAM_SEGMENTS = List.of(new BeamEmitter.BeamSegment(ColorHelper.getArgb(255, 0, 0)));
   private static final List<BeamEmitter.BeamSegment> OPTIONAL_FAIL_BEAM_SEGMENTS = List.of(new BeamEmitter.BeamSegment(ColorHelper.getArgb(255, 128, 0)));
   private static final Vec3i STRUCTURE_OFFSET = new Vec3i(0, 1, 1);
   private TestInstanceBlockEntity.Data data;
   private final List<TestInstanceBlockEntity.Error> errors = new ArrayList<>();

   public TestInstanceBlockEntity(BlockPos pos, BlockState state) {
      super(BlockEntityType.TEST_INSTANCE_BLOCK, pos, state);
      this.data = new TestInstanceBlockEntity.Data(
         Optional.empty(), Vec3i.ZERO, BlockRotation.NONE, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty()
      );
   }

   public void setData(TestInstanceBlockEntity.Data data) {
      this.data = data;
      this.markDirty();
   }

   public static Optional<Vec3i> getStructureSize(ServerWorld world, RegistryKey<TestInstance> testInstance) {
      return getStructureTemplate(world, testInstance).map(StructureTemplate::getSize);
   }

   public BlockBox getBlockBox() {
      BlockPos blockPos = this.getStructurePos();
      BlockPos blockPos2 = blockPos.add(this.getTransformedSize()).add(-1, -1, -1);
      return BlockBox.create(blockPos, blockPos2);
   }

   public Box getBox() {
      return Box.from(this.getBlockBox());
   }

   private static Optional<StructureTemplate> getStructureTemplate(ServerWorld world, RegistryKey<TestInstance> testInstance) {
      return world.getRegistryManager()
         .getOptionalEntry(testInstance)
         .map(entry -> entry.value().getStructure())
         .flatMap(structureId -> world.getStructureTemplateManager().getTemplate(structureId));
   }

   public Optional<RegistryKey<TestInstance>> getTestKey() {
      return this.data.test();
   }

   public Text getTestName() {
      return this.getTestKey()
            .<Text>map(key -> Text.literal(key.getValue().toString()))
            .orElse(INVALID_TEST_TEXT);
   }

   private Optional<RegistryEntry.Reference<TestInstance>> getTestEntry() {
      return this.getTestKey().flatMap(this.world.getRegistryManager()::getOptionalEntry);
   }

   public boolean shouldIgnoreEntities() {
      return this.data.ignoreEntities();
   }

   public Vec3i getSize() {
      return this.data.size();
   }

   public BlockRotation getRotation() {
      return this.getTestEntry().map(RegistryEntry::value).map(TestInstance::getRotation).orElse(BlockRotation.NONE).rotate(this.data.rotation());
   }

   public Optional<Text> getErrorMessage() {
      return this.data.errorMessage();
   }

   public void setErrorMessage(Text errorMessage) {
      this.setData(this.data.withErrorMessage(errorMessage));
   }

   public void setFinished() {
      this.setData(this.data.withStatus(TestInstanceBlockEntity.Status.FINISHED));
   }

   public void setRunning() {
      this.setData(this.data.withStatus(TestInstanceBlockEntity.Status.RUNNING));
   }

   @Override
   public void markDirty() {
      super.markDirty();
      if (this.world instanceof ServerWorld) {
         this.world.updateListeners(this.getPos(), Blocks.AIR.getDefaultState(), this.getCachedState(), 3);
      }
   }

   public BlockEntityUpdateS2CPacket toUpdatePacket() {
      return BlockEntityUpdateS2CPacket.create(this);
   }

   @Override
   public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
      return this.createComponentlessNbt(registries);
   }

   @Override
   protected void readData(ReadView view) {
      view.<TestInstanceBlockEntity.Data>read("data", TestInstanceBlockEntity.Data.CODEC).ifPresent(this::setData);
      this.errors.clear();
      this.errors.addAll(view.<List<TestInstanceBlockEntity.Error>>read("errors", TestInstanceBlockEntity.Error.LIST_CODEC).orElse(List.of()));
   }

   @Override
   protected void writeData(WriteView view) {
      view.put("data", TestInstanceBlockEntity.Data.CODEC, this.data);
      if (!this.errors.isEmpty()) {
         view.put("errors", TestInstanceBlockEntity.Error.LIST_CODEC, this.errors);
      }
   }

   @Override
   public StructureBoxRendering.RenderMode getRenderMode() {
      return StructureBoxRendering.RenderMode.BOX;
   }

   public BlockPos getStructurePos() {
      return getStructurePos(this.getPos());
   }

   public static BlockPos getStructurePos(BlockPos pos) {
      return pos.add(STRUCTURE_OFFSET);
   }

   @Override
   public StructureBoxRendering.StructureBox getStructureBox() {
      return new StructureBoxRendering.StructureBox(new BlockPos(STRUCTURE_OFFSET), this.getTransformedSize());
   }

   @Override
   public List<BeamEmitter.BeamSegment> getBeamSegments() {
      return switch (this.data.status()) {
         case CLEARED -> CLEARED_BEAM_SEGMENTS;
         case RUNNING -> RUNNING_BEAM_SEGMENTS;
         case FINISHED -> this.getErrorMessage().isEmpty()
            ? SUCCESS_BEAM_SEGMENTS
            : (
               this.getTestEntry().map(RegistryEntry::value).map(TestInstance::isRequired).orElse(true)
                  ? REQUIRED_FAIL_BEAM_SEGMENTS
                  : OPTIONAL_FAIL_BEAM_SEGMENTS
            );
      };
   }

   private Vec3i getTransformedSize() {
      Vec3i vec3i = this.getSize();
      BlockRotation blockRotation = this.getRotation();
      boolean bl = blockRotation == BlockRotation.CLOCKWISE_90 || blockRotation == BlockRotation.COUNTERCLOCKWISE_90;
      int i = bl ? vec3i.getZ() : vec3i.getX();
      int j = bl ? vec3i.getX() : vec3i.getZ();
      return new Vec3i(i, vec3i.getY(), j);
   }

   public void reset(Consumer<Text> messageConsumer) {
      this.clearBarriers();
      this.clearErrors();
      boolean bl = this.placeStructure();
      if (bl) {
         messageConsumer.accept(Text.translatable("test_instance_block.reset_success", this.getTestName()).formatted(Formatting.GREEN));
      }

      this.setData(this.data.withStatus(TestInstanceBlockEntity.Status.CLEARED));
   }

   public Optional<Identifier> saveStructure(Consumer<Text> messageConsumer) {
      Optional<RegistryEntry.Reference<TestInstance>> optional = this.getTestEntry();
      Optional<Identifier> optional2;
      if (optional.isPresent()) {
         optional2 = Optional.of(optional.get().value().getStructure());
      } else {
         optional2 = this.getTestKey().map(RegistryKey::getValue);
      }

      if (optional2.isEmpty()) {
         BlockPos blockPos = this.getPos();
         messageConsumer.accept(
            Text.translatable("test_instance_block.error.unable_to_save", blockPos.getX(), blockPos.getY(), blockPos.getZ()).formatted(Formatting.RED)
         );
         return optional2;
      } else {
         if (this.world instanceof ServerWorld serverWorld) {
            StructureBlockBlockEntity.saveStructure(
               serverWorld, optional2.get(), this.getStructurePos(), this.getSize(), this.shouldIgnoreEntities(), "", true, List.of(Blocks.AIR)
            );
         }

         return optional2;
      }
   }

   public boolean export(Consumer<Text> messageConsumer) {
      Optional<Identifier> optional = this.saveStructure(messageConsumer);
      return !optional.isEmpty() && this.world instanceof ServerWorld serverWorld ? exportData(serverWorld, optional.get(), messageConsumer) : false;
   }

   public static boolean exportData(ServerWorld world, Identifier structureId, Consumer<Text> messageConsumer) {
      Path path = TestInstanceUtil.testStructuresDirectoryName;
      Path path2 = world.getStructureTemplateManager().getTemplatePath(structureId, ".nbt");
      Path path3 = NbtProvider.convertNbtToSnbt(
         DataWriter.UNCACHED, path2, structureId.getPath(), path.resolve(structureId.getNamespace()).resolve("structure")
      );
      if (path3 == null) {
         messageConsumer.accept(Text.literal("Failed to export " + path2).formatted(Formatting.RED));
         return true;
      } else {
         try {
            PathUtil.createDirectories(path3.getParent());
         } catch (IOException var7) {
            messageConsumer.accept(Text.literal("Could not create folder " + path3.getParent()).formatted(Formatting.RED));
            return true;
         }

         messageConsumer.accept(Text.literal("Exported " + structureId + " to " + path3.toAbsolutePath()));
         return false;
      }
   }

   public void start(Consumer<Text> messageConsumer) {
      if (this.world instanceof ServerWorld serverWorld) {
         Optional var7 = this.getTestEntry();
         BlockPos blockPos = this.getPos();
         if (var7.isEmpty()) {
            messageConsumer.accept(
               Text.translatable("test_instance_block.error.no_test", blockPos.getX(), blockPos.getY(), blockPos.getZ()).formatted(Formatting.RED)
            );
         } else if (!this.placeStructure()) {
            messageConsumer.accept(
               Text.translatable("test_instance_block.error.no_test_structure", blockPos.getX(), blockPos.getY(), blockPos.getZ()).formatted(Formatting.RED)
            );
         } else {
            this.clearErrors();
            TestManager.INSTANCE.clear();
            RuntimeTestInstances.clear();
            messageConsumer.accept(Text.translatable("test_instance_block.starting", ((RegistryEntry.Reference)var7.get()).getIdAsString()));
            GameTestState gameTestState = new GameTestState(
               (RegistryEntry.Reference<TestInstance>)var7.get(), this.data.rotation(), serverWorld, TestAttemptConfig.once()
            );
            gameTestState.setTestBlockPos(blockPos);
            TestRunContext testRunContext = TestRunContext.Builder.ofStates(List.of(gameTestState), serverWorld).build();
            TestCommand.start(serverWorld.getServer().getCommandSource(), testRunContext);
         }
      }
   }

   public boolean placeStructure() {
      if (this.world instanceof ServerWorld serverWorld) {
         Optional<StructureTemplate> optional = this.data.test().flatMap(template -> getStructureTemplate(serverWorld, (RegistryKey<TestInstance>)template));
         if (optional.isPresent()) {
            this.placeStructure(serverWorld, optional.get());
            return true;
         }
      }

      return false;
   }

   private void placeStructure(ServerWorld world, StructureTemplate template) {
      StructurePlacementData structurePlacementData = new StructurePlacementData()
         .setRotation(this.getRotation())
         .setIgnoreEntities(this.data.ignoreEntities())
         .setUpdateNeighbors(true);
      BlockPos blockPos = this.getStartPos();
      this.setChunksForced();
      TestInstanceUtil.clearArea(this.getBlockBox(), world);
      this.discardEntities();
      template.place(world, blockPos, blockPos, structurePlacementData, world.getRandom(), 818);
   }

   private void discardEntities() {
      this.world.getOtherEntities(null, this.getBox()).stream().filter(entity -> !(entity instanceof PlayerEntity)).forEach(Entity::discard);
   }

   private void setChunksForced() {
      if (this.world instanceof ServerWorld serverWorld) {
         this.getBlockBox().streamChunkPos().forEach(pos -> serverWorld.setChunkForced(pos.x, pos.z, true));
      }
   }

   public BlockPos getStartPos() {
      Vec3i vec3i = this.getSize();
      BlockRotation blockRotation = this.getRotation();
      BlockPos blockPos = this.getStructurePos();

      return switch (blockRotation) {
         case NONE -> blockPos;
         case CLOCKWISE_90 -> blockPos.add(vec3i.getZ() - 1, 0, 0);
         case CLOCKWISE_180 -> blockPos.add(vec3i.getX() - 1, 0, vec3i.getZ() - 1);
         case COUNTERCLOCKWISE_90 -> blockPos.add(0, 0, vec3i.getX() - 1);
      };
   }

   public void placeBarriers() {
      this.forEachPos(pos -> {
         if (!this.world.getBlockState(pos).isOf(Blocks.TEST_INSTANCE_BLOCK)) {
            this.world.setBlockState(pos, Blocks.BARRIER.getDefaultState());
         }
      });
   }

   public void clearBarriers() {
      this.forEachPos(pos -> {
         if (this.world.getBlockState(pos).isOf(Blocks.BARRIER)) {
            this.world.setBlockState(pos, Blocks.AIR.getDefaultState());
         }
      });
   }

   public void forEachPos(Consumer<BlockPos> posConsumer) {
      Box box = this.getBox();
      boolean bl = !this.getTestEntry().map(entry -> entry.value().requiresSkyAccess()).orElse(false);
      BlockPos blockPos = BlockPos.ofFloored(box.minX, box.minY, box.minZ).add(-1, -1, -1);
      BlockPos blockPos2 = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);
      BlockPos.stream(blockPos, blockPos2)
         .forEach(
            pos -> {
               boolean bl2 = pos.getX() == blockPos.getX()
                  || pos.getX() == blockPos2.getX()
                  || pos.getZ() == blockPos.getZ()
                  || pos.getZ() == blockPos2.getZ()
                  || pos.getY() == blockPos.getY();
               boolean bl3 = pos.getY() == blockPos2.getY();
               if (bl2 || bl3 && bl) {
                  posConsumer.accept(pos);
               }
            }
         );
   }

   public void addError(BlockPos pos, Text message) {
      this.errors.add(new TestInstanceBlockEntity.Error(pos, message));
      this.markDirty();
   }

   public void clearErrors() {
      if (!this.errors.isEmpty()) {
         this.errors.clear();
         this.markDirty();
      }
   }

   public List<TestInstanceBlockEntity.Error> getErrors() {
      return this.errors;
   }

   public record Data(
      Optional<RegistryKey<TestInstance>> test,
      Vec3i size,
      BlockRotation rotation,
      boolean ignoreEntities,
      TestInstanceBlockEntity.Status status,
      Optional<Text> errorMessage
   ) {
      public static final Codec<TestInstanceBlockEntity.Data> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               RegistryKey.createCodec(RegistryKeys.TEST_INSTANCE).optionalFieldOf("test").forGetter(TestInstanceBlockEntity.Data::test),
               Vec3i.CODEC.fieldOf("size").forGetter(TestInstanceBlockEntity.Data::size),
               BlockRotation.CODEC.fieldOf("rotation").forGetter(TestInstanceBlockEntity.Data::rotation),
               Codec.BOOL.fieldOf("ignore_entities").forGetter(TestInstanceBlockEntity.Data::ignoreEntities),
               TestInstanceBlockEntity.Status.CODEC.fieldOf("status").forGetter(TestInstanceBlockEntity.Data::status),
               TextCodecs.CODEC.optionalFieldOf("error_message").forGetter(TestInstanceBlockEntity.Data::errorMessage)
            )
            .apply(instance, TestInstanceBlockEntity.Data::new)
      );
      public static final PacketCodec<RegistryByteBuf, TestInstanceBlockEntity.Data> PACKET_CODEC = PacketCodec.tuple(
         PacketCodecs.optional(RegistryKey.createPacketCodec(RegistryKeys.TEST_INSTANCE)),
         TestInstanceBlockEntity.Data::test,
         Vec3i.PACKET_CODEC,
         TestInstanceBlockEntity.Data::size,
         BlockRotation.PACKET_CODEC,
         TestInstanceBlockEntity.Data::rotation,
         PacketCodecs.BOOLEAN,
         TestInstanceBlockEntity.Data::ignoreEntities,
         TestInstanceBlockEntity.Status.PACKET_CODEC,
         TestInstanceBlockEntity.Data::status,
         PacketCodecs.optional(TextCodecs.REGISTRY_PACKET_CODEC),
         TestInstanceBlockEntity.Data::errorMessage,
         TestInstanceBlockEntity.Data::new
      );

      public TestInstanceBlockEntity.Data withSize(Vec3i size) {
         return new TestInstanceBlockEntity.Data(this.test, size, this.rotation, this.ignoreEntities, this.status, this.errorMessage);
      }

      public TestInstanceBlockEntity.Data withStatus(TestInstanceBlockEntity.Status status) {
         return new TestInstanceBlockEntity.Data(this.test, this.size, this.rotation, this.ignoreEntities, status, Optional.empty());
      }

      public TestInstanceBlockEntity.Data withErrorMessage(Text errorMessage) {
         return new TestInstanceBlockEntity.Data(
            this.test, this.size, this.rotation, this.ignoreEntities, TestInstanceBlockEntity.Status.FINISHED, Optional.of(errorMessage)
         );
      }
   }

   public record Error(BlockPos pos, Text text) {
      public static final Codec<TestInstanceBlockEntity.Error> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(TestInstanceBlockEntity.Error::pos),
               TextCodecs.CODEC.fieldOf("text").forGetter(TestInstanceBlockEntity.Error::text)
            )
            .apply(instance, TestInstanceBlockEntity.Error::new)
      );
      public static final Codec<List<TestInstanceBlockEntity.Error>> LIST_CODEC = CODEC.listOf();
   }

   public static enum Status implements StringIdentifiable {
      CLEARED("cleared", 0),
      RUNNING("running", 1),
      FINISHED("finished", 2);

      private static final IntFunction<TestInstanceBlockEntity.Status> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
         (TestInstanceBlockEntity.Status status) -> status.index, values(), ValueLists.OutOfBoundsHandling.ZERO
      );
      public static final Codec<TestInstanceBlockEntity.Status> CODEC = StringIdentifiable.createCodec(TestInstanceBlockEntity.Status::values);
      public static final PacketCodec<ByteBuf, TestInstanceBlockEntity.Status> PACKET_CODEC = PacketCodecs.indexed(
         TestInstanceBlockEntity.Status::fromIndex, status -> status.index
      );
      private final String id;
      private final int index;

      private Status(final String id, final int index) {
         this.id = id;
         this.index = index;
      }

      @Override
      public String asString() {
         return this.id;
      }

      public static TestInstanceBlockEntity.Status fromIndex(int index) {
         return INDEX_MAPPER.apply(index);
      }
   }
}
