package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.Octree;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;
import org.apache.commons.lang3.mutable.MutableInt;

@Environment(EnvType.CLIENT)
/**
 * {@code OctreeDebugRenderer}.
 */
public class OctreeDebugRenderer implements DebugRenderer.Renderer {

	private final MinecraftClient client;

	public OctreeDebugRenderer(MinecraftClient client) {
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
		Octree octree = this.client.worldRenderer.getChunkRenderingDataPreparer().getOctree();
		MutableInt mutableInt = new MutableInt(0);
		octree.visit((node, bl, i, bl2) -> this.renderNode(node, i, bl, mutableInt, bl2), frustum, 32);
	}

	private void renderNode(Octree.Node node, int i, boolean bl, MutableInt mutableInt, boolean bl2) {
		Box box = node.getBoundingBox();
		double d = box.getLengthX();
		long l = Math.round(d / 16.0);
		if (l == 1L) {
			mutableInt.add(1);
			int j = bl2 ? -16711936 : -1;
			GizmoDrawing.text(
					String.valueOf(mutableInt.intValue()),
					box.getCenter(),
					TextGizmo.Style.left(j).scaled(4.8F)
			);
		}

		long m = l + 5L;
		GizmoDrawing.box(
				box.contract(0.1 * i),
				DrawStyle.stroked(ColorHelper.fromFloats(
						bl ? 0.4F : 1.0F,
						getColorComponent(m, 0.3F),
						getColorComponent(m, 0.8F),
						getColorComponent(m, 0.5F)
				))
		);
	}

	private static float getColorComponent(long size, float gradient) {
		float f = 0.1F;
		return MathHelper.fractionalPart(gradient * (float) size) * 0.9F + 0.1F;
	}
}
