package net.minecraft.server.network;

import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.CollisionView;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * {@code SpawnLocating}.
 */
public class SpawnLocating {

	private static final EntityDimensions PLAYER_DIMENSIONS = EntityType.PLAYER.getDimensions();
	private static final int MAX_SPAWN_AREA = 1024;
	private final ServerWorld world;
	private final BlockPos spawnPos;
	private final int spawnRadius;
	private final int spawnArea;
	private final int shiftAmount;
	private final int offset;
	private int attempt;
	private final CompletableFuture<Vec3d> future = new CompletableFuture<>();

	private SpawnLocating(ServerWorld world, BlockPos spawnPos, int spawnRadius) {
		this.world = world;
		this.spawnPos = spawnPos;
		this.spawnRadius = spawnRadius;
		long l = spawnRadius * 2L + 1L;
		this.spawnArea = (int) Math.min(1024L, l * l);
		this.shiftAmount = calculateShiftAmount(this.spawnArea);
		this.offset = Random.create().nextInt(this.spawnArea);
	}

	/**
	 * Locate spawn pos.
	 *
	 * @param world world
	 * @param spawnPos spawn pos
	 *
	 * @return CompletableFuture — результат операции
	 */
	public static CompletableFuture<Vec3d> locateSpawnPos(ServerWorld world, BlockPos spawnPos) {
		if (world.getDimension().hasSkyLight()
				&& world.getServer().getSaveProperties().getGameMode() != GameMode.ADVENTURE) {
			int i = Math.max(0, world.getGameRules().getValue(GameRules.RESPAWN_RADIUS));
			int j = MathHelper.floor(world.getWorldBorder().getDistanceInsideBorder(spawnPos.getX(), spawnPos.getZ()));
			if (j < i) {
				i = j;
			}

			if (j <= 1) {
				i = 1;
			}

			SpawnLocating spawnLocating = new SpawnLocating(world, spawnPos, i);
			spawnLocating.scheduleNextSearch();
			return spawnLocating.future;
		}
		else {
			return CompletableFuture.completedFuture(findPosInColumn(world, spawnPos));
		}
	}

	private void scheduleNextSearch() {
		int i = this.attempt++;
		if (i < this.spawnArea) {
			int j = (this.offset + this.shiftAmount * i) % this.spawnArea;
			int k = j % (this.spawnRadius * 2 + 1);
			int l = j / (this.spawnRadius * 2 + 1);
			int m = this.spawnPos.getX() + k - this.spawnRadius;
			int n = this.spawnPos.getZ() + l - this.spawnRadius;
			this.scheduleSearch(
					m, n, i, () -> {
						BlockPos blockPos = findOverworldSpawn(this.world, m, n);
						return blockPos != null && isSpaceEmpty(this.world, blockPos)
						       ? Optional.of(Vec3d.ofBottomCenter(blockPos)) : Optional.empty();
					}
			);
		}
		else {
			this.scheduleSearch(
					this.spawnPos.getX(),
					this.spawnPos.getZ(),
					i,
					() -> Optional.of(findPosInColumn(this.world, this.spawnPos))
			);
		}
	}

	private static Vec3d findPosInColumn(CollisionView world, BlockPos pos) {
		BlockPos.Mutable mutable = pos.mutableCopy();

		while (!isSpaceEmpty(world, mutable) && mutable.getY() < world.getTopYInclusive()) {
			mutable.move(Direction.UP);
		}

		mutable.move(Direction.DOWN);

		while (isSpaceEmpty(world, mutable) && mutable.getY() > world.getBottomY()) {
			mutable.move(Direction.DOWN);
		}

		mutable.move(Direction.UP);
		return Vec3d.ofBottomCenter(mutable);
	}

	private static boolean isSpaceEmpty(CollisionView world, BlockPos pos) {
		return world.isSpaceEmpty(null, PLAYER_DIMENSIONS.getBoxAt(pos.toBottomCenterPos()), true);
	}

	private static int calculateShiftAmount(int spawnArea) {
		return spawnArea <= 16 ? spawnArea - 1 : 17;
	}

	private void scheduleSearch(int x, int z, int index, Supplier<Optional<Vec3d>> spawnFinder) {
		if (!this.future.isDone()) {
			int i = ChunkSectionPos.getSectionCoord(x);
			int j = ChunkSectionPos.getSectionCoord(z);
			this.world
					.getChunkManager()
					.addChunkLoadingTicket(ChunkTicketType.SPAWN_SEARCH, new ChunkPos(i, j), 0)
					.whenCompleteAsync(
							(object, throwable) -> {
								if (throwable == null) {
									try {
										Optional<Vec3d> optional = spawnFinder.get();
										if (optional.isPresent()) {
											this.future.complete(optional.get());
										}
										else {
											this.scheduleNextSearch();
										}
									}
									catch (Throwable var9) {
										throwable = var9;
									}
								}

								if (throwable != null) {
									CrashReport crashReport = CrashReport.create(throwable, "Searching for spawn");
									CrashReportSection crashReportSection = crashReport.addElement("Spawn Lookup");
									crashReportSection.add("Origin", this.spawnPos::toString);
									crashReportSection.add("Radius", () -> Integer.toString(this.spawnRadius));
									crashReportSection.add("Candidate", () -> "[" + x + "," + z + "]");
									crashReportSection.add("Progress", () -> index + " out of " + this.spawnArea);
									this.future.completeExceptionally(new CrashException(crashReport));
								}
							}, this.world.getServer()
					);
		}
	}

	/**
	 * Ищет overworld spawn.
	 *
	 * @param world world
	 * @param x x
	 * @param z z
	 *
	 * @return @Nullable BlockPos — overworld spawn
	 */
	protected static @Nullable BlockPos findOverworldSpawn(ServerWorld world, int x, int z) {
		boolean bl = world.getDimension().hasCeiling();
		WorldChunk worldChunk = world.getChunk(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
		int i = bl
		        ? world.getChunkManager().getChunkGenerator().getSpawnHeight(world)
		        : worldChunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x & 15, z & 15);
		if (i < world.getBottomY()) {
			return null;
		}
		else {
			int j = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15);
			if (j <= i && j > worldChunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, x & 15, z & 15)) {
				return null;
			}
			else {
				BlockPos.Mutable mutable = new BlockPos.Mutable();

				for (int k = i + 1; k >= world.getBottomY(); k--) {
					mutable.set(x, k, z);
					BlockState blockState = world.getBlockState(mutable);
					if (!blockState.getFluidState().isEmpty()) {
						break;
					}

					if (Block.isFaceFullSquare(blockState.getCollisionShape(world, mutable), Direction.UP)) {
						return mutable.up().toImmutable();
					}
				}

				return null;
			}
		}
	}

	/**
	 * Ищет server spawn point.
	 *
	 * @param world world
	 * @param chunkPos chunk pos
	 *
	 * @return @Nullable BlockPos — server spawn point
	 */
	public static @Nullable BlockPos findServerSpawnPoint(ServerWorld world, ChunkPos chunkPos) {
		if (SharedConstants.isOutsideGenerationArea(chunkPos)) {
			return null;
		}
		else {
			for (int i = chunkPos.getStartX(); i <= chunkPos.getEndX(); i++) {
				for (int j = chunkPos.getStartZ(); j <= chunkPos.getEndZ(); j++) {
					BlockPos blockPos = findOverworldSpawn(world, i, j);
					if (blockPos != null) {
						return blockPos;
					}
				}
			}

			return null;
		}
	}
}
