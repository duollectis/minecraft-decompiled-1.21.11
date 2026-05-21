package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
/**
 * {@code RedstoneUpdateOrderDebugRenderer}.
 */
public class RedstoneUpdateOrderDebugRenderer implements DebugRenderer.Renderer {

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
				DebugSubscriptionTypes.REDSTONE_WIRE_ORIENTATIONS, (blockPos, orientation) -> {
					Vec3d vec3d = blockPos.toBottomCenterPos().subtract(0.0, 0.1, 0.0);
					GizmoDrawing.arrow(
							vec3d,
							vec3d.add(orientation.getFront().getDoubleVector().multiply(0.5)),
							-16776961
					);
					GizmoDrawing.arrow(vec3d, vec3d.add(orientation.getUp().getDoubleVector().multiply(0.4)), -65536);
					GizmoDrawing.arrow(vec3d, vec3d.add(orientation.getRight().getDoubleVector().multiply(0.3)), -256);
				}
		);
	}
}
