package net.minecraft.client.render.debug;

import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;

import java.util.Map;

@Environment(EnvType.CLIENT)
/**
 * {@code GameTestDebugRenderer}.
 */
public class GameTestDebugRenderer {

	private static final int MARKER_LIFESPAN_MS = 10000;
	private static final float MARKER_BOX_SIZE = 0.02F;
	private final Map<BlockPos, GameTestDebugRenderer.Marker> markers = Maps.newHashMap();

	/**
	 * Добавляет marker.
	 *
	 * @param absolutePos absolute pos
	 * @param relativePos relative pos
	 */
	public void addMarker(BlockPos absolutePos, BlockPos relativePos) {
		String string = relativePos.toShortString();
		this.markers.put(
				absolutePos,
				new GameTestDebugRenderer.Marker(1610678016, string, Util.getMeasuringTimeMs() + 10000L)
		);
	}

	/**
	 * Clear.
	 */
	public void clear() {
		this.markers.clear();
	}

	/**
	 * Render.
	 */
	public void render() {
		long l = Util.getMeasuringTimeMs();
		this.markers.entrySet().removeIf(marker -> l > marker.getValue().removalTime);
		this.markers.forEach((pos, marker) -> this.render(pos, marker));
	}

	private void render(BlockPos blockPos, GameTestDebugRenderer.Marker marker) {
		GizmoDrawing.box(blockPos, 0.02F, DrawStyle.filled(marker.color()));
		if (!marker.message.isEmpty()) {
			GizmoDrawing
					.text(marker.message, Vec3d.add(blockPos, 0.5, 1.2, 0.5), TextGizmo.Style.left().scaled(0.16F))
					.ignoreOcclusion();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Marker}.
	 */
	record Marker(int color, String message, long removalTime) {
	}
}
