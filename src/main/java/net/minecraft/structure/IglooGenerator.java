package net.minecraft.structure;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.Map;

/**
 * Генератор структуры иглу. Размещает три шаблонных части: верхнюю (снежный купол),
 * среднюю (лестничный пролёт) и нижнюю (подземная лаборатория). Нижняя часть и
 * промежуточные секции генерируются только с вероятностью 50%.
 */
public class IglooGenerator {

	public static final int OFFSET_Y = 90;
	static final Identifier TOP_TEMPLATE = Identifier.ofVanilla("igloo/top");
	private static final Identifier MIDDLE_TEMPLATE = Identifier.ofVanilla("igloo/middle");
	private static final Identifier BOTTOM_TEMPLATE = Identifier.ofVanilla("igloo/bottom");
	static final Map<Identifier, BlockPos> OFFSETS = ImmutableMap.of(
			TOP_TEMPLATE,
			new BlockPos(3, 5, 5),
			MIDDLE_TEMPLATE,
			new BlockPos(1, 3, 1),
			BOTTOM_TEMPLATE,
			new BlockPos(3, 6, 7)
	);
	static final Map<Identifier, BlockPos> OFFSETS_FROM_TOP = ImmutableMap.of(
			TOP_TEMPLATE,
			BlockPos.ORIGIN,
			MIDDLE_TEMPLATE,
			new BlockPos(2, -3, 4),
			BOTTOM_TEMPLATE,
			new BlockPos(0, -3, -2)
	);

	public static void addPieces(
			StructureTemplateManager manager,
			BlockPos pos,
			BlockRotation rotation,
			StructurePiecesHolder holder,
			Random random
	) {
		if (random.nextDouble() < 0.5) {
			int depth = random.nextInt(8) + 4;
			holder.addPiece(new IglooGenerator.Piece(manager, BOTTOM_TEMPLATE, pos, rotation, depth * 3));

			for (int floorIndex = 0; floorIndex < depth - 1; floorIndex++) {
				holder.addPiece(new IglooGenerator.Piece(manager, MIDDLE_TEMPLATE, pos, rotation, floorIndex * 3));
			}
		}

		holder.addPiece(new IglooGenerator.Piece(manager, TOP_TEMPLATE, pos, rotation, 0));
	}

	/**
	 * Один структурный фрагмент иглу, загружаемый из NBT-шаблона.
	 * Корректирует Y-позицию по высоте поверхности мира при генерации.
	 */
	public static class Piece extends SimpleStructurePiece {

		public Piece(
				StructureTemplateManager manager,
				Identifier identifier,
				BlockPos pos,
				BlockRotation rotation,
				int yOffset
		) {
			super(
					StructurePieceType.IGLOO,
					0,
					manager,
					identifier,
					identifier.toString(),
					createPlacementData(rotation, identifier),
					getPosOffset(identifier, pos, yOffset)
			);
		}

		public Piece(StructureTemplateManager manager, NbtCompound nbt) {
			super(
					StructurePieceType.IGLOO,
					nbt,
					manager,
					identifier -> createPlacementData(
							nbt
									.<BlockRotation>get("Rot", BlockRotation.ENUM_NAME_CODEC)
									.orElseThrow(), identifier
					)
			);
		}

		private static StructurePlacementData createPlacementData(BlockRotation rotation, Identifier identifier) {
			return new StructurePlacementData()
					.setRotation(rotation)
					.setMirror(BlockMirror.NONE)
					.setPosition(IglooGenerator.OFFSETS.get(identifier))
					.addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS)
					.setLiquidSettings(StructureLiquidSettings.IGNORE_WATERLOGGING);
		}

		private static BlockPos getPosOffset(Identifier identifier, BlockPos pos, int yOffset) {
			return pos.add(IglooGenerator.OFFSETS_FROM_TOP.get(identifier)).down(yOffset);
		}

		@Override
		protected void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			nbt.put("Rot", BlockRotation.ENUM_NAME_CODEC, this.placementData.getRotation());
		}

		@Override
		protected void handleMetadata(
				String metadata,
				BlockPos pos,
				ServerWorldAccess world,
				Random random,
				BlockBox boundingBox
		) {
			if ("chest".equals(metadata)) {
				world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
				BlockEntity blockEntity = world.getBlockEntity(pos.down());
				if (blockEntity instanceof ChestBlockEntity) {
					((ChestBlockEntity) blockEntity).setLootTable(LootTables.IGLOO_CHEST_CHEST, random.nextLong());
				}
			}
		}

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
			Identifier templateId = Identifier.of(this.templateIdString);
			StructurePlacementData placementData = createPlacementData(this.placementData.getRotation(), templateId);
			BlockPos offsetFromTop = IglooGenerator.OFFSETS_FROM_TOP.get(templateId);
			BlockPos surfaceCheckPos = this.pos.add(
					StructureTemplate.transform(placementData, new BlockPos(3 - offsetFromTop.getX(), 0, -offsetFromTop.getZ()))
			);
			int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, surfaceCheckPos.getX(), surfaceCheckPos.getZ());
			BlockPos originalPos = this.pos;
			this.pos = this.pos.add(0, surfaceY - OFFSET_Y - 1, 0);
			super.generate(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot);

			if (templateId.equals(IglooGenerator.TOP_TEMPLATE)) {
				BlockPos snowCheckPos = this.pos.add(StructureTemplate.transform(placementData, new BlockPos(3, 0, 5)));
				BlockState belowState = world.getBlockState(snowCheckPos.down());
				if (!belowState.isAir() && !belowState.isOf(Blocks.LADDER)) {
					world.setBlockState(snowCheckPos, Blocks.SNOW_BLOCK.getDefaultState(), 3);
				}
			}

			this.pos = originalPos;
		}
	}
}
