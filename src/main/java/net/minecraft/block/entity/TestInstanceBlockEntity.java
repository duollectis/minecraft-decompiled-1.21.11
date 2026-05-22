package net.minecraft.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
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
import net.minecraft.test.*;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.*;
import net.minecraft.util.path.PathUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Блок-сущность тестового экземпляра ({@code test_instance_block}).
 * Управляет жизненным циклом одного игрового теста: хранит привязанный тест, его статус,
 * размер структуры, вращение и список ошибок. Отображает цветной луч в зависимости от результата.
 */
public class TestInstanceBlockEntity extends BlockEntity implements BeamEmitter, StructureBoxRendering {

	private static final Text INVALID_TEST_TEXT = Text.translatable("test_instance_block.invalid_test");
	private static final List<BeamEmitter.BeamSegment> CLEARED_BEAM_SEGMENTS = List.of();
	private static final List<BeamEmitter.BeamSegment>
			RUNNING_BEAM_SEGMENTS =
			List.of(new BeamEmitter.BeamSegment(ColorHelper.getArgb(128, 128, 128)));
	private static final List<BeamEmitter.BeamSegment>
			SUCCESS_BEAM_SEGMENTS =
			List.of(new BeamEmitter.BeamSegment(ColorHelper.getArgb(0, 255, 0)));
	private static final List<BeamEmitter.BeamSegment>
			REQUIRED_FAIL_BEAM_SEGMENTS =
			List.of(new BeamEmitter.BeamSegment(ColorHelper.getArgb(255, 0, 0)));
	private static final List<BeamEmitter.BeamSegment>
			OPTIONAL_FAIL_BEAM_SEGMENTS =
			List.of(new BeamEmitter.BeamSegment(ColorHelper.getArgb(255, 128, 0)));
	private static final Vec3i STRUCTURE_OFFSET = new Vec3i(0, 1, 1);
	private TestInstanceBlockEntity.Data data;
	private final List<TestInstanceBlockEntity.Error> errors = new ArrayList<>();

