package net.minecraft.block.entity;

import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.BlockRotStructureProcessor;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Блок-сущность блока структуры. Управляет сохранением, загрузкой и сканированием
 * структурных шаблонов. Поддерживает режимы SAVE, LOAD, CORNER и DATA.
 */
public class StructureBlockBlockEntity extends BlockEntity implements StructureBoxRendering {

	private static final int DEFAULT_AUTHOR_MAX_LENGTH = 5;
	public static final int MAX_STRUCTURE_SIZE = 48;
	public static final int MAX_STRUCTURE_OFFSET = 48;
	public static final String AUTHOR_KEY = "author";
	private static final String DEFAULT_AUTHOR = "";
	private static final String DEFAULT_METADATA = "";
	private static final BlockPos DEFAULT_OFFSET = new BlockPos(0, 1, 0);
	private static final Vec3i DEFAULT_SIZE = Vec3i.ZERO;
	private static final BlockRotation DEFAULT_ROTATION = BlockRotation.NONE;
	private static final BlockMirror DEFAULT_MIRROR = BlockMirror.NONE;
	private static final boolean DEFAULT_IGNORE_ENTITIES = true;
	private static final boolean DEFAULT_STRICT = false;
	private static final boolean DEFAULT_POWERED = false;
	private static final boolean DEFAULT_SHOW_AIR = false;
	private static final boolean DEFAULT_SHOW_BOUNDING_BOX = true;
	private static final float DEFAULT_INTEGRITY = 1.0F;
	private static final long DEFAULT_SEED = 0L;
	private @Nullable Identifier templateName;
	private String author = "";
	private String metadata = "";
	private BlockPos offset = DEFAULT_OFFSET;
	private Vec3i size = DEFAULT_SIZE;
	private BlockMirror mirror = BlockMirror.NONE;
	private BlockRotation rotation = BlockRotation.NONE;
	private StructureBlockMode mode;
	private boolean ignoreEntities = true;
	private boolean strict = false;
	private boolean powered = false;
	private boolean showAir = false;
	private boolean showBoundingBox = true;
	private float integrity = 1.0F;
	private long seed = 0L;

