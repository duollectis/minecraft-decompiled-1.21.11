package net.minecraft.client.world;

import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.world.waypoint.TrackedWaypoint;
import net.minecraft.world.waypoint.TrackedWaypointHandler;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code ClientWaypointHandler}.
 */
public class ClientWaypointHandler implements TrackedWaypointHandler {

	private final Map<Either<UUID, String>, TrackedWaypoint> waypoints = new ConcurrentHashMap<>();

	/**
	 * Обрабатывает событие track.
	 *
	 * @param trackedWaypoint tracked waypoint
	 */
	public void onTrack(TrackedWaypoint trackedWaypoint) {
		this.waypoints.put(trackedWaypoint.getSource(), trackedWaypoint);
	}

	/**
	 * Обрабатывает событие update.
	 *
	 * @param trackedWaypoint tracked waypoint
	 */
	public void onUpdate(TrackedWaypoint trackedWaypoint) {
		this.waypoints.get(trackedWaypoint.getSource()).handleUpdate(trackedWaypoint);
	}

	/**
	 * Обрабатывает событие untrack.
	 *
	 * @param trackedWaypoint tracked waypoint
	 */
	public void onUntrack(TrackedWaypoint trackedWaypoint) {
		this.waypoints.remove(trackedWaypoint.getSource());
	}

	public boolean hasWaypoint() {
		return !this.waypoints.isEmpty();
	}

	/**
	 * For each waypoint.
	 *
	 * @param receiver receiver
	 * @param action action
	 */
	public void forEachWaypoint(Entity receiver, Consumer<TrackedWaypoint> action) {
		this.waypoints
				.values()
				.stream()
				.sorted(Comparator
						.<TrackedWaypoint>comparingDouble(waypoint -> waypoint.squaredDistanceTo(receiver))
						.reversed())
				.forEachOrdered(action);
	}
}
