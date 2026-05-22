package net.minecraft.world.chunk.light;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Провайдер небесного освещения.
 * <p>
 * Реализует алгоритм BFS-распространения небесного света (солнечного/лунного).
 * Небесный свет имеет уровень 15 во всех колонках выше поверхности ({@link ChunkSkyLight}).
 * <p>
 * Ключевые особенности по сравнению с блочным светом:
 * <ul>
 *   <li>При загрузке чанка инициализирует все секции выше поверхности уровнем 15</li>
 *   <li>При изменении блока проверяет высоту поверхности и распространяет свет вниз</li>
 *   <li>Учитывает «пустые» секции ниже текущей при распространении через границы чанков</li>
 * </ul>
 */
public final class ChunkSkyLightProvider extends ChunkLightProvider<SkyLightStorage.Data, SkyLightStorage> {

	private static final long SKY_LIGHT_PACKED_ALL_DIRS = ChunkLightProvider.PackedInfo.packWithAllDirectionsSet(15);
	private static final long SKY_LIGHT_PACKED_NO_UP = ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(15, Direction.UP);
	private static final long SKY_LIGHT_PACKED_PROPAGATION = ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(15, false, Direction.UP);

	private final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
	private final ChunkSkyLight defaultSkyLight;

	public ChunkSkyLightProvider(ChunkProvider chunkProvider) {
		this(chunkProvider, new SkyLightStorage(chunkProvider));
	}

	@VisibleForTesting
	protected ChunkSkyLightProvider(ChunkProvider chunkProvider, SkyLightStorage lightStorage) {
		super(chunkProvider, lightStorage);
		defaultSkyLight = new ChunkSkyLight(chunkProvider.getWorld());
	}

	private static boolean isMaxLightLevel(int lightLevel) {
		return lightLevel == MAX_LIGHT_LEVEL;
	}

	private int getSkyLightOrDefault(int x, int z, int defaultValue) {
		ChunkSkyLight skyLight = getSkyLight(
			ChunkSectionPos.getSectionCoord(x),
			ChunkSectionPos.getSectionCoord(z)
		);

		return skyLight == null
			? defaultValue
			: skyLight.get(ChunkSectionPos.getLocalCoord(x), ChunkSectionPos.getLocalCoord(z));
	}

	private @Nullable ChunkSkyLight getSkyLight(int chunkX, int chunkZ) {
		LightSourceView chunk = chunkProvider.getChunk(chunkX, chunkZ);

		return chunk != null ? chunk.getChunkSkyLight() : null;
	}

	@Override
	protected void checkForLightUpdate(long blockPos) {
		int x = BlockPos.unpackLongX(blockPos);
		int y = BlockPos.unpackLongY(blockPos);
		int z = BlockPos.unpackLongZ(blockPos);
		long sectionPos = ChunkSectionPos.fromBlockPos(blockPos);

		int surfaceY = lightStorage.isSectionInEnabledColumn(sectionPos)
			? getSkyLightOrDefault(x, z, Integer.MAX_VALUE)
			: Integer.MAX_VALUE;

		if (surfaceY != Integer.MAX_VALUE) {
			checkColumnForSkyLight(x, z, surfaceY);
		}

		if (!lightStorage.hasSection(sectionPos)) {
			return;
		}

		if (y >= surfaceY) {
			queueLightDecrease(blockPos, SKY_LIGHT_PACKED_NO_UP);
			queueLightIncrease(blockPos, SKY_LIGHT_PACKED_PROPAGATION);
		} else {
			int storedLevel = lightStorage.get(blockPos);

			if (storedLevel > 0) {
				lightStorage.set(blockPos, 0);
				queueLightDecrease(blockPos, ChunkLightProvider.PackedInfo.packWithAllDirectionsSet(storedLevel));
			} else {
				queueLightDecrease(blockPos, INITIAL_PACKED_INFO);
			}
		}
	}