	public StructureBlockBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.STRUCTURE_BLOCK, pos, state);
		this.mode = state.get(StructureBlock.MODE);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putString("name", this.getTemplateName());
		view.putString("author", this.author);
		view.putString("metadata", this.metadata);
		view.putInt("posX", this.offset.getX());
		view.putInt("posY", this.offset.getY());
		view.putInt("posZ", this.offset.getZ());
		view.putInt("sizeX", this.size.getX());
		view.putInt("sizeY", this.size.getY());
		view.putInt("sizeZ", this.size.getZ());
		view.put("rotation", BlockRotation.ENUM_NAME_CODEC, this.rotation);
		view.put("mirror", BlockMirror.ENUM_NAME_CODEC, this.mirror);
		view.put("mode", StructureBlockMode.CODEC, this.mode);
		view.putBoolean("ignoreEntities", this.ignoreEntities);
		view.putBoolean("strict", this.strict);
		view.putBoolean("powered", this.powered);
		view.putBoolean("showair", this.showAir);
		view.putBoolean("showboundingbox", this.showBoundingBox);
		view.putFloat("integrity", this.integrity);
		view.putLong("seed", this.seed);
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		setTemplateName(view.getString("name", ""));
		author = view.getString("author", "");
		metadata = view.getString("metadata", "");
		int offsetX = MathHelper.clamp(view.getInt("posX", DEFAULT_OFFSET.getX()), -MAX_STRUCTURE_OFFSET, MAX_STRUCTURE_OFFSET);
		int offsetY = MathHelper.clamp(view.getInt("posY", DEFAULT_OFFSET.getY()), -MAX_STRUCTURE_OFFSET, MAX_STRUCTURE_OFFSET);
		int offsetZ = MathHelper.clamp(view.getInt("posZ", DEFAULT_OFFSET.getZ()), -MAX_STRUCTURE_OFFSET, MAX_STRUCTURE_OFFSET);
		offset = new BlockPos(offsetX, offsetY, offsetZ);
		int sizeX = MathHelper.clamp(view.getInt("sizeX", DEFAULT_SIZE.getX()), 0, MAX_STRUCTURE_SIZE);
		int sizeY = MathHelper.clamp(view.getInt("sizeY", DEFAULT_SIZE.getY()), 0, MAX_STRUCTURE_SIZE);
		int sizeZ = MathHelper.clamp(view.getInt("sizeZ", DEFAULT_SIZE.getZ()), 0, MAX_STRUCTURE_SIZE);
		size = new Vec3i(sizeX, sizeY, sizeZ);
		rotation = view.<BlockRotation>read("rotation", BlockRotation.ENUM_NAME_CODEC).orElse(DEFAULT_ROTATION);
		mirror = view.<BlockMirror>read("mirror", BlockMirror.ENUM_NAME_CODEC).orElse(DEFAULT_MIRROR);
		mode = view.<StructureBlockMode>read("mode", StructureBlockMode.CODEC).orElse(StructureBlockMode.DATA);
		ignoreEntities = view.getBoolean("ignoreEntities", DEFAULT_IGNORE_ENTITIES);
		strict = view.getBoolean("strict", DEFAULT_STRICT);
		powered = view.getBoolean("powered", DEFAULT_POWERED);
		showAir = view.getBoolean("showair", DEFAULT_SHOW_AIR);
		showBoundingBox = view.getBoolean("showboundingbox", DEFAULT_SHOW_BOUNDING_BOX);
		integrity = view.getFloat("integrity", DEFAULT_INTEGRITY);
		seed = view.getLong("seed", DEFAULT_SEED);
		updateBlockMode();
	}

	private void updateBlockMode() {
		if (world == null) {
			return;
		}

		BlockPos currentPos = getPos();
		BlockState currentState = world.getBlockState(currentPos);
		if (currentState.isOf(Blocks.STRUCTURE_BLOCK)) {
			world.setBlockState(currentPos, currentState.with(StructureBlock.MODE, mode), 2);
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

	public boolean openScreen(PlayerEntity player) {
		if (!player.isCreativeLevelTwoOp()) {
			return false;
		}

		if (player.getEntityWorld().isClient()) {
			player.openStructureBlockScreen(this);
		}

		return true;
	}

	public String getTemplateName() {
		return templateName == null ? "" : templateName.toString();
	}

	public boolean hasStructureName() {
		return templateName != null;
	}

	public void setTemplateName(@Nullable String templateName) {
		setTemplateName(StringHelper.isEmpty(templateName) ? null : Identifier.tryParse(templateName));
	}

	public void setTemplateName(@Nullable Identifier templateName) {
		this.templateName = templateName;
	}

	public void setAuthor(LivingEntity entity) {
		author = entity.getStringifiedName();
	}

	public BlockPos getOffset() {
		return offset;
	}

	public void setOffset(BlockPos offset) {
		this.offset = offset;
	}

	public Vec3i getSize() {
		return size;
	}

	public void setSize(Vec3i size) {
		this.size = size;
	}

	public BlockMirror getMirror() {
		return mirror;
	}

	public void setMirror(BlockMirror mirror) {
		this.mirror = mirror;
	}

	public BlockRotation getRotation() {
		return rotation;
	}

	public void setRotation(BlockRotation rotation) {
		this.rotation = rotation;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public StructureBlockMode getMode() {
		return mode;
	}

	public void setMode(StructureBlockMode mode) {
		this.mode = mode;
		BlockState currentState = world.getBlockState(getPos());
		if (currentState.isOf(Blocks.STRUCTURE_BLOCK)) {
			world.setBlockState(getPos(), currentState.with(StructureBlock.MODE, mode), 2);
		}
	}

	public boolean shouldIgnoreEntities() {
		return ignoreEntities;
	}

	public boolean isStrict() {
		return strict;
	}

	public void setIgnoreEntities(boolean ignoreEntities) {
		this.ignoreEntities = ignoreEntities;
	}

	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	public float getIntegrity() {
		return integrity;
	}

	public void setIntegrity(float integrity) {
		this.integrity = integrity;
	}

	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	/**
	 * Автоматически определяет размер структуры по угловым блокам с тем же именем.
	 * Сканирует область ±80 блоков и вычисляет ограничивающий прямоугольник.
	 *
	 * @return {@code true} если структура успешно обнаружена и размер обновлён
	 */
	public boolean detectStructureSize() {
		if (mode != StructureBlockMode.SAVE) {
			return false;
		}

		BlockPos currentPos = getPos();
		int scanRadius = 80;
		BlockPos scanMin = new BlockPos(currentPos.getX() - scanRadius, world.getBottomY(), currentPos.getZ() - scanRadius);
		BlockPos scanMax = new BlockPos(currentPos.getX() + scanRadius, world.getTopYInclusive(), currentPos.getZ() + scanRadius);
		Stream<BlockPos> corners = streamCornerPos(scanMin, scanMax);

		return getStructureBox(currentPos, corners).filter(box -> {
			int sizeX = box.getMaxX() - box.getMinX();
			int sizeY = box.getMaxY() - box.getMinY();
			int sizeZ = box.getMaxZ() - box.getMinZ();
			if (sizeX <= 1 || sizeY <= 1 || sizeZ <= 1) {
				return false;
			}

			offset = new BlockPos(
				box.getMinX() - currentPos.getX() + 1,
				box.getMinY() - currentPos.getY() + 1,
				box.getMinZ() - currentPos.getZ() + 1
			);
			size = new Vec3i(sizeX - 1, sizeY - 1, sizeZ - 1);
			markDirty();
			BlockState currentState = world.getBlockState(currentPos);
			world.updateListeners(currentPos, currentState, currentState, 3);
			return true;
		}).isPresent();
	}

	private Stream<BlockPos> streamCornerPos(BlockPos start, BlockPos end) {
		return BlockPos.stream(start, end)
			.filter(pos -> world.getBlockState(pos).isOf(Blocks.STRUCTURE_BLOCK))
			.map(world::getBlockEntity)
			.filter(entity -> entity instanceof StructureBlockBlockEntity)
			.map(entity -> (StructureBlockBlockEntity) entity)
			.filter(entity -> entity.mode == StructureBlockMode.CORNER
				&& Objects.equals(templateName, entity.templateName))
			.map(BlockEntity::getPos);
	}

	private static Optional<BlockBox> getStructureBox(BlockPos pos, Stream<BlockPos> corners) {
		Iterator<BlockPos> iterator = corners.iterator();
		if (!iterator.hasNext()) {
			return Optional.empty();
		}

		BlockPos first = iterator.next();
		BlockBox box = new BlockBox(first);
		if (iterator.hasNext()) {
			iterator.forEachRemaining(box::encompass);
		} else {
			box.encompass(pos);
		}

		return Optional.of(box);
	}

	public boolean saveStructure() {
		return mode != StructureBlockMode.SAVE ? false : saveStructure(true);
	}

	public boolean saveStructure(boolean toDisk) {
		if (templateName == null || !(world instanceof ServerWorld serverWorld)) {
			return false;
		}

		BlockPos startPos = getPos().add(offset);
		return saveStructure(
			serverWorld,
			templateName,
			startPos,
			size,
			ignoreEntities,
			author,
			toDisk,
			List.of()
		);
	}

	public static boolean saveStructure(
			ServerWorld world,
			Identifier templateId,
			BlockPos start,
			Vec3i size,
			boolean ignoreEntities,
			String author,
			boolean toDisk,
			List<Block> list
	) {
		StructureTemplateManager structureTemplateManager = world.getStructureTemplateManager();

		StructureTemplate structureTemplate;
		try {
			structureTemplate = structureTemplateManager.getTemplateOrBlank(templateId);
		}
		catch (InvalidIdentifierException var12) {
			return false;
		}

		structureTemplate.saveFromWorld(
				world,
				start,
				size,
				!ignoreEntities,
				Stream.concat(list.stream(), Stream.of(Blocks.STRUCTURE_VOID)).toList()
		);
		structureTemplate.setAuthor(author);
		if (toDisk) {
			try {
				return structureTemplateManager.saveTemplate(templateId);
			}
			catch (InvalidIdentifierException var11) {
				return false;
			}
		}
		else {
			return true;
		}
	}

	/**
	 * Создаёт генератор случайных чисел для размещения структуры.
	 * При seed=0 использует текущее время, иначе — фиксированный seed.
	 */
	public static Random createRandom(long seed) {
		return seed == 0L ? Random.create(Util.getMeasuringTimeMs()) : Random.create(seed);
	}

	public boolean loadAndTryPlaceStructure(ServerWorld world) {
		if (mode != StructureBlockMode.LOAD || templateName == null) {
			return false;
		}

		StructureTemplate template = world.getStructureTemplateManager().getTemplate(templateName).orElse(null);
		if (template == null) {
			return false;
		}

		if (template.getSize().equals(size)) {
			loadAndPlaceStructure(world, template);
			return true;
		}

		loadStructure(template);
		return false;
	}

	public boolean loadStructure(ServerWorld world) {
		StructureTemplate template = getStructureTemplate(world);
		if (template == null) {
			return false;
		}

		loadStructure(template);
		return true;
	}

	private void loadStructure(StructureTemplate template) {
		author = !StringHelper.isEmpty(template.getAuthor()) ? template.getAuthor() : "";
		size = template.getSize();
		markDirty();
	}

	public void loadAndPlaceStructure(ServerWorld world) {
		StructureTemplate template = getStructureTemplate(world);
		if (template != null) {
			loadAndPlaceStructure(world, template);
		}
	}

	private @Nullable StructureTemplate getStructureTemplate(ServerWorld world) {
		return templateName == null
			? null
			: world.getStructureTemplateManager().getTemplate(templateName).orElse(null);
	}

	private void loadAndPlaceStructure(ServerWorld world, StructureTemplate template) {
		loadStructure(template);
		StructurePlacementData placementData = new StructurePlacementData()
			.setMirror(mirror)
			.setRotation(rotation)
			.setIgnoreEntities(ignoreEntities)
			.setUpdateNeighbors(strict);
		if (integrity < 1.0F) {
			placementData.clearProcessors()
				.addProcessor(new BlockRotStructureProcessor(MathHelper.clamp(integrity, 0.0F, 1.0F)))
				.setRandom(createRandom(seed));
		}

		BlockPos startPos = getPos().add(offset);
		if (SharedConstants.STRUCTURE_EDIT_MODE) {
			BlockPos.iterate(startPos, startPos.add(size))
				.forEach(pos -> world.setBlockState(pos, Blocks.STRUCTURE_VOID.getDefaultState(), 2));
		}

		template.place(
			world,
			startPos,
			startPos,
			placementData,
			createRandom(seed),
			2 | (strict ? 816 : 0)
		);
	}

	public void unloadStructure() {
		if (templateName == null) {
			return;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		serverWorld.getStructureTemplateManager().unloadTemplate(templateName);
	}

	public boolean isStructureAvailable() {
		if (mode != StructureBlockMode.LOAD || world.isClient() || templateName == null) {
			return false;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		try {
			return serverWorld.getStructureTemplateManager().getTemplate(templateName).isPresent();
		} catch (InvalidIdentifierException ignored) {
			return false;
		}
	}

	public boolean isPowered() {
		return powered;
	}

	public void setPowered(boolean powered) {
		this.powered = powered;
	}

	public boolean shouldShowAir() {
		return showAir;
	}

	public void setShowAir(boolean showAir) {
		this.showAir = showAir;
	}

	public boolean shouldShowBoundingBox() {
		return showBoundingBox;
	}

	public void setShowBoundingBox(boolean showBoundingBox) {
		this.showBoundingBox = showBoundingBox;
	}

	@Override
	public StructureBoxRendering.RenderMode getRenderMode() {
		if (mode != StructureBlockMode.SAVE && mode != StructureBlockMode.LOAD) {
			return StructureBoxRendering.RenderMode.NONE;
		}

		if (mode == StructureBlockMode.SAVE && showAir) {
			return StructureBoxRendering.RenderMode.BOX_AND_INVISIBLE_BLOCKS;
		}

		return mode != StructureBlockMode.SAVE && !showBoundingBox
			? StructureBoxRendering.RenderMode.NONE
			: StructureBoxRendering.RenderMode.BOX;
	}

	@Override
	public StructureBoxRendering.StructureBox getStructureBox() {
		BlockPos offsetPos = getOffset();
		Vec3i structureSize = getSize();
		int offsetX = offsetPos.getX();
		int offsetZ = offsetPos.getZ();
		int offsetY = offsetPos.getY();
		int topY = offsetY + structureSize.getY();
		int mirroredX;
		int mirroredZ;
		switch (mirror) {
			case LEFT_RIGHT:
				mirroredX = structureSize.getX();
				mirroredZ = -structureSize.getZ();
				break;
			case FRONT_BACK:
				mirroredX = -structureSize.getX();
				mirroredZ = structureSize.getZ();
				break;
			default:
				mirroredX = structureSize.getX();
				mirroredZ = structureSize.getZ();
		}

		int startX;
		int startZ;
		int endX;
		int endZ;
		switch (rotation) {
			case CLOCKWISE_90:
				startX = mirroredZ < 0 ? offsetX : offsetX + 1;
				startZ = mirroredX < 0 ? offsetZ + 1 : offsetZ;
				endX = startX - mirroredZ;
				endZ = startZ + mirroredX;
				break;
			case CLOCKWISE_180:
				startX = mirroredX < 0 ? offsetX : offsetX + 1;
				startZ = mirroredZ < 0 ? offsetZ : offsetZ + 1;
				endX = startX - mirroredX;
				endZ = startZ - mirroredZ;
				break;
			case COUNTERCLOCKWISE_90:
				startX = mirroredZ < 0 ? offsetX + 1 : offsetX;
				startZ = mirroredX < 0 ? offsetZ : offsetZ + 1;
				endX = startX + mirroredZ;
				endZ = startZ - mirroredX;
				break;
			default:
				startX = mirroredX < 0 ? offsetX + 1 : offsetX;
				startZ = mirroredZ < 0 ? offsetZ + 1 : offsetZ;
				endX = startX + mirroredX;
				endZ = startZ + mirroredZ;
		}

		return StructureBoxRendering.StructureBox.create(startX, offsetY, startZ, endX, topY, endZ);
	}

	public enum Action {
		UPDATE_DATA,
		SAVE_AREA,
		LOAD_AREA,
		SCAN_AREA;
	}
}
