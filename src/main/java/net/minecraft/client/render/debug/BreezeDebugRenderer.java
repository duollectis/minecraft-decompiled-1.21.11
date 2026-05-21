package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

@Environment(EnvType.CLIENT)
/**
 * {@code BreezeDebugRenderer}.
 */
public class BreezeDebugRenderer implements DebugRenderer.Renderer {

	private static final int PINK = ColorHelper.getArgb(255, 255, 100, 255);
	private static final int LIGHT_BLUE = ColorHelper.getArgb(255, 100, 255, 255);
	private static final int GREEN = ColorHelper.getArgb(255, 0, 255, 0);
	private static final int ORANGE = ColorHelper.getArgb(255, 255, 165, 0);
	private static final int RED = ColorHelper.getArgb(255, 255, 0, 0);
	private final MinecraftClient client;

	public BreezeDebugRenderer(MinecraftClient client) {
		this.client = client;
	}

	@Override
	public void render(
			double cameraX,
			double cameraY,
			double cameraZ,
			DebugDataStore store,
			Frustum frustum,
			float tickProgress
	) {
		ClientWorld clientWorld = this.client.world;
		store.forEachEntityData(
				DebugSubscriptionTypes.BREEZES,
				(entity, breezeDebugData) -> {
					breezeDebugData.attackTarget()
					               .map(clientWorld::getEntityById)
					               .map(entityx -> entityx.getLerpedPos(this.client
							               .getRenderTickCounter()
							               .getTickProgress(true)))
					               .ifPresent(vec3d -> {
						               GizmoDrawing.arrow(entity.getEntityPos(), vec3d, LIGHT_BLUE);
						               Vec3d vec3d2 = vec3d.add(0.0, 0.01F, 0.0);
						               GizmoDrawing.circle(vec3d2, 4.0F, DrawStyle.stroked(GREEN));
						               GizmoDrawing.circle(vec3d2, 8.0F, DrawStyle.stroked(ORANGE));
						               GizmoDrawing.circle(vec3d2, 24.0F, DrawStyle.stroked(RED));
					               });
					breezeDebugData.jumpTarget().ifPresent(blockPos -> {
						GizmoDrawing.arrow(entity.getEntityPos(), blockPos.toCenterPos(), PINK);
						GizmoDrawing.box(
								Box.from(Vec3d.of(blockPos)),
								DrawStyle.filled(ColorHelper.fromFloats(1.0F, 1.0F, 0.0F, 0.0F))
						);
					});
				}
		);
	}
}