	private void checkColumnForSkyLight(int x, int z, int surfaceY) {
		int minBlockY = ChunkSectionPos.getBlockCoord(lightStorage.getMinSectionY());
		propagateSkyLightDecrease(x, z, surfaceY, minBlockY);
		propagateSkyLightIncrease(x, z, surfaceY, minBlockY);
	}

	/**
	 * Гасит небесный свет в колонке от {@code surfaceY} вниз до {@code minBlockY}.
	 * Останавливается, если встречает блок с уровнем света меньше 15 (уже погашен).
	 */
	private void propagateSkyLightDecrease(int x, int z, int surfaceY, int minBlockY) {
		if (surfaceY <= minBlockY) {
			return;
		}

		int chunkX = ChunkSectionPos.getSectionCoord(x);
		int chunkZ = ChunkSectionPos.getSectionCoord(z);
		int startY = surfaceY - 1;

		for (int sectionY = ChunkSectionPos.getSectionCoord(startY);
			 lightStorage.isAboveMinHeight(sectionY);
			 sectionY--
		) {
			if (!lightStorage.hasSection(ChunkSectionPos.asLong(chunkX, sectionY, chunkZ))) {
				continue;
			}

			int sectionMinY = ChunkSectionPos.getBlockCoord(sectionY);
			int sectionMaxY = sectionMinY + 15;

			for (int blockY = Math.min(sectionMaxY, startY); blockY >= sectionMinY; blockY--) {
				long pos = BlockPos.asLong(x, blockY, z);

				if (!isMaxLightLevel(lightStorage.get(pos))) {
					return;
				}

				lightStorage.set(pos, 0);
				queueLightDecrease(pos, blockY == surfaceY - 1 ? SKY_LIGHT_PACKED_ALL_DIRS : SKY_LIGHT_PACKED_NO_UP);
			}
		}
	}

