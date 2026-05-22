package net.minecraft.server.network;

import com.google.common.collect.Comparators;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.SharedConstants;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkSentS2CPacket;
import net.minecraft.network.packet.s2c.play.StartChunkSendS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Класс Chunk Data Sender.
 */
public class ChunkDataSender {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final float MIN_BATCH_SIZE = 0.01F;
	public static final float MAX_BATCH_SIZE = 64.0F;
	private static final float DEFAULT_BATCH_SIZE = 9.0F;
	private static final int MAX_UNACKNOWLEDGED_BATCHES = 10;
	private final LongSet chunks = new LongOpenHashSet();
	private final boolean local;
	private float desiredBatchSize = 9.0F;
	private float pending;
	private int unacknowledgedBatches;
	private int maxUnacknowledgedBatches = 1;

	public ChunkDataSender(boolean local) {
		this.local = local;
	}

	/**
	 * Add.
	 *
	 * @param chunk chunk
	 */
	public void add(WorldChunk chunk) {
		this.chunks.add(chunk.getPos().toLong());
	}

	/**
	 * Unload.
	 *
	 * @param player player
	 * @param pos pos
	 */
	public void unload(ServerPlayerEntity player, ChunkPos pos) {
		if (!this.chunks.remove(pos.toLong()) && player.isAlive()) {
			player.networkHandler.sendPacket(new UnloadChunkS2CPacket(pos));
		}
	}

	/**
	 * Отправляет chunk batches.
	 *
	 * @param player player
	 */
	public void sendChunkBatches(ServerPlayerEntity player) {
		if (this.unacknowledgedBatches < this.maxUnacknowledgedBatches) {
			float f = Math.max(1.0F, this.desiredBatchSize);
			this.pending = Math.min(this.pending + this.desiredBatchSize, f);
			if (!(this.pending < 1.0F)) {
				if (!this.chunks.isEmpty()) {
					ServerWorld serverWorld = player.getEntityWorld();
					ServerChunkLoadingManager
							serverChunkLoadingManager =
							serverWorld.getChunkManager().chunkLoadingManager;
					List<WorldChunk> list = this.makeBatch(serverChunkLoadingManager, player.getChunkPos());
					if (!list.isEmpty()) {
						ServerPlayNetworkHandler serverPlayNetworkHandler = player.networkHandler;
						this.unacknowledgedBatches++;
						serverPlayNetworkHandler.sendPacket(StartChunkSendS2CPacket.INSTANCE);

						for (WorldChunk worldChunk : list) {
							sendChunkData(serverPlayNetworkHandler, serverWorld, worldChunk);
						}

						serverPlayNetworkHandler.sendPacket(new ChunkSentS2CPacket(list.size()));
						this.pending = this.pending - list.size();
					}
				}
			}
		}
	}

	private static void sendChunkData(ServerPlayNetworkHandler handler, ServerWorld world, WorldChunk chunk) {
		handler.sendPacket(new ChunkDataS2CPacket(chunk, world.getLightingProvider(), null, null));
		ChunkPos chunkPos = chunk.getPos();
		if (SharedConstants.VERBOSE_SERVER_EVENTS) {
			LOGGER.debug("SEN {}", chunkPos);
		}

		world.getSubscriptionTracker().sendInitialIfSubscribed(handler.player, chunk.getPos());
	}

	private List<WorldChunk> makeBatch(ServerChunkLoadingManager chunkLoadingManager, ChunkPos playerPos) {
		int i = MathHelper.floor(this.pending);
		List<WorldChunk> list;
		if (!this.local && this.chunks.size() > i) {
			list = this.chunks
					.stream()
					.collect(Comparators.least(i, Comparator.comparingInt(playerPos::getSquaredDistance)))
					.stream()
					.mapToLong(Long::longValue)
					.mapToObj(chunkLoadingManager::getPostProcessedChunk)
					.filter(Objects::nonNull)
					.toList();
		}
		else {
			list = this.chunks
					.longStream()
					.mapToObj(chunkLoadingManager::getPostProcessedChunk)
					.filter(Objects::nonNull)
					.sorted(Comparator.comparingInt(chunk -> playerPos.getSquaredDistance(chunk.getPos())))
					.toList();
		}

		for (WorldChunk worldChunk : list) {
			this.chunks.remove(worldChunk.getPos().toLong());
		}

		return list;
	}

	/**
	 * Обрабатывает событие acknowledge chunks.
	 *
	 * @param desiredBatchSize desired batch size
	 */
	public void onAcknowledgeChunks(float desiredBatchSize) {
		this.unacknowledgedBatches--;
		this.desiredBatchSize =
				Double.isNaN(desiredBatchSize) ? MIN_BATCH_SIZE : MathHelper.clamp(desiredBatchSize, MIN_BATCH_SIZE, MAX_BATCH_SIZE);
		if (this.unacknowledgedBatches == 0) {
			this.pending = 1.0F;
		}

		this.maxUnacknowledgedBatches = MAX_UNACKNOWLEDGED_BATCHES;
	}

	public boolean isInNextBatch(long chunkPos) {
		return this.chunks.contains(chunkPos);
	}
}
