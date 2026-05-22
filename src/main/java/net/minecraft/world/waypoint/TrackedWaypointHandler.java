package net.minecraft.world.waypoint;

/**
 * Специализированный обработчик жизненного цикла {@link TrackedWaypoint}.
 * <p>
 * Используется на клиенте для реакции на события отслеживания вейпоинтов,
 * полученных от сервера через пакеты {@code WaypointS2CPacket}.
 */
public interface TrackedWaypointHandler extends WaypointHandler<TrackedWaypoint> {
}