	/**
	 * Восстанавливает небесный свет в колонке от {@code surfaceY} вниз.
	 * Также проверяет соседние колонки: если их поверхность ниже, распространяет свет горизонтально.
	 */
	private void propagateSkyLightIncrease(int x, int z, int surfaceY, int minBlockY) {
		int chunkX = ChunkSectionPos.getSectionCoord(x);
		int chunkZ = ChunkSectionPos.getSectionCoord(z);

		int maxNeighborSurface = Math.max(
			Math.max(
				getSkyLightOrDefault(x - 1, z, Integer.MIN_VALUE),
				getSkyLightOrDefault(x + 1, z, Integer.MIN_VALUE)
			),
			Math.max(
				getSkyLightOrDefault(x, z - 1, Integer.MIN_VALUE),
				getSkyLightOrDefault(x, z + 1, Integer.MIN_VALUE)
			)
		);

		int startY = Math.max(surfaceY, minBlockY);
		long startSection = ChunkSectionPos.asLong(chunkX, ChunkSectionPos.getSectionCoord(startY), chunkZ);

		for (long sectionPos = startSection;
			 !lightStorage.isAtOrAboveTopmostSection(sectionPos);
			 sectionPos = ChunkSectionPos.offset(sectionPos, Direction.UP)
		) {
			if (!lightStorage.hasSection(sectionPos)) {
				continue;
			}

			int sectionMinY = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackY(sectionPos));
			int sectionMaxY = sectionMinY + 15;

			for (int blockY = Math.max(sectionMinY, startY); blockY <= sectionMaxY; blockY++) {
				long pos = BlockPos.asLong(x, blockY, z);

				if (isMaxLightLevel(lightStorage.get(pos))) {
					return;
				}

				lightStorage.set(pos, MAX_LIGHT_LEVEL);

				if (blockY < maxNeighborSurface || blockY == surfaceY) {
					queueLightIncrease(pos, SKY_LIGHT_PACKED_PROPAGATION);
				}
			}
		}
	}

	@Override
	protected void propagateLightIncrease(long blockPos, long packed, int lightLevel) {
		BlockState sourceState = null;
		int sectionsBelow = getNumberOfSectionsBelowPos(blockPos);

		for (Direction direction : DIRECTIONS) {
			if (!ChunkLightProvider.PackedInfo.isDirectionBitSet(packed, direction)) {
				continue;
			}

			long neighborPos = BlockPos.offset(blockPos, direction);

			if (!lightStorage.hasSection(ChunkSectionPos.fromBlockPos(neighborPos))) {
				continue;
			}

			int neighborLevel = lightStorage.get(neighborPos);
			int minRequired = lightLevel - 1;

			if (minRequired <= neighborLevel) {
				continue;
			}

			mutablePos.set(neighborPos);
			BlockState neighborState = getStateForLighting(mutablePos);
			int propagated = lightLevel - getOpacity(neighborState);

			if (propagated <= neighborLevel) {
				continue;
			}

			if (sourceState == null) {
				sourceState = ChunkLightProvider.PackedInfo.isTrivial(packed)
					? Blocks.AIR.getDefaultState()
					: getStateForLighting(mutablePos.set(blockPos));
			}

			if (shapesCoverFullCube(sourceState, neighborState, direction)) {
				continue;
			}

			lightStorage.set(neighborPos, propagated);

			if (propagated > 1) {
				queueLightIncrease(
					neighborPos,
					ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(
						propagated,
						isTrivialForLighting(neighborState),
						direction.getOpposite()
					)
				);
			}

			propagateSkyLightToNeighbor(neighborPos, direction, propagated, true, sectionsBelow);
		}
	}

	@Override
	protected void propagateLightDecrease(long blockPos, long packed) {
		int sectionsBelow = getNumberOfSectionsBelowPos(blockPos);
		int packedLevel = ChunkLightProvider.PackedInfo.getLightLevel(packed);

		for (Direction direction : DIRECTIONS) {
			if (!ChunkLightProvider.PackedInfo.isDirectionBitSet(packed, direction)) {
				continue;
			}

			long neighborPos = BlockPos.offset(blockPos, direction);

			if (!lightStorage.hasSection(ChunkSectionPos.fromBlockPos(neighborPos))) {
				continue;
			}

			int neighborLevel = lightStorage.get(neighborPos);

			if (neighborLevel == 0) {
				continue;
			}

			if (neighborLevel <= packedLevel - 1) {
				lightStorage.set(neighborPos, 0);
				queueLightDecrease(
					neighborPos,
					ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(
						neighborLevel,
						direction.getOpposite()
					)
				);
				propagateSkyLightToNeighbor(neighborPos, direction, neighborLevel, false, sectionsBelow);
			} else {
				queueLightIncrease(
					neighborPos,
					ChunkLightProvider.PackedInfo.packWithRepropagate(neighborLevel, false, direction.getOpposite())
				);
			}
		}
	}

	/**
	 * Считает количество незагруженных секций ниже секции, содержащей данный блок.
	 * Используется для распространения небесного света через границы чанков по вертикали.
	 * Возвращает 0, если блок не находится на нижней границе секции или не на краю чанка по XZ.
	 */
	private int getNumberOfSectionsBelowPos(long blockPos) {
		int y = BlockPos.unpackLongY(blockPos);

		if (ChunkSectionPos.getLocalCoord(y) != 0) {
			return 0;
		}

		int x = BlockPos.unpackLongX(blockPos);
		int z = BlockPos.unpackLongZ(blockPos);
		int localX = ChunkSectionPos.getLocalCoord(x);
		int localZ = ChunkSectionPos.getLocalCoord(z);

		if (localX != 0 && localX != 15 && localZ != 0 && localZ != 15) {
			return 0;
		}

		int chunkX = ChunkSectionPos.getSectionCoord(x);
		int sectionY = ChunkSectionPos.getSectionCoord(y);
		int chunkZ = ChunkSectionPos.getSectionCoord(z);
		int gap = 0;

		while (!lightStorage.hasSection(ChunkSectionPos.asLong(chunkX, sectionY - gap - 1, chunkZ))
			&& lightStorage.isAboveMinHeight(sectionY - gap - 1)
		) {
			gap++;
		}

		return gap;
	}

	/**
	 * Распространяет небесный свет в незагруженные секции соседнего чанка по вертикали.
	 * Вызывается при пересечении горизонтальной границы чанка (XZ).
	 *
	 * @param increase {@code true} — увеличение света, {@code false} — уменьшение
	 * @param sectionsBelow количество незагруженных секций ниже текущей
	 */
	private void propagateSkyLightToNeighbor(long blockPos, Direction direction, int lightLevel, boolean increase, int sectionsBelow) {
		if (sectionsBelow == 0) {
			return;
		}

		int x = BlockPos.unpackLongX(blockPos);
		int z = BlockPos.unpackLongZ(blockPos);

		if (!exitsChunkXZ(direction, ChunkSectionPos.getLocalCoord(x), ChunkSectionPos.getLocalCoord(z))) {
			return;
		}

		int y = BlockPos.unpackLongY(blockPos);
		int chunkX = ChunkSectionPos.getSectionCoord(x);
		int chunkZ = ChunkSectionPos.getSectionCoord(z);
		int topSection = ChunkSectionPos.getSectionCoord(y) - 1;
		int bottomSection = topSection - sectionsBelow + 1;

		for (int sectionY = topSection; sectionY >= bottomSection; sectionY--) {
			if (!lightStorage.hasSection(ChunkSectionPos.asLong(chunkX, sectionY, chunkZ))) {
				continue;
			}

			int sectionMinY = ChunkSectionPos.getBlockCoord(sectionY);

			for (int localY = 15; localY >= 0; localY--) {
				long pos = BlockPos.asLong(x, sectionMinY + localY, z);

				if (increase) {
					lightStorage.set(pos, lightLevel);

					if (lightLevel > 1) {
						queueLightIncrease(
							pos,
							ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(
								lightLevel,
								true,
								direction.getOpposite()
							)
						);
					}
				} else {
					lightStorage.set(pos, 0);
					queueLightDecrease(
						pos,
						ChunkLightProvider.PackedInfo.packWithOneDirectionCleared(
							lightLevel,
							direction.getOpposite()
						)
					);
				}
			}
		}
	}

	private static boolean exitsChunkXZ(Direction direction, int localX, int localZ) {
		return switch (direction) {
			case NORTH -> localZ == 15;
			case SOUTH -> localZ == 0;
			case WEST -> localX == 15;
			case EAST -> localX == 0;
			default -> false;
		};
	}

	@Override
	public void setColumnEnabled(ChunkPos pos, boolean retainData) {
		super.setColumnEnabled(pos, retainData);

		if (!retainData) {
			return;
		}

		ChunkSkyLight skyLight = Objects.requireNonNullElse(getSkyLight(pos.x, pos.z), defaultSkyLight);
		int surfaceY = skyLight.getMaxSurfaceY() - 1;
		int topSection = ChunkSectionPos.getSectionCoord(surfaceY) + 1;
		long columnPos = ChunkSectionPos.withZeroY(pos.x, pos.z);
		int columnTop = lightStorage.getTopSectionForColumn(columnPos);
		int startSection = Math.max(lightStorage.getMinSectionY(), topSection);

		for (int sectionY = columnTop - 1; sectionY >= startSection; sectionY--) {
			ChunkNibbleArray section = lightStorage.getOrCreateLightSection(
				ChunkSectionPos.asLong(pos.x, sectionY, pos.z)
			);

			if (section != null && section.isUninitialized()) {
				section.clear(MAX_LIGHT_LEVEL);
			}
		}
	}

	/**
	 * Инициализирует небесное освещение для нового чанка.
	 * <p>
	 * Для каждой секции заполняет nibble-массив уровнями 15 сверху вниз до поверхности,
	 * затем ставит в очередь распространение света на соседние чанки через горизонтальные границы.
	 */
	@Override
	public void propagateLight(ChunkPos chunkPos) {
		long columnPos = ChunkSectionPos.withZeroY(chunkPos.x, chunkPos.z);
		lightStorage.setColumnEnabled(columnPos, true);

		ChunkSkyLight skyLight = Objects.requireNonNullElse(getSkyLight(chunkPos.x, chunkPos.z), defaultSkyLight);
		ChunkSkyLight southSkyLight = Objects.requireNonNullElse(getSkyLight(chunkPos.x, chunkPos.z - 1), defaultSkyLight);
		ChunkSkyLight northSkyLight = Objects.requireNonNullElse(getSkyLight(chunkPos.x, chunkPos.z + 1), defaultSkyLight);
		ChunkSkyLight westSkyLight = Objects.requireNonNullElse(getSkyLight(chunkPos.x - 1, chunkPos.z), defaultSkyLight);
		ChunkSkyLight eastSkyLight = Objects.requireNonNullElse(getSkyLight(chunkPos.x + 1, chunkPos.z), defaultSkyLight);

		int topSection = lightStorage.getTopSectionForColumn(columnPos);
		int minSection = lightStorage.getMinSectionY();
		int chunkBlockX = ChunkSectionPos.getBlockCoord(chunkPos.x);
		int chunkBlockZ = ChunkSectionPos.getBlockCoord(chunkPos.z);

		for (int sectionY = topSection - 1; sectionY >= minSection; sectionY--) {
			long sectionPos = ChunkSectionPos.asLong(chunkPos.x, sectionY, chunkPos.z);
			ChunkNibbleArray section = lightStorage.getOrCreateLightSection(sectionPos);

			if (section == null) {
				continue;
			}

			int sectionMinY = ChunkSectionPos.getBlockCoord(sectionY);
			int sectionMaxY = sectionMinY + 15;
			boolean hasBelowSurface = false;

			for (int localZ = 0; localZ < 16; localZ++) {
				for (int localX = 0; localX < 16; localX++) {
					int surfaceY = skyLight.get(localX, localZ);

					if (surfaceY > sectionMaxY) {
						continue;
					}

					int southY = localZ == 0 ? southSkyLight.get(localX, 15) : skyLight.get(localX, localZ - 1);
					int northY = localZ == 15 ? northSkyLight.get(localX, 0) : skyLight.get(localX, localZ + 1);
					int westY = localX == 0 ? westSkyLight.get(15, localZ) : skyLight.get(localX - 1, localZ);
					int eastY = localX == 15 ? eastSkyLight.get(0, localZ) : skyLight.get(localX + 1, localZ);
					int maxNeighborY = Math.max(Math.max(southY, northY), Math.max(westY, eastY));

					for (int blockY = sectionMaxY; blockY >= Math.max(sectionMinY, surfaceY); blockY--) {
						section.set(localX, ChunkSectionPos.getLocalCoord(blockY), localZ, ChunkSectionPos.LOCAL_COORD_MASK);

						if (blockY == surfaceY || blockY < maxNeighborY) {
							long blockPos = BlockPos.asLong(chunkBlockX + localX, blockY, chunkBlockZ + localZ);
							queueLightIncrease(
								blockPos,
								ChunkLightProvider.PackedInfo.packSkyLightPropagation(
									blockY == surfaceY,
									blockY < southY,
									blockY < northY,
									blockY < westY,
									blockY < eastY
								)
							);
						}
					}

					if (surfaceY < sectionMinY) {
						hasBelowSurface = true;
					}
				}
			}

			if (!hasBelowSurface) {
				break;
			}
		}
	}
}
