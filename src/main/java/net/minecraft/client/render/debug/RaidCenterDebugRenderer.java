package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;

@Environment(EnvType.CLIENT)
/**
 * {@code RaidCenterDebugRenderer}.
 */
public class RaidCenterDebugRenderer implements DebugRenderer.Renderer {

	private static final int RANGE = 160;
	private static final float DRAWN_STRING_SIZE = 0.64F;
	private final MinecraftClient client;

	public RaidCenterDebugRenderer(MinecraftClient client) {
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
		BlockPos blockPos = this.getCamera().getBlockPos();
		store.forEachChunkData(
				DebugSubscriptionTypes.RAIDS, (chunkPos, list) -> {
					for (BlockPos blockPos2 : list) {
						if (blockPos.isWithinDistance(blockPos2, 160.0)) {
							drawRaidCenter(blockPos2);
						}
					}
				}
		);
	}

	private static void drawRaidCenter(BlockPos pos) {
		GizmoDrawing.box(pos, DrawStyle.filled(ColorHelper.fromFloats(0.15F, 1.0F, 0.0F, 0.0F)));
		drawString("Raid center", pos, -65536);
	}

	private static void drawString(String text, BlockPos pos, int color) {
		GizmoDrawing
				.text(text, Vec3d.add(pos, 0.5, 1.3, 0.5), TextGizmo.Style.centered(color).scaled(0.64F))
				.ignoreOcclusion();
	}

	private Camera getCamera() {
		return this.client.gameRenderer.getCamera();
	}
}
