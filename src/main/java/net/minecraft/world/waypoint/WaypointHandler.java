package net.minecraft.world.waypoint;

/**
 * {@code WaypointHandler}.
 */
public interface WaypointHandler<T extends Waypoint> {

	void onTrack(T waypoint);

	void onUpdate(T waypoint);

	void onUntrack(T waypoint);
}
