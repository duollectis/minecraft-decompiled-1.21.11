package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
/**
 * {@code EntityBlockIntersectionsDebugRenderer}.
 */
public class EntityBlockIntersectionsDebugRenderer implements DebugRenderer.Renderer {

	private static final float EXPANSION = 0.02F;

	@Override
	public void render(
			double cameraX,
			double cameraY,
			double cameraZ,
			DebugDataStore store,
			Frustum frustum,
			float tickProgress
	) {
		store.forEachBlockData(
				DebugSubscriptionTypes.ENTITY_BLOCK_INTERSECTIONS,
				(blockPos, entityBlockIntersectionType) -> GizmoDrawing.box(
						blockPos,
						0.02F,
						DrawStyle.filled(entityBlockIntersectionType.getColor())
				)
		);
	}
}
