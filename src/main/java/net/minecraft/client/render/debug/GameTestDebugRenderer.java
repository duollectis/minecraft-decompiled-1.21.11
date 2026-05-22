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

/**
 * Рендерит отладочные маркеры игровых тестов (Game Tests) в мировом пространстве.
 * Каждый маркер отображается как небольшой закрашенный бокс с текстовой меткой
 * и автоматически удаляется через {@code MARKER_LIFESPAN_MS} миллисекунд.
 */
@Environment(EnvType.CLIENT)
public class GameTestDebugRenderer {

	private static final int MARKER_LIFESPAN_MS = 10000;
	private static final float MARKER_BOX_SIZE = 0.02F;
	// 0x5FFF0000 — полупрозрачный красный цвет маркера
	private static final int MARKER_COLOR = 1610678016;
	// Смещение текста по Y над центром блока
	private static final double MARKER_TEXT_Y_OFFSET = 1.2;
	private static final float MARKER_TEXT_SCALE = 0.16F;

	private final Map<BlockPos, GameTestDebugRenderer.Marker> markers = Maps.newHashMap();

	public void addMarker(BlockPos absolutePos, BlockPos relativePos) {
		markers.put(
				absolutePos,
				new GameTestDebugRenderer.Marker(MARKER_COLOR, relativePos.toShortString(), Util.getMeasuringTimeMs() + MARKER_LIFESPAN_MS)
		);
	}

	public void clear() {
		markers.clear();
	}

	public void render() {
		long now = Util.getMeasuringTimeMs();
		markers.entrySet().removeIf(entry -> now > entry.getValue().removalTime);
		markers.forEach((pos, marker) -> render(pos, marker));
	}

	private void render(BlockPos blockPos, GameTestDebugRenderer.Marker marker) {
		GizmoDrawing.box(blockPos, MARKER_BOX_SIZE, DrawStyle.filled(marker.color()));

		if (!marker.message.isEmpty()) {
			GizmoDrawing
					.text(marker.message, Vec3d.add(blockPos, 0.5, MARKER_TEXT_Y_OFFSET, 0.5), TextGizmo.Style.left().scaled(MARKER_TEXT_SCALE))
					.ignoreOcclusion();
		}
	}

	/** Данные одного отладочного маркера: цвет, сообщение и время автоудаления. */
	@Environment(EnvType.CLIENT)
	record Marker(int color, String message, long removalTime) {
	}
}
