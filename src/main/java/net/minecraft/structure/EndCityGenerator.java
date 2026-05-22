package net.minecraft.structure;

import com.google.common.collect.Lists;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import java.util.List;

/**
 * Генератор структур города Края. Использует рекурсивную систему «частей» (Part),
 * каждая из которых добавляет шаблонные блоки и порождает дочерние части до достижения
 * максимальной глубины рекурсии {@link #MAX_DEPTH}.
 */
public class EndCityGenerator {

	private static final int MAX_DEPTH = 8;

	static final EndCityGenerator.Part BUILDING = new EndCityGenerator.Part() {
		@Override
		public void init() {
		}

		@Override
		public boolean create(
				StructureTemplateManager manager,
				int depth,
				EndCityGenerator.Piece root,
				BlockPos pos,
				List<StructurePiece> pieces,
				Random random
		) {
			if (depth > MAX_DEPTH) {
				return false;
			}

			BlockRotation blockRotation = root.getPlacementData().getRotation();
			EndCityGenerator.Piece piece = EndCityGenerator.addPiece(
					pieces, EndCityGenerator.createPiece(manager, root, pos, "base_floor", blockRotation, true)
			);
			int variant = random.nextInt(3);

			if (variant == 0) {
				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(-1, 4, -1), "base_roof", blockRotation, true)
				);
			} else if (variant == 1) {
				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(-1, 0, -1), "second_floor_2", blockRotation, false)
				);
				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(-1, 8, -1), "second_roof", blockRotation, false)
				);
				EndCityGenerator.createPart(manager, EndCityGenerator.SMALL_TOWER, depth + 1, piece, null, pieces, random);
			} else if (variant == 2) {
				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(-1, 0, -1), "second_floor_2", blockRotation, false)
				);
				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(-1, 4, -1), "third_floor_2", blockRotation, false)
				);
				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(-1, 8, -1), "third_roof", blockRotation, true)
				);
				EndCityGenerator.createPart(manager, EndCityGenerator.SMALL_TOWER, depth + 1, piece, null, pieces, random);
			}

			return true;
		}
	};

	static final List<Pair<BlockRotation, BlockPos>> SMALL_TOWER_BRIDGE_ATTACHMENTS = Lists.newArrayList(
			new Pair[]{
					new Pair<>(BlockRotation.NONE, new BlockPos(1, -1, 0)),
					new Pair<>(BlockRotation.CLOCKWISE_90, new BlockPos(6, -1, 1)),
					new Pair<>(BlockRotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 5)),
					new Pair<>(BlockRotation.CLOCKWISE_180, new BlockPos(5, -1, 6))
			}
	);

	static final EndCityGenerator.Part SMALL_TOWER = new EndCityGenerator.Part() {
		@Override
		public void init() {
		}

		@Override
		public boolean create(
				StructureTemplateManager manager,
				int depth,
				EndCityGenerator.Piece root,
				BlockPos pos,
				List<StructurePiece> pieces,
				Random random
		) {
			BlockRotation blockRotation = root.getPlacementData().getRotation();
			EndCityGenerator.Piece piece = EndCityGenerator.addPiece(
					pieces,
					EndCityGenerator.createPiece(
							manager,
							root,
							new BlockPos(3 + random.nextInt(2), -3, 3 + random.nextInt(2)),
							"tower_base",
							blockRotation,
							true
					)
			);
			piece = EndCityGenerator.addPiece(
					pieces,
					EndCityGenerator.createPiece(manager, piece, new BlockPos(0, 7, 0), "tower_piece", blockRotation, true)
			);
			EndCityGenerator.Piece bridgeAttachPiece = random.nextInt(3) == 0 ? piece : null;
			int towerHeight = 1 + random.nextInt(3);

			for (int floorIndex = 0; floorIndex < towerHeight; floorIndex++) {
				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(0, 4, 0), "tower_piece", blockRotation, true)
				);
				if (floorIndex < towerHeight - 1 && random.nextBoolean()) {
					bridgeAttachPiece = piece;
				}
			}

			if (bridgeAttachPiece != null) {
				for (Pair<BlockRotation, BlockPos> pair : EndCityGenerator.SMALL_TOWER_BRIDGE_ATTACHMENTS) {
					if (random.nextBoolean()) {
						EndCityGenerator.Piece bridgeEnd = EndCityGenerator.addPiece(
								pieces,
								EndCityGenerator.createPiece(
										manager,
										bridgeAttachPiece,
										pair.getRight(),
										"bridge_end",
										blockRotation.rotate(pair.getLeft()),
										true
								)
						);
						EndCityGenerator.createPart(manager, EndCityGenerator.BRIDGE_PIECE, depth + 1, bridgeEnd, null, pieces, random);
					}
				}

				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(-1, 4, -1), "tower_top", blockRotation, true)
				);
			} else {
				if (depth != MAX_DEPTH - 1) {
					return EndCityGenerator.createPart(manager, EndCityGenerator.FAT_TOWER, depth + 1, piece, null, pieces, random);
				}

				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(-1, 4, -1), "tower_top", blockRotation, true)
				);
			}

			return true;
		}
	};

	static final EndCityGenerator.Part BRIDGE_PIECE = new EndCityGenerator.Part() {
		public boolean shipGenerated;

		@Override
		public void init() {
			shipGenerated = false;
		}

		@Override
		public boolean create(
				StructureTemplateManager manager,
				int depth,
				EndCityGenerator.Piece root,
				BlockPos pos,
				List<StructurePiece> pieces,
				Random random
		) {
			BlockRotation blockRotation = root.getPlacementData().getRotation();
			int bridgeLength = random.nextInt(4) + 1;
			EndCityGenerator.Piece piece = EndCityGenerator.addPiece(
					pieces,
					EndCityGenerator.createPiece(manager, root, new BlockPos(0, 0, -4), "bridge_piece", blockRotation, true)
			);
			piece.setChainLength(-1);
			int heightOffset = 0;

			for (int segmentIndex = 0; segmentIndex < bridgeLength; segmentIndex++) {
				if (random.nextBoolean()) {
					piece = EndCityGenerator.addPiece(
							pieces,
							EndCityGenerator.createPiece(
									manager, piece, new BlockPos(0, heightOffset, -4), "bridge_piece", blockRotation, true
							)
					);
					heightOffset = 0;
				} else {
					if (random.nextBoolean()) {
						piece = EndCityGenerator.addPiece(
								pieces,
								EndCityGenerator.createPiece(
										manager, piece, new BlockPos(0, heightOffset, -4), "bridge_steep_stairs", blockRotation, true
								)
						);
					} else {
						piece = EndCityGenerator.addPiece(
								pieces,
								EndCityGenerator.createPiece(
										manager, piece, new BlockPos(0, heightOffset, -8), "bridge_gentle_stairs", blockRotation, true
								)
						);
					}

					heightOffset = 4;
				}
			}

			if (!shipGenerated && random.nextInt(10 - depth) == 0) {
				EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(
								manager,
								piece,
								new BlockPos(-8 + random.nextInt(8), heightOffset, -70 + random.nextInt(10)),
								"ship",
								blockRotation,
								true
						)
				);
				shipGenerated = true;
			} else if (!EndCityGenerator.createPart(
					manager,
					EndCityGenerator.BUILDING,
					depth + 1,
					piece,
					new BlockPos(-3, heightOffset + 1, -11),
					pieces,
					random
			)) {
				return false;
			}

			piece = EndCityGenerator.addPiece(
					pieces,
					EndCityGenerator.createPiece(
							manager, piece, new BlockPos(4, heightOffset, 0), "bridge_end", blockRotation.rotate(BlockRotation.CLOCKWISE_180), true
					)
			);
			piece.setChainLength(-1);
			return true;
		}
	};

	static final List<Pair<BlockRotation, BlockPos>> FAT_TOWER_BRIDGE_ATTACHMENTS = Lists.newArrayList(
			new Pair[]{
					new Pair<>(BlockRotation.NONE, new BlockPos(4, -1, 0)),
					new Pair<>(BlockRotation.CLOCKWISE_90, new BlockPos(12, -1, 4)),
					new Pair<>(BlockRotation.COUNTERCLOCKWISE_90, new BlockPos(0, -1, 8)),
					new Pair<>(BlockRotation.CLOCKWISE_180, new BlockPos(8, -1, 12))
			}
	);

	static final EndCityGenerator.Part FAT_TOWER = new EndCityGenerator.Part() {
		@Override
		public void init() {
		}

		@Override
		public boolean create(
				StructureTemplateManager manager,
				int depth,
				EndCityGenerator.Piece root,
				BlockPos pos,
				List<StructurePiece> pieces,
				Random random
		) {
			BlockRotation blockRotation = root.getPlacementData().getRotation();
			EndCityGenerator.Piece piece = EndCityGenerator.addPiece(
					pieces,
					EndCityGenerator.createPiece(manager, root, new BlockPos(-3, 4, -3), "fat_tower_base", blockRotation, true)
			);
			piece = EndCityGenerator.addPiece(
					pieces,
					EndCityGenerator.createPiece(manager, piece, new BlockPos(0, 4, 0), "fat_tower_middle", blockRotation, true)
			);

			for (int floorIndex = 0; floorIndex < 2 && random.nextInt(3) != 0; floorIndex++) {
				piece = EndCityGenerator.addPiece(
						pieces,
						EndCityGenerator.createPiece(manager, piece, new BlockPos(0, 8, 0), "fat_tower_middle", blockRotation, true)
				);

				for (Pair<BlockRotation, BlockPos> pair : EndCityGenerator.FAT_TOWER_BRIDGE_ATTACHMENTS) {
					if (random.nextBoolean()) {
						EndCityGenerator.Piece bridgeEnd = EndCityGenerator.addPiece(
								pieces,
								EndCityGenerator.createPiece(
										manager, piece, pair.getRight(), "bridge_end", blockRotation.rotate(pair.getLeft()), true
								)
						);
						EndCityGenerator.createPart(manager, EndCityGenerator.BRIDGE_PIECE, depth + 1, bridgeEnd, null, pieces, random);
					}
				}
			}

			piece = EndCityGenerator.addPiece(
					pieces,
					EndCityGenerator.createPiece(manager, piece, new BlockPos(-2, 8, -2), "fat_tower_top", blockRotation, true)
			);
			return true;
		}
	};

	/**
	 * Создаёт новый фрагмент структуры, позиционируя его относительно предыдущего
	 * через трансформацию bounding box шаблона.
	 */
	static EndCityGenerator.Piece createPiece(
			StructureTemplateManager structureTemplateManager,
			EndCityGenerator.Piece lastPiece,
			BlockPos relativePosition,
			String template,
			BlockRotation rotation,
			boolean ignoreAir
	) {
		EndCityGenerator.Piece piece = new EndCityGenerator.Piece(
				structureTemplateManager, template, lastPiece.getPos(), rotation, ignoreAir
		);
		BlockPos offset = lastPiece
				.getTemplate()
				.transformBox(lastPiece.getPlacementData(), relativePosition, piece.getPlacementData(), BlockPos.ORIGIN);
		piece.translate(offset.getX(), offset.getY(), offset.getZ());
		return piece;
	}

	/**
	 * Точка входа генерации: инициализирует все части и строит базовую структуру города Края.
	 */
	public static void addPieces(
			StructureTemplateManager structureTemplateManager,
			BlockPos pos,
			BlockRotation rotation,
			List<StructurePiece> pieces,
			Random random
	) {
		FAT_TOWER.init();
		BUILDING.init();
		BRIDGE_PIECE.init();
		SMALL_TOWER.init();
		EndCityGenerator.Piece piece = addPiece(
				pieces, new EndCityGenerator.Piece(structureTemplateManager, "base_floor", pos, rotation, true)
		);
		piece = addPiece(pieces, createPiece(structureTemplateManager, piece, new BlockPos(-1, 0, -1), "second_floor_1", rotation, false));
		piece = addPiece(pieces, createPiece(structureTemplateManager, piece, new BlockPos(-1, 4, -1), "third_floor_1", rotation, false));
		piece = addPiece(pieces, createPiece(structureTemplateManager, piece, new BlockPos(-1, 8, -1), "third_roof", rotation, true));
		createPart(structureTemplateManager, SMALL_TOWER, 1, piece, null, pieces, random);
	}

	static EndCityGenerator.Piece addPiece(List<StructurePiece> pieces, EndCityGenerator.Piece piece) {
		pieces.add(piece);
		return piece;
	}

	/**
	 * Рекурсивно создаёт часть структуры, проверяя пересечения с уже размещёнными фрагментами.
	 * Если новые фрагменты пересекаются с чужой цепочкой — откатывает всю попытку.
	 */
	static boolean createPart(
			StructureTemplateManager manager,
			EndCityGenerator.Part part,
			int depth,
			EndCityGenerator.Piece parent,
			BlockPos pos,
			List<StructurePiece> pieces,
			Random random
	) {
		if (depth > MAX_DEPTH) {
			return false;
		}

		List<StructurePiece> candidates = Lists.newArrayList();
		if (!part.create(manager, depth, parent, pos, candidates, random)) {
			return false;
		}

		int chainId = random.nextInt();
		for (StructurePiece candidate : candidates) {
			candidate.setChainLength(chainId);
			StructurePiece intersecting = StructurePiece.firstIntersecting(pieces, candidate.getBoundingBox());
			if (intersecting != null && intersecting.getChainLength() != parent.getChainLength()) {
				return false;
			}
		}

		pieces.addAll(candidates);
		return true;
	}

	/**
	 * Стратегия генерации одной части города Края. Реализации определяют конкретный тип
	 * структурного блока (башня, мост, здание).
	 */
	interface Part {

		void init();

		boolean create(
				StructureTemplateManager manager,
				int depth,
				EndCityGenerator.Piece root,
				BlockPos pos,
				List<StructurePiece> pieces,
				Random random
		);
	}

	/**
	 * Один структурный фрагмент города Края, загружаемый из NBT-шаблона.
	 * Обрабатывает метаданные для спавна сундуков, шалкеров и элитр.
	 */
	public static class Piece extends SimpleStructurePiece {

		public Piece(
				StructureTemplateManager manager,
				String template,
				BlockPos pos,
				BlockRotation rotation,
				boolean includeAir
		) {
			super(
					StructurePieceType.END_CITY,
					0,
					manager,
					getId(template),
					template,
					createPlacementData(includeAir, rotation),
					pos
			);
		}

		public Piece(StructureTemplateManager manager, NbtCompound nbt) {
			super(
					StructurePieceType.END_CITY,
					nbt,
					manager,
					id -> createPlacementData(
							nbt.getBoolean("OW", false),
							nbt.<BlockRotation>get("Rot", BlockRotation.ENUM_NAME_CODEC).orElseThrow()
					)
			);
		}

		private static StructurePlacementData createPlacementData(boolean includeAir, BlockRotation rotation) {
			BlockIgnoreStructureProcessor processor = includeAir
					? BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS
					: BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS;
			return new StructurePlacementData()
					.setIgnoreEntities(true)
					.addProcessor(processor)
					.setRotation(rotation);
		}

		@Override
		protected Identifier getId() {
			return getId(this.templateIdString);
		}

		private static Identifier getId(String template) {
			return Identifier.ofVanilla("end_city/" + template);
		}

		@Override
		protected void writeNbt(StructureContext context, NbtCompound nbt) {
			super.writeNbt(context, nbt);
			nbt.put("Rot", BlockRotation.ENUM_NAME_CODEC, this.placementData.getRotation());
			nbt.putBoolean(
					"OW",
					this.placementData.getProcessors().get(0) == BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS
			);
		}

		@Override
		protected void handleMetadata(
				String metadata,
				BlockPos pos,
				ServerWorldAccess world,
				Random random,
				BlockBox boundingBox
		) {
			if (metadata.startsWith("Chest")) {
				BlockPos chestPos = pos.down();
				if (boundingBox.contains(chestPos)) {
					LootableInventory.setLootTable(world, random, chestPos, LootTables.END_CITY_TREASURE_CHEST);
				}
				return;
			}

			if (!boundingBox.contains(pos) || !World.isValid(pos)) {
				return;
			}

			if (metadata.startsWith("Sentry")) {
				ShulkerEntity shulker = EntityType.SHULKER.create(world.toServerWorld(), SpawnReason.STRUCTURE);
				if (shulker != null) {
					shulker.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
					world.spawnEntity(shulker);
				}
			} else if (metadata.startsWith("Elytra")) {
				ItemFrameEntity itemFrame = new ItemFrameEntity(
						world.toServerWorld(),
						pos,
						this.placementData.getRotation().rotate(Direction.SOUTH)
				);
				itemFrame.setHeldItemStack(new ItemStack(Items.ELYTRA), false);
				world.spawnEntity(itemFrame);
			}
		}
	}
}
