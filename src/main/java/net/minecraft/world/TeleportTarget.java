package net.minecraft.world;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

/**
 * Описывает цель телепортации сущности: мир назначения, позицию, скорость,
 * углы поворота, флаги относительного позиционирования и пост-переходный обработчик.
 * Используется при переходе между измерениями и при телепортации через порталы.
 */
public record TeleportTarget(
	ServerWorld world,
	Vec3d position,
	Vec3d velocity,
	float yaw,
	float pitch,
	boolean missingRespawnBlock,
	boolean asPassenger,
	Set<PositionFlag> relatives,
	PostDimensionTransition postTeleportTransition
) {

	/** Пустой пост-переходный обработчик — ничего не делает. */
	public static final PostDimensionTransition NO_OP = entity -> {};

	/** Отправляет клиенту пакет о прохождении через портал ({@link WorldEvents#TRAVEL_THROUGH_PORTAL}). */
	public static final PostDimensionTransition SEND_TRAVEL_THROUGH_PORTAL_PACKET = TeleportTarget::sendTravelThroughPortalPacket;

	/** Добавляет тикет чанка у портала, чтобы чанк оставался загруженным после перехода. */
	public static final PostDimensionTransition ADD_PORTAL_CHUNK_TICKET = TeleportTarget::addPortalChunkTicket;

	public TeleportTarget(
		ServerWorld world,
		Vec3d pos,
		Vec3d velocity,
		float yaw,
		float pitch,
		PostDimensionTransition postDimensionTransition
	) {
		this(world, pos, velocity, yaw, pitch, Set.of(), postDimensionTransition);
	}

	public TeleportTarget(
		ServerWorld world,
		Vec3d pos,
		Vec3d velocity,
		float yaw,
		float pitch,
		Set<PositionFlag> flags,
		PostDimensionTransition postDimensionTransition
	) {
		this(world, pos, velocity, yaw, pitch, false, false, flags, postDimensionTransition);
	}

	/**
	 * Создаёт цель телепортации к точке спавна мира, когда у игрока не задана
	 * точка возрождения (кровать/якорь отсутствует или не установлена).
	 */
	public static TeleportTarget noRespawnPointSet(
		ServerPlayerEntity player,
		PostDimensionTransition postDimensionTransition
	) {
		ServerWorld spawnWorld = player.getEntityWorld().getServer().getSpawnWorld();
		WorldProperties.SpawnPoint spawnPoint = spawnWorld.getSpawnPoint();
		return new TeleportTarget(
			spawnWorld,
			getWorldSpawnPos(spawnWorld, player),
			Vec3d.ZERO,
			spawnPoint.yaw(),
			spawnPoint.pitch(),
			false,
			false,
			Set.of(),
			postDimensionTransition
		);
	}

	/**
	 * Создаёт цель телепортации к точке спавна мира, когда блок возрождения игрока
	 * был уничтожен или заблокирован (флаг {@code missingRespawnBlock = true}).
	 */
	public static TeleportTarget missingSpawnBlock(
		ServerPlayerEntity player,
		PostDimensionTransition postDimensionTransition
	) {
		ServerWorld spawnWorld = player.getEntityWorld().getServer().getSpawnWorld();
		WorldProperties.SpawnPoint spawnPoint = spawnWorld.getSpawnPoint();
		return new TeleportTarget(
			spawnWorld,
			getWorldSpawnPos(spawnWorld, player),
			Vec3d.ZERO,
			spawnPoint.yaw(),
			spawnPoint.pitch(),
			true,
			false,
			Set.of(),
			postDimensionTransition
		);
	}

	/** Возвращает копию цели с изменёнными углами поворота. */
	public TeleportTarget withRotation(float yaw, float pitch) {
		return new TeleportTarget(
			world(),
			position(),
			velocity(),
			yaw,
			pitch,
			missingRespawnBlock(),
			asPassenger(),
			relatives(),
			postTeleportTransition()
		);
	}

	/** Возвращает копию цели с изменённой позицией. */
	public TeleportTarget withPosition(Vec3d position) {
		return new TeleportTarget(
			world(),
			position,
			velocity(),
			yaw(),
			pitch(),
			missingRespawnBlock(),
			asPassenger(),
			relatives(),
			postTeleportTransition()
		);
	}

	/** Возвращает копию цели с флагом {@code asPassenger = true}. */
	public TeleportTarget withAsPassenger() {
		return new TeleportTarget(
			world(),
			position(),
			velocity(),
			yaw(),
			pitch(),
			missingRespawnBlock(),
			true,
			relatives(),
			postTeleportTransition()
		);
	}

	private static void sendTravelThroughPortalPacket(Entity entity) {
		if (entity instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.networkHandler.sendPacket(
				new WorldEventS2CPacket(WorldEvents.TRAVEL_THROUGH_PORTAL, BlockPos.ORIGIN, 0, false)
			);
		}
	}

	private static void addPortalChunkTicket(Entity entity) {
		entity.addPortalChunkTicketAt(BlockPos.ofFloored(entity.getEntityPos()));
	}

	private static Vec3d getWorldSpawnPos(ServerWorld world, Entity entity) {
		return entity.getWorldSpawnPos(world, world.getSpawnPoint().getPos()).toBottomCenterPos();
	}

	/**
	 * Функциональный интерфейс для выполнения действий после перехода сущности
	 * между измерениями или после телепортации через портал.
	 */
	@FunctionalInterface
	public interface PostDimensionTransition {

		void onTransition(Entity entity);

		/**
		 * Создаёт составной обработчик: сначала выполняется {@code this}, затем {@code next}.
		 */
		default PostDimensionTransition then(PostDimensionTransition next) {
			return entity -> {
				onTransition(entity);
				next.onTransition(entity);
			};
		}
	}
}
