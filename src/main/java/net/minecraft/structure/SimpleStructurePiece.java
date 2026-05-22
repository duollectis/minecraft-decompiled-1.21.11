package net.minecraft.structure;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.slf4j.Logger;

import java.util.function.Function;

/**
 * Базовый класс для структурных фрагментов, основанных на {@link StructureTemplate} (файлах .nbt).
 * Загружает шаблон по идентификатору, размещает его в мире и обрабатывает
 * специальные блоки: {@code structure_block} в режиме DATA и {@code jigsaw}.
 */
public abstract class SimpleStructurePiece extends StructurePiece {

	private static final Logger LOGGER = LogUtils.getLogger();

	protected final String templateIdString;
	protected StructureTemplate template;
	protected StructurePlacementData placementData;
	protected BlockPos pos;

	public SimpleStructurePiece(
		StructurePieceType type,
		int length,
		StructureTemplateManager structureTemplateManager,
		Identifier id,
		String templateId,
		StructurePlacementData placementData,
		BlockPos pos
	) {
		super(type, length, structureTemplateManager.getTemplateOrBlank(id).calculateBoundingBox(placementData, pos));
		setOrientation(Direction.NORTH);
		templateIdString = templateId;
		this.pos = pos;
		template = structureTemplateManager.getTemplateOrBlank(id);
		this.placementData = placementData;
	}

	public SimpleStructurePiece(
		StructurePieceType type,
		NbtCompound nbt,
		StructureTemplateManager structureTemplateManager,
		Function<Identifier, StructurePlacementData> placementDataGetter
	) {
		super(type, nbt);
		setOrientation(Direction.NORTH);
		templateIdString = nbt.getString("Template", "");
		pos = new BlockPos(nbt.getInt("TPX", 0), nbt.getInt("TPY", 0), nbt.getInt("TPZ", 0));
		Identifier id = getId();
		template = structureTemplateManager.getTemplateOrBlank(id);
		placementData = placementDataGetter.apply(id);
		boundingBox = template.calculateBoundingBox(placementData, pos);
	}

	protected Identifier getId() {
		return Identifier.of(templateIdString);
	}

	@Override
	protected void writeNbt(StructureContext context, NbtCompound nbt) {
		nbt.putInt("TPX", pos.getX());
		nbt.putInt("TPY", pos.getY());
		nbt.putInt("TPZ", pos.getZ());
		nbt.putString("Template", templateIdString);
	}

	/**
	 * Размещает шаблон структуры в мире, затем обрабатывает специальные блоки:
	 * <ul>
	 *   <li>{@code structure_block} в режиме DATA — вызывает {@link #handleMetadata}</li>
	 *   <li>{@code jigsaw} — заменяет блок на финальное состояние из поля {@code final_state}</li>
	 * </ul>
	 */
	@Override
	public void generate(
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		Random random,
		BlockBox chunkBox,
		ChunkPos chunkPos,
		BlockPos pivot
	) {
		placementData.setBoundingBox(chunkBox);
		boundingBox = template.calculateBoundingBox(placementData, pos);

		if (!template.place(world, pos, pivot, placementData, random, 2)) {
			return;
		}

		for (StructureTemplate.StructureBlockInfo blockInfo : template.getInfosForBlock(pos, placementData, Blocks.STRUCTURE_BLOCK)) {
			if (blockInfo.nbt() == null) {
				continue;
			}

			StructureBlockMode mode = blockInfo.nbt()
				.<StructureBlockMode>get("mode", StructureBlockMode.CODEC)
				.orElseThrow();

			if (mode == StructureBlockMode.DATA) {
				handleMetadata(
					blockInfo.nbt().getString("metadata", ""),
					blockInfo.pos(),
					world,
					random,
					chunkBox
				);
			}
		}

		for (StructureTemplate.StructureBlockInfo jigsawInfo : template.getInfosForBlock(pos, placementData, Blocks.JIGSAW)) {
			if (jigsawInfo.nbt() == null) {
				continue;
			}

			String finalState = jigsawInfo.nbt().getString("final_state", "minecraft:air");
			BlockState blockState = Blocks.AIR.getDefaultState();

			try {
				blockState = BlockArgumentParser
					.block(world.createCommandRegistryWrapper(RegistryKeys.BLOCK), finalState, true)
					.blockState();
			} catch (CommandSyntaxException e) {
				LOGGER.error("Error while parsing blockstate {} in jigsaw block @ {}", finalState, jigsawInfo.pos());
			}

			world.setBlockState(jigsawInfo.pos(), blockState, 3);
		}
	}

	/**
	 * Обрабатывает метаданные из блока {@code structure_block} в режиме DATA.
	 * Вызывается для каждого такого блока после размещения шаблона.
	 *
	 * @param metadata строка метаданных из поля {@code metadata} блока
	 * @param pos позиция блока в мире
	 * @param world серверный мир
	 * @param random генератор случайных чисел
	 * @param boundingBox ограничивающий прямоугольник чанка
	 */
	protected abstract void handleMetadata(
		String metadata,
		BlockPos pos,
		ServerWorldAccess world,
		Random random,
		BlockBox boundingBox
	);

	@Deprecated
	@Override
	public void translate(int x, int y, int z) {
		super.translate(x, y, z);
		pos = pos.add(x, y, z);
	}

	@Override
	public BlockRotation getRotation() {
		return placementData.getRotation();
	}

	public StructureTemplate getTemplate() {
		return template;
	}

	public BlockPos getPos() {
		return pos;
	}

	public StructurePlacementData getPlacementData() {
		return placementData;
	}
}