	public TestInstanceBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.TEST_INSTANCE_BLOCK, pos, state);
		data = new Data(
				Optional.empty(),
				Vec3i.ZERO,
				BlockRotation.NONE,
				false,
				Status.CLEARED,
				Optional.empty()
		);
	}

	public void setData(Data data) {
		this.data = data;
		markDirty();
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

	private static Optional<StructureTemplate> getStructureTemplate(
			ServerWorld world,
			RegistryKey<TestInstance> testInstance
	) {
		return world.getRegistryManager()
		            .getOptionalEntry(testInstance)
		            .map(entry -> entry.value().getStructure())
		            .flatMap(structureId -> world.getStructureTemplateManager().getTemplate(structureId));
	}

	public Optional<RegistryKey<TestInstance>> getTestKey() {
		return data.test();
	}

	public Text getTestName() {
		return getTestKey()
				.<Text>map(key -> Text.literal(key.getValue().toString()))
				.orElse(INVALID_TEST_TEXT);
	}

	private Optional<RegistryEntry.Reference<TestInstance>> getTestEntry() {
		return getTestKey().flatMap(world.getRegistryManager()::getOptionalEntry);
	}

	public boolean shouldIgnoreEntities() {
		return data.ignoreEntities();
	}

	public Vec3i getSize() {
		return data.size();
	}

	public BlockRotation getRotation() {
		return getTestEntry()
				.map(RegistryEntry::value)
				.map(TestInstance::getRotation)
				.orElse(BlockRotation.NONE)
				.rotate(data.rotation());
	}

	public Optional<Text> getErrorMessage() {
		return data.errorMessage();
	}

	public void setErrorMessage(Text errorMessage) {
		setData(data.withErrorMessage(errorMessage));
	}

	public void setFinished() {
		setData(data.withStatus(Status.FINISHED));
	}

	public void setRunning() {
		setData(data.withStatus(Status.RUNNING));
	}

	@Override
	public void markDirty() {
		super.markDirty();
		if (world instanceof ServerWorld) {
			world.updateListeners(getPos(), Blocks.AIR.getDefaultState(), getCachedState(), 3);
		}
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	@Override
	protected void readData(ReadView view) {
		view.<Data>read("data", Data.CODEC).ifPresent(this::setData);
		errors.clear();
		errors.addAll(
				view.<List<Error>>read("errors", Error.LIST_CODEC)
						.orElse(List.of())
		);
	}

	@Override
	protected void writeData(WriteView view) {
		view.put("data", Data.CODEC, data);
		if (!errors.isEmpty()) {
			view.put("errors", Error.LIST_CODEC, errors);
		}
	}

	@Override
	public StructureBoxRendering.RenderMode getRenderMode() {
		return StructureBoxRendering.RenderMode.BOX;
	}

	public BlockPos getStructurePos() {
		return getStructurePos(getPos());
	}

	public static BlockPos getStructurePos(BlockPos pos) {
		return pos.add(STRUCTURE_OFFSET);
	}

	@Override
	public StructureBoxRendering.StructureBox getStructureBox() {
		return new StructureBoxRendering.StructureBox(new BlockPos(STRUCTURE_OFFSET), getTransformedSize());
	}

	@Override
	public List<BeamEmitter.BeamSegment> getBeamSegments() {
		return switch (data.status()) {
			case CLEARED -> CLEARED_BEAM_SEGMENTS;
			case RUNNING -> RUNNING_BEAM_SEGMENTS;
			case FINISHED -> getErrorMessage().isEmpty()
					? SUCCESS_BEAM_SEGMENTS
					: (
							getTestEntry()
									.map(RegistryEntry::value)
									.map(TestInstance::isRequired)
									.orElse(true)
							? REQUIRED_FAIL_BEAM_SEGMENTS
							: OPTIONAL_FAIL_BEAM_SEGMENTS
					);
		};
	}

	private Vec3i getTransformedSize() {
		Vec3i size = getSize();
		BlockRotation rotation = getRotation();
		boolean isRotated90 = rotation == BlockRotation.CLOCKWISE_90
				|| rotation == BlockRotation.COUNTERCLOCKWISE_90;

		int sizeX = isRotated90 ? size.getZ() : size.getX();
		int sizeZ = isRotated90 ? size.getX() : size.getZ();

		return new Vec3i(sizeX, size.getY(), sizeZ);
	}

	public void reset(Consumer<Text> messageConsumer) {
		clearBarriers();
		clearErrors();
		boolean placed = placeStructure();

		if (placed) {
			messageConsumer.accept(
					Text.translatable("test_instance_block.reset_success", getTestName())
							.formatted(Formatting.GREEN)
			);
		}

		setData(data.withStatus(Status.CLEARED));
	}

	/**
	 * Определяет идентификатор структуры для сохранения: берёт из зарегистрированного теста
	 * или из ключа теста напрямую, если тест ещё не зарегистрирован.
	 */
	public Optional<Identifier> saveStructure(Consumer<Text> messageConsumer) {
		Optional<RegistryEntry.Reference<TestInstance>> testEntry = getTestEntry();
		Optional<Identifier> structureId = testEntry.isPresent()
				? Optional.of(testEntry.get().value().getStructure())
				: getTestKey().map(RegistryKey::getValue);

		if (structureId.isEmpty()) {
			BlockPos blockPos = getPos();
			messageConsumer.accept(
					Text.translatable(
							"test_instance_block.error.unable_to_save",
							blockPos.getX(),
							blockPos.getY(),
							blockPos.getZ()
					).formatted(Formatting.RED)
			);
			return structureId;
		}

		if (world instanceof ServerWorld serverWorld) {
			StructureBlockBlockEntity.saveStructure(
					serverWorld,
					structureId.get(),
					getStructurePos(),
					getSize(),
					shouldIgnoreEntities(),
					"",
					true,
					List.of(Blocks.AIR)
			);
		}

		return structureId;
	}

	public boolean export(Consumer<Text> messageConsumer) {
		Optional<Identifier> structureId = saveStructure(messageConsumer);
		return structureId.isPresent() && world instanceof ServerWorld serverWorld
				? exportData(serverWorld, structureId.get(), messageConsumer)
				: false;
	}

	public static boolean exportData(ServerWorld world, Identifier structureId, Consumer<Text> messageConsumer) {
		Path structuresDir = TestInstanceUtil.testStructuresDirectoryName;
		Path nbtPath = world.getStructureTemplateManager().getTemplatePath(structureId, ".nbt");
		Path snbtPath = NbtProvider.convertNbtToSnbt(
				DataWriter.UNCACHED,
				nbtPath,
				structureId.getPath(),
				structuresDir.resolve(structureId.getNamespace()).resolve("structure")
		);

		if (snbtPath == null) {
			messageConsumer.accept(Text.literal("Failed to export " + nbtPath).formatted(Formatting.RED));
			return true;
		}

		try {
			PathUtil.createDirectories(snbtPath.getParent());
		} catch (IOException exception) {
			messageConsumer.accept(
					Text.literal("Could not create folder " + snbtPath.getParent())
							.formatted(Formatting.RED)
			);
			return true;
		}

		messageConsumer.accept(Text.literal("Exported " + structureId + " to " + snbtPath.toAbsolutePath()));
		return false;
	}

	public void start(Consumer<Text> messageConsumer) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}

		Optional<RegistryEntry.Reference<TestInstance>> testEntry = getTestEntry();
		BlockPos blockPos = getPos();

		if (testEntry.isEmpty()) {
			messageConsumer.accept(
					Text.translatable(
							"test_instance_block.error.no_test",
							blockPos.getX(),
							blockPos.getY(),
							blockPos.getZ()
					).formatted(Formatting.RED)
			);
			return;
		}

		if (!placeStructure()) {
			messageConsumer.accept(
					Text.translatable(
							"test_instance_block.error.no_test_structure",
							blockPos.getX(),
							blockPos.getY(),
							blockPos.getZ()
					).formatted(Formatting.RED)
			);
			return;
		}

		clearErrors();
		TestManager.INSTANCE.clear();
		RuntimeTestInstances.clear();

		RegistryEntry.Reference<TestInstance> entry = testEntry.get();
		messageConsumer.accept(Text.translatable("test_instance_block.starting", entry.getIdAsString()));

		GameTestState gameTestState = new GameTestState(entry, data.rotation(), serverWorld, TestAttemptConfig.once());
		gameTestState.setTestBlockPos(blockPos);

		TestRunContext testRunContext = TestRunContext.Builder
				.ofStates(List.of(gameTestState), serverWorld)
				.build();
		TestCommand.start(serverWorld.getServer().getCommandSource(), testRunContext);
	}

	public boolean placeStructure() {
		if (!(world instanceof ServerWorld serverWorld)) {
			return false;
		}

		Optional<StructureTemplate> template = data
				.test()
				.flatMap(testKey -> getStructureTemplate(serverWorld, testKey));

		if (template.isEmpty()) {
			return false;
		}

		placeStructure(serverWorld, template.get());
		return true;
	}

	private void placeStructure(ServerWorld world, StructureTemplate template) {
		StructurePlacementData placementData = new StructurePlacementData()
				.setRotation(getRotation())
				.setIgnoreEntities(data.ignoreEntities())
				.setUpdateNeighbors(true);
		BlockPos startPos = getStartPos();

		setChunksForced();
		TestInstanceUtil.clearArea(getBlockBox(), world);
		discardEntities();
		template.place(world, startPos, startPos, placementData, world.getRandom(), 818);
	}

	private void discardEntities() {
		world.getOtherEntities(null, getBox())
				.stream()
				.filter(entity -> !(entity instanceof PlayerEntity))
				.forEach(Entity::discard);
	}

	private void setChunksForced() {
		if (world instanceof ServerWorld serverWorld) {
			getBlockBox().streamChunkPos().forEach(pos -> serverWorld.setChunkForced(pos.x, pos.z, true));
		}
	}

	public BlockPos getStartPos() {
		Vec3i size = getSize();
		BlockRotation rotation = getRotation();
		BlockPos structurePos = getStructurePos();

		return switch (rotation) {
			case NONE -> structurePos;
			case CLOCKWISE_90 -> structurePos.add(size.getZ() - 1, 0, 0);
			case CLOCKWISE_180 -> structurePos.add(size.getX() - 1, 0, size.getZ() - 1);
			case COUNTERCLOCKWISE_90 -> structurePos.add(0, 0, size.getX() - 1);
		};
	}

	public void placeBarriers() {
		forEachPos(pos -> {
			if (!world.getBlockState(pos).isOf(Blocks.TEST_INSTANCE_BLOCK)) {
				world.setBlockState(pos, Blocks.BARRIER.getDefaultState());
			}
		});
	}

	public void clearBarriers() {
		forEachPos(pos -> {
			if (world.getBlockState(pos).isOf(Blocks.BARRIER)) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState());
			}
		});
	}

	/**
	 * Итерирует все позиции ограничивающего бокса теста (стены, пол и, если тест требует
	 * доступа к небу, потолок), передавая каждую позицию в {@code posConsumer}.
	 */
	public void forEachPos(Consumer<BlockPos> posConsumer) {
		Box box = getBox();
		boolean includeRoof = !getTestEntry()
				.map(entry -> entry.value().requiresSkyAccess())
				.orElse(false);

		BlockPos minCorner = BlockPos.ofFloored(box.minX, box.minY, box.minZ).add(-1, -1, -1);
		BlockPos maxCorner = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);

		BlockPos.stream(minCorner, maxCorner).forEach(pos -> {
			boolean isWallOrFloor = pos.getX() == minCorner.getX()
					|| pos.getX() == maxCorner.getX()
					|| pos.getZ() == minCorner.getZ()
					|| pos.getZ() == maxCorner.getZ()
					|| pos.getY() == minCorner.getY();
			boolean isRoof = pos.getY() == maxCorner.getY();

			if (isWallOrFloor || isRoof && includeRoof) {
				posConsumer.accept(pos);
			}
		});
	}

	public void addError(BlockPos pos, Text message) {
		errors.add(new Error(pos, message));
		markDirty();
	}

	public void clearErrors() {
		if (!errors.isEmpty()) {
			errors.clear();
			markDirty();
		}
	}

	public List<Error> getErrors() {
		return errors;
	}

	public record Data(
			Optional<RegistryKey<TestInstance>> test,
			Vec3i size,
			BlockRotation rotation,
			boolean ignoreEntities,
			Status status,
			Optional<Text> errorMessage
	) {

		public static final Codec<Data> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						RegistryKey.createCodec(RegistryKeys.TEST_INSTANCE)
								.optionalFieldOf("test")
								.forGetter(Data::test),
						Vec3i.CODEC.fieldOf("size").forGetter(Data::size),
						BlockRotation.CODEC.fieldOf("rotation").forGetter(Data::rotation),
						Codec.BOOL.fieldOf("ignore_entities").forGetter(Data::ignoreEntities),
						Status.CODEC.fieldOf("status").forGetter(Data::status),
						TextCodecs.CODEC.optionalFieldOf("error_message").forGetter(Data::errorMessage)
				).apply(instance, Data::new)
		);

		public static final PacketCodec<RegistryByteBuf, Data> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.optional(RegistryKey.createPacketCodec(RegistryKeys.TEST_INSTANCE)),
				Data::test,
				Vec3i.PACKET_CODEC,
				Data::size,
				BlockRotation.PACKET_CODEC,
				Data::rotation,
				PacketCodecs.BOOLEAN,
				Data::ignoreEntities,
				Status.PACKET_CODEC,
				Data::status,
				PacketCodecs.optional(TextCodecs.REGISTRY_PACKET_CODEC),
				Data::errorMessage,
				Data::new
		);

		public Data withSize(Vec3i size) {
			return new Data(test, size, rotation, ignoreEntities, status, errorMessage);
		}

		public Data withStatus(Status status) {
			return new Data(test, this.size, rotation, ignoreEntities, status, Optional.empty());
		}

		public Data withErrorMessage(Text errorMessage) {
			return new Data(test, size, rotation, ignoreEntities, Status.FINISHED, Optional.of(errorMessage));
		}
	}

	public record Error(BlockPos pos, Text text) {

		public static final Codec<Error> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						BlockPos.CODEC.fieldOf("pos").forGetter(Error::pos),
						TextCodecs.CODEC.fieldOf("text").forGetter(Error::text)
				).apply(instance, Error::new)
		);

		public static final Codec<List<Error>> LIST_CODEC = CODEC.listOf();
	}

	public enum Status implements StringIdentifiable {
		CLEARED("cleared", 0),
		RUNNING("running", 1),
		FINISHED("finished", 2);

		private static final IntFunction<Status> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
				(Status status) -> status.index,
				values(),
				ValueLists.OutOfBoundsHandling.ZERO
		);

		public static final Codec<Status> CODEC = StringIdentifiable.createCodec(Status::values);

		public static final PacketCodec<ByteBuf, Status> PACKET_CODEC = PacketCodecs.indexed(
				Status::fromIndex,
				status -> status.index
		);

		private final String id;
		private final int index;

		Status(final String id, final int index) {
			this.id = id;
			this.index = index;
		}

		@Override
		public String asString() {
			return id;
		}

		public static Status fromIndex(int index) {
			return INDEX_MAPPER.apply(index);
		}
	}
}
