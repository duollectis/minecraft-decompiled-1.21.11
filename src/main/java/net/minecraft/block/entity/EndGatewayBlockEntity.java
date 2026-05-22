package net.minecraft.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.feature.EndConfiguredFeatures;
import net.minecraft.world.gen.feature.EndGatewayFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Блок-сущность врат Края. Управляет телепортацией между островами Края:
 * отслеживает кулдаун, находит или создаёт выходной портал на целевом острове.
 */
public class EndGatewayBlockEntity extends EndPortalBlockEntity {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int RECENTLY_GENERATED_AGE_TICKS = 200;
	private static final int TELEPORT_COOLDOWN_TICKS = 40;
	private static final int PERIODIC_COOLDOWN_INTERVAL_TICKS = 2400;
	private static final int EXIT_PORTAL_SPAWN_HEIGHT_OFFSET = 10;
	private static final int SYNCED_EVENT_COOLDOWN_TYPE = 1;
	private static final int ISLAND_SEARCH_RADIUS = 1024;
	private static final int ISLAND_SEARCH_STEPS = 16;

	private long age;
	private int teleportCooldown;
	private @Nullable BlockPos exitPortalPos;
	private boolean exactTeleport;

	public EndGatewayBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(BlockEntityType.END_GATEWAY, blockPos, blockState);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.putLong("Age", age);
		view.putNullable("exit_portal", BlockPos.CODEC, exitPortalPos);
		if (exactTeleport) {
			view.putBoolean("ExactTeleport", true);
		}
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		age = view.getLong("Age", 0L);
		exitPortalPos = view.<BlockPos>read("exit_portal", BlockPos.CODEC).filter(World::isValid).orElse(null);
		exactTeleport = view.getBoolean("ExactTeleport", false);
	}

	public static void clientTick(World world, BlockPos pos, BlockState state, EndGatewayBlockEntity blockEntity) {
		blockEntity.age++;
		if (blockEntity.needsCooldownBeforeTeleporting()) {
			blockEntity.teleportCooldown--;
		}
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, EndGatewayBlockEntity blockEntity) {
		boolean wasRecentlyGenerated = blockEntity.isRecentlyGenerated();
		boolean wasOnCooldown = blockEntity.needsCooldownBeforeTeleporting();
		blockEntity.age++;

		if (wasOnCooldown) {
			blockEntity.teleportCooldown--;
		} else if (blockEntity.age % PERIODIC_COOLDOWN_INTERVAL_TICKS == 0L) {
			startTeleportCooldown(world, pos, state, blockEntity);
		}

		if (wasRecentlyGenerated != blockEntity.isRecentlyGenerated()
				|| wasOnCooldown != blockEntity.needsCooldownBeforeTeleporting()
		) {
			markDirty(world, pos, state);
		}
	}

	public boolean isRecentlyGenerated() {
		return age < RECENTLY_GENERATED_AGE_TICKS;
	}

	public boolean needsCooldownBeforeTeleporting() {
		return teleportCooldown > 0;
	}

	public float getRecentlyGeneratedBeamHeight(float tickProgress) {
		return MathHelper.clamp(((float) age + tickProgress) / RECENTLY_GENERATED_AGE_TICKS, 0.0F, 1.0F);
	}

	public float getCooldownBeamHeight(float tickProgress) {
		return 1.0F - MathHelper.clamp((teleportCooldown - tickProgress) / TELEPORT_COOLDOWN_TICKS, 0.0F, 1.0F);
	}

	@Override
	public BlockEntityUpdateS2CPacket toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createComponentlessNbt(registries);
	}

	public static void startTeleportCooldown(
			World world,
			BlockPos pos,
			BlockState state,
			EndGatewayBlockEntity blockEntity
	) {
		if (!world.isClient()) {
			blockEntity.teleportCooldown = TELEPORT_COOLDOWN_TICKS;
			world.addSyncedBlockEvent(pos, state.getBlock(), 1, 0);
			markDirty(world, pos, state);
		}
	}

	@Override
	public boolean onSyncedBlockEvent(int type, int data) {
		if (type == SYNCED_EVENT_COOLDOWN_TYPE) {
			teleportCooldown = TELEPORT_COOLDOWN_TICKS;
			return true;
		}

		return super.onSyncedBlockEvent(type, data);
	}

	/**
	 * Возвращает позицию выходного портала, создавая его при необходимости.
	 * Если портал ещё не создан и мир — Край, генерирует новый остров с порталом.
	 */
	public @Nullable Vec3d getOrCreateExitPortalPos(ServerWorld world, BlockPos pos) {
		if (exitPortalPos == null && world.getRegistryKey() == World.END) {
			BlockPos spawnPos = setupExitPortalLocation(world, pos).up(EXIT_PORTAL_SPAWN_HEIGHT_OFFSET);
			LOGGER.debug("Creating portal at {}", spawnPos);
			createPortal(world, spawnPos, EndGatewayFeatureConfig.createConfig(pos, false));
			setExitPortalPos(spawnPos, exactTeleport);
		}

		if (exitPortalPos == null) {
			return null;
		}

		BlockPos exitPos = exactTeleport ? exitPortalPos : findBestPortalExitPos(world, exitPortalPos);
		return exitPos.toBottomCenterPos();
	}

	private static BlockPos findBestPortalExitPos(World world, BlockPos pos) {
		BlockPos blockPos = findExitPortalPos(world, pos.add(0, 2, 0), 5, false);
		LOGGER.debug("Best exit position for portal at {} is {}", pos, blockPos);
		return blockPos.up();
	}

	private static BlockPos setupExitPortalLocation(ServerWorld world, BlockPos pos) {
		Vec3d vec3d = findTeleportLocation(world, pos);
		WorldChunk worldChunk = getChunk(world, vec3d);
		BlockPos blockPos = findPortalPosition(worldChunk);
		if (blockPos == null) {
			BlockPos blockPos2 = BlockPos.ofFloored(vec3d.x + 0.5, 75.0, vec3d.z + 0.5);
			LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", blockPos2);
			world.getRegistryManager()
			     .getOptional(RegistryKeys.CONFIGURED_FEATURE)
			     .flatMap(registry -> registry.getOptional(EndConfiguredFeatures.END_ISLAND))
			     .ifPresent(
					     reference -> reference
							     .value()
							     .generate(
									     world,
									     world.getChunkManager().getChunkGenerator(),
									     Random.create(blockPos2.asLong()),
									     blockPos2
							     )
			     );
			blockPos = blockPos2;
		}
		else {
			LOGGER.debug("Found suitable block to teleport to: {}", blockPos);
		}

		return findExitPortalPos(world, blockPos, Block.FORCE_STATE, true);
	}

	private static Vec3d findTeleportLocation(ServerWorld world, BlockPos pos) {
		Vec3d direction = new Vec3d(pos.getX(), 0.0, pos.getZ()).normalize();
		Vec3d searchPos = direction.multiply(ISLAND_SEARCH_RADIUS);

		for (int steps = ISLAND_SEARCH_STEPS; !isChunkEmpty(world, searchPos) && steps-- > 0; searchPos = searchPos.add(direction.multiply(-16.0))) {
			LOGGER.debug("Skipping backwards past nonempty chunk at {}", searchPos);
		}

		for (int steps = ISLAND_SEARCH_STEPS; isChunkEmpty(world, searchPos) && steps-- > 0; searchPos = searchPos.add(direction.multiply(16.0))) {
			LOGGER.debug("Skipping forward past empty chunk at {}", searchPos);
		}

		LOGGER.debug("Found chunk at {}", searchPos);
		return searchPos;
	}

	private static boolean isChunkEmpty(ServerWorld world, Vec3d pos) {
		return getChunk(world, pos).getHighestNonEmptySection() == -1;
	}

	private static BlockPos findExitPortalPos(BlockView world, BlockPos pos, int searchRadius, boolean force) {
		BlockPos bestPos = null;

		for (int dx = -searchRadius; dx <= searchRadius; dx++) {
			for (int dz = -searchRadius; dz <= searchRadius; dz++) {
				if (dx == 0 && dz == 0 && !force) {
					continue;
				}

				int minY = bestPos == null ? world.getBottomY() : bestPos.getY();
				for (int y = world.getTopYInclusive(); y > minY; y--) {
					BlockPos candidate = new BlockPos(pos.getX() + dx, y, pos.getZ() + dz);
					BlockState candidateState = world.getBlockState(candidate);
					if (candidateState.isFullCube(world, candidate) && (force || !candidateState.isOf(Blocks.BEDROCK))) {
						bestPos = candidate;
						break;
					}
				}
			}
		}

		return bestPos == null ? pos : bestPos;
	}

	private static WorldChunk getChunk(World world, Vec3d pos) {
		return world.getChunk(MathHelper.floor(pos.x / 16.0), MathHelper.floor(pos.z / 16.0));
	}

	private static @Nullable BlockPos findPortalPosition(WorldChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		BlockPos searchStart = new BlockPos(chunkPos.getStartX(), 30, chunkPos.getStartZ());
		int topY = chunk.getHighestNonEmptySectionYOffset() + 16 - 1;
		BlockPos searchEnd = new BlockPos(chunkPos.getEndX(), topY, chunkPos.getEndZ());
		BlockPos bestPos = null;
		double bestDistSq = 0.0;

		for (BlockPos candidate : BlockPos.iterate(searchStart, searchEnd)) {
			BlockState candidateState = chunk.getBlockState(candidate);
			BlockPos above1 = candidate.up();
			BlockPos above2 = candidate.up(2);

			if (candidateState.isOf(Blocks.END_STONE)
					&& !chunk.getBlockState(above1).isFullCube(chunk, above1)
					&& !chunk.getBlockState(above2).isFullCube(chunk, above2)
			) {
				double distSq = candidate.getSquaredDistanceFromCenter(0.0, 0.0, 0.0);
				if (bestPos == null || distSq < bestDistSq) {
					bestPos = candidate;
					bestDistSq = distSq;
				}
			}
		}

		return bestPos;
	}

	private static void createPortal(ServerWorld world, BlockPos pos, EndGatewayFeatureConfig config) {
		Feature.END_GATEWAY.generateIfValid(
				config,
				world,
				world.getChunkManager().getChunkGenerator(),
				Random.create(),
				pos
		);
	}

	@Override
	public boolean shouldDrawSide(Direction direction) {
		return Block.shouldDrawSide(
				getCachedState(),
				world.getBlockState(getPos().offset(direction)),
				direction
		);
	}

	public int getDrawnSidesCount() {
		int count = 0;

		for (Direction direction : Direction.values()) {
			count += shouldDrawSide(direction) ? 1 : 0;
		}

		return count;
	}

	public void setExitPortalPos(BlockPos pos, boolean exactTeleport) {
		this.exactTeleport = exactTeleport;
		exitPortalPos = pos;
		markDirty();
	}
}
