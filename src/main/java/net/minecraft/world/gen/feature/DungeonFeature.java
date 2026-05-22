package net.minecraft.world.gen.feature;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.slf4j.Logger;

import java.util.function.Predicate;

/** Генерирует подземелье: прямоугольную комнату из булыжника с мшистым полом, спаунером и сундуками с лутом. */
public class DungeonFeature extends Feature<DefaultFeatureConfig> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MIN_ENTRANCES = 1;
	private static final int MAX_ENTRANCES = 5;
	private static final EntityType<?>[] MOB_SPAWNER_ENTITIES = new EntityType[]{
			EntityType.SKELETON,
			EntityType.ZOMBIE,
			EntityType.ZOMBIE,
			EntityType.SPIDER
	};
	private static final BlockState CAVE_AIR = Blocks.CAVE_AIR.getDefaultState();

	public DungeonFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		Predicate<BlockState> canReplace = Feature.notInBlockTagPredicate(BlockTags.FEATURES_CANNOT_REPLACE);
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();
		StructureWorldAccess world = context.getWorld();
		int halfX = random.nextInt(2) + 2;
		int minX = -halfX - 1;
		int maxX = halfX + 1;
		int halfZ = random.nextInt(2) + 2;
		int minZ = -halfZ - 1;
		int maxZ = halfZ + 1;
		int entranceCount = 0;

		for (int dx = minX; dx <= maxX; dx++) {
			for (int dy = -1; dy <= 4; dy++) {
				for (int dz = minZ; dz <= maxZ; dz++) {
					BlockPos pos = origin.add(dx, dy, dz);
					boolean isSolid = world.getBlockState(pos).isSolid();

					if (dy == -1 && !isSolid) {
						return false;
					}

					if (dy == 4 && !isSolid) {
						return false;
					}

					if ((dx == minX || dx == maxX || dz == minZ || dz == maxZ)
							&& dy == 0
							&& world.isAir(pos)
							&& world.isAir(pos.up())) {
						entranceCount++;
					}
				}
			}
		}

		if (entranceCount < MIN_ENTRANCES || entranceCount > MAX_ENTRANCES) {
			return false;
		}

		for (int dx = minX; dx <= maxX; dx++) {
			for (int dy = 3; dy >= -1; dy--) {
				for (int dz = minZ; dz <= maxZ; dz++) {
					BlockPos pos = origin.add(dx, dy, dz);
					BlockState state = world.getBlockState(pos);
					boolean isWall = dx == minX || dy == -1 || dz == minZ || dx == maxX || dy == 4 || dz == maxZ;

					if (isWall) {
						if (pos.getY() >= world.getBottomY() && !world.getBlockState(pos.down()).isSolid()) {
							world.setBlockState(pos, CAVE_AIR, 2);
						} else if (state.isSolid() && !state.isOf(Blocks.CHEST)) {
							BlockState wallBlock = (dy == -1 && random.nextInt(4) != 0)
									? Blocks.MOSSY_COBBLESTONE.getDefaultState()
									: Blocks.COBBLESTONE.getDefaultState();
							setBlockStateIf(world, pos, wallBlock, canReplace);
						}
					} else if (!state.isOf(Blocks.CHEST) && !state.isOf(Blocks.SPAWNER)) {
						setBlockStateIf(world, pos, CAVE_AIR, canReplace);
					}
				}
			}
		}

		for (int attempt = 0; attempt < 2; attempt++) {
			for (int retry = 0; retry < 3; retry++) {
				BlockPos chestPos = new BlockPos(
						origin.getX() + random.nextInt(halfX * 2 + 1) - halfX,
						origin.getY(),
						origin.getZ() + random.nextInt(halfZ * 2 + 1) - halfZ
				);

				if (!world.isAir(chestPos)) {
					continue;
				}

				int solidNeighbors = 0;

				for (Direction direction : Direction.Type.HORIZONTAL) {
					if (world.getBlockState(chestPos.offset(direction)).isSolid()) {
						solidNeighbors++;
					}
				}

				if (solidNeighbors == 1) {
					setBlockStateIf(
							world,
							chestPos,
							StructurePiece.orientateChest(world, chestPos, Blocks.CHEST.getDefaultState()),
							canReplace
					);
					LootableInventory.setLootTable(world, random, chestPos, LootTables.SIMPLE_DUNGEON_CHEST);
					break;
				}
			}
		}

		setBlockStateIf(world, origin, Blocks.SPAWNER.getDefaultState(), canReplace);

		if (world.getBlockEntity(origin) instanceof MobSpawnerBlockEntity spawner) {
			spawner.setEntityType(getMobSpawnerEntity(random), random);
		} else {
			LOGGER.error(
					"Failed to fetch mob spawner entity at ({}, {}, {})",
					origin.getX(), origin.getY(), origin.getZ()
			);
		}

		return true;
	}

	private EntityType<?> getMobSpawnerEntity(Random random) {
		return Util.getRandom(MOB_SPAWNER_ENTITIES, random);
	}
}
