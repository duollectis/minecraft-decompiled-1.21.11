package net.minecraft.server.network;

import com.google.common.collect.*;
import com.google.common.collect.Sets.SetView;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.waypoint.ServerWaypoint;
import net.minecraft.world.waypoint.WaypointHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * {@code ServerWaypointHandler}.
 */
public class ServerWaypointHandler implements WaypointHandler<ServerWaypoint> {

	private final Set<ServerWaypoint> waypoints = new HashSet<>();
	private final Set<ServerPlayerEntity> players = new HashSet<>();
	private final Table<ServerPlayerEntity, ServerWaypoint, ServerWaypoint.WaypointTracker>
			trackers =
			HashBasedTable.create();

	/**
	 * Обрабатывает событие track.
	 *
	 * @param serverWaypoint server waypoint
	 */
	public void onTrack(ServerWaypoint serverWaypoint) {
		this.waypoints.add(serverWaypoint);

		for (ServerPlayerEntity serverPlayerEntity : this.players) {
			this.refreshTracking(serverPlayerEntity, serverWaypoint);
		}
	}

	/**
	 * Обрабатывает событие update.
	 *
	 * @param serverWaypoint server waypoint
	 */
	public void onUpdate(ServerWaypoint serverWaypoint) {
		if (this.waypoints.contains(serverWaypoint)) {
			Map<ServerPlayerEntity, ServerWaypoint.WaypointTracker>
					map =
					Tables.transpose(this.trackers).row(serverWaypoint);
			SetView<ServerPlayerEntity> setView = Sets.difference(this.players, map.keySet());
			UnmodifiableIterator var4 = ImmutableSet.copyOf(map.entrySet()).iterator();

			while (var4.hasNext()) {
				Entry<ServerPlayerEntity, ServerWaypoint.WaypointTracker>
						entry =
						(Entry<ServerPlayerEntity, ServerWaypoint.WaypointTracker>) var4.next();
				this.refreshTracking(entry.getKey(), serverWaypoint, entry.getValue());
			}

			var4 = setView.iterator();

			while (var4.hasNext()) {
				ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) var4.next();
				this.refreshTracking(serverPlayerEntity, serverWaypoint);
			}
		}
	}

	/**
	 * Обрабатывает событие untrack.
	 *
	 * @param serverWaypoint server waypoint
	 */
	public void onUntrack(ServerWaypoint serverWaypoint) {
		this.trackers.column(serverWaypoint).forEach((player, tracker) -> tracker.untrack());
		Tables.transpose(this.trackers).row(serverWaypoint).clear();
		this.waypoints.remove(serverWaypoint);
	}

	/**
	 * Добавляет player.
	 *
	 * @param player player
	 */
	public void addPlayer(ServerPlayerEntity player) {
		this.players.add(player);

		for (ServerWaypoint serverWaypoint : this.waypoints) {
			this.refreshTracking(player, serverWaypoint);
		}

		if (player.hasWaypoint()) {
			this.onTrack((ServerWaypoint) player);
		}
	}

	/**
	 * Обновляет player pos.
	 *
	 * @param player player
	 */
	public void updatePlayerPos(ServerPlayerEntity player) {
		Map<ServerWaypoint, ServerWaypoint.WaypointTracker> map = this.trackers.row(player);
		SetView<ServerWaypoint> setView = Sets.difference(this.waypoints, map.keySet());
		UnmodifiableIterator var4 = ImmutableSet.copyOf(map.entrySet()).iterator();

		while (var4.hasNext()) {
			Entry<ServerWaypoint, ServerWaypoint.WaypointTracker>
					entry =
					(Entry<ServerWaypoint, ServerWaypoint.WaypointTracker>) var4.next();
			this.refreshTracking(player, entry.getKey(), entry.getValue());
		}

		var4 = setView.iterator();

		while (var4.hasNext()) {
			ServerWaypoint serverWaypoint = (ServerWaypoint) var4.next();
			this.refreshTracking(player, serverWaypoint);
		}
	}

	/**
	 * Удаляет player.
	 *
	 * @param player player
	 */
	public void removePlayer(ServerPlayerEntity player) {
		this.trackers.row(player).values().removeIf(tracker -> {
			tracker.untrack();
			return true;
		});
		this.onUntrack((ServerWaypoint) player);
		this.players.remove(player);
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.trackers.values().forEach(ServerWaypoint.WaypointTracker::untrack);
		this.trackers.clear();
	}

	/**
	 * Refresh tracking.
	 *
	 * @param waypoint waypoint
	 */
	public void refreshTracking(ServerWaypoint waypoint) {
		for (ServerPlayerEntity serverPlayerEntity : this.players) {
			this.refreshTracking(serverPlayerEntity, waypoint);
		}
	}

	public Set<ServerWaypoint> getWaypoints() {
		return this.waypoints;
	}

	private static boolean isLocatorBarEnabled(ServerPlayerEntity player) {
		return player.getEntityWorld().getGameRules().getValue(GameRules.LOCATOR_BAR);
	}

	private void refreshTracking(ServerPlayerEntity player, ServerWaypoint waypoint) {
		if (player != waypoint) {
			if (isLocatorBarEnabled(player)) {
				waypoint.createTracker(player).ifPresentOrElse(
						tracker -> {
							this.trackers.put(player, waypoint, tracker);
							tracker.track();
						}, () -> {
							ServerWaypoint.WaypointTracker
									waypointTracker =
									(ServerWaypoint.WaypointTracker) this.trackers.remove(player, waypoint);
							if (waypointTracker != null) {
								waypointTracker.untrack();
							}
						}
				);
			}
		}
	}

	private void refreshTracking(
			ServerPlayerEntity player,
			ServerWaypoint waypoint,
			ServerWaypoint.WaypointTracker tracker
	) {
		if (player != waypoint) {
			if (isLocatorBarEnabled(player)) {
				if (!tracker.isInvalid()) {
					tracker.update();
				}
				else {
					waypoint.createTracker(player).ifPresentOrElse(
							newTracker -> {
								newTracker.track();
								this.trackers.put(player, waypoint, newTracker);
							}, () -> {
								tracker.untrack();
								this.trackers.remove(player, waypoint);
							}
					);
				}
			}
		}
	}
}
