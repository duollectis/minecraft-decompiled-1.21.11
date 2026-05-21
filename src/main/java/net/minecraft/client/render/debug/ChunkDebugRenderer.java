package net.minecraft.client.render.debug;

import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.*;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
/**
 * {@code ChunkDebugRenderer}.
 */
public class ChunkDebugRenderer implements DebugRenderer.Renderer {

	public static final Direction[] DIRECTIONS = Direction.values();
	private final MinecraftClient client;

	public ChunkDebugRenderer(MinecraftClient client) {
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
		WorldRenderer worldRenderer = this.client.worldRenderer;
		boolean bl = this.client.debugHudEntryList.isEntryVisible(DebugHudEntries.CHUNK_SECTION_PATHS);
		boolean bl2 = this.client.debugHudEntryList.isEntryVisible(DebugHudEntries.CHUNK_SECTION_VISIBILITY);
		if (bl || bl2) {
			ChunkRenderingDataPreparer chunkRenderingDataPreparer = worldRenderer.getChunkRenderingDataPreparer();
			ObjectListIterator vec3d = worldRenderer.getBuiltChunks().iterator();

			while (vec3d.hasNext()) {
				ChunkBuilder.BuiltChunk builtChunk = (ChunkBuilder.BuiltChunk) vec3d.next();
				ChunkRenderingDataPreparer.ChunkInfo chunkInfo = chunkRenderingDataPreparer.getInfo(builtChunk);
				if (chunkInfo != null) {
					BlockPos blockPos = builtChunk.getOrigin();
					if (bl) {
						int
								i =
								chunkInfo.propagationLevel == 0 ? 0 : MathHelper.hsvToRgb(
										chunkInfo.propagationLevel / 50.0F,
										0.9F,
										0.9F
								);

						for (int j = 0; j < DIRECTIONS.length; j++) {
							if (chunkInfo.hasDirection(j)) {
								Direction direction = DIRECTIONS[j];
								GizmoDrawing.line(
										Vec3d.add(blockPos, 8.0, 8.0, 8.0),
										Vec3d.add(
												blockPos,
												8 - 16 * direction.getOffsetX(),
												8 - 16 * direction.getOffsetY(),
												8 - 16 * direction.getOffsetZ()
										),
										ColorHelper.fullAlpha(i)
								);
							}
						}
					}

					if (bl2 && builtChunk.getCurrentRenderData().hasData()) {
						int i = 0;

						for (Direction direction2 : DIRECTIONS) {
							for (Direction direction3 : DIRECTIONS) {
								boolean
										bl3 =
										builtChunk.getCurrentRenderData().isVisibleThrough(direction2, direction3);
								if (!bl3) {
									i++;
									GizmoDrawing.line(
											Vec3d.add(
													blockPos,
													8 + 8 * direction2.getOffsetX(),
													8 + 8 * direction2.getOffsetY(),
													8 + 8 * direction2.getOffsetZ()
											),
											Vec3d.add(
													blockPos,
													8 + 8 * direction3.getOffsetX(),
													8 + 8 * direction3.getOffsetY(),
													8 + 8 * direction3.getOffsetZ()
											),
											ColorHelper.getArgb(255, 255, 0, 0)
									);
								}
							}
						}

						if (i > 0) {
							float f = 0.5F;
							float g = 0.2F;
							GizmoDrawing.box(
									builtChunk.getBoundingBox().contract(0.5),
									DrawStyle.filled(ColorHelper.fromFloats(0.2F, 0.9F, 0.9F, 0.0F))
							);
						}
					}
				}
			}
		}

		Frustum frustum2 = worldRenderer.getCapturedFrustum();
		if (frustum2 != null) {
			Vec3d vec3d = new Vec3d(frustum2.getX(), frustum2.getY(), frustum2.getZ());
			Vector4f[] vector4fs = frustum2.getBoundaryPoints();
			this.addFace(vec3d, vector4fs, 0, 1, 2, 3, 0, 1, 1);
			this.addFace(vec3d, vector4fs, 4, 5, 6, 7, 1, 0, 0);
			this.addFace(vec3d, vector4fs, 0, 1, 5, 4, 1, 1, 0);
			this.addFace(vec3d, vector4fs, 2, 3, 7, 6, 0, 0, 1);
			this.addFace(vec3d, vector4fs, 0, 4, 7, 3, 0, 1, 0);
			this.addFace(vec3d, vector4fs, 1, 5, 6, 2, 1, 0, 1);
			this.addFrustumEdge(vec3d, vector4fs[0], vector4fs[1]);
			this.addFrustumEdge(vec3d, vector4fs[1], vector4fs[2]);
			this.addFrustumEdge(vec3d, vector4fs[2], vector4fs[3]);
			this.addFrustumEdge(vec3d, vector4fs[3], vector4fs[0]);
			this.addFrustumEdge(vec3d, vector4fs[4], vector4fs[5]);
			this.addFrustumEdge(vec3d, vector4fs[5], vector4fs[6]);
			this.addFrustumEdge(vec3d, vector4fs[6], vector4fs[7]);
			this.addFrustumEdge(vec3d, vector4fs[7], vector4fs[4]);
			this.addFrustumEdge(vec3d, vector4fs[0], vector4fs[4]);
			this.addFrustumEdge(vec3d, vector4fs[1], vector4fs[5]);
			this.addFrustumEdge(vec3d, vector4fs[2], vector4fs[6]);
			this.addFrustumEdge(vec3d, vector4fs[3], vector4fs[7]);
		}
	}

	private void addFrustumEdge(Vec3d origin, Vector4f startOffset, Vector4f endOffset) {
		GizmoDrawing.line(
				new Vec3d(origin.x + startOffset.x, origin.y + startOffset.y, origin.z + startOffset.z),
				new Vec3d(origin.x + endOffset.x, origin.y + endOffset.y, origin.z + endOffset.z),
				-16777216
		);
	}

	private void addFace(
			Vec3d origin,
			Vector4f[] vertexOffsets,
			int i1,
			int i2,
			int i3,
			int i4,
			int red,
			int green,
			int blue
	) {
		float f = 0.25F;
		GizmoDrawing.quad(
				new Vec3d(vertexOffsets[i1].x(), vertexOffsets[i1].y(), vertexOffsets[i1].z()).add(origin),
				new Vec3d(vertexOffsets[i2].x(), vertexOffsets[i2].y(), vertexOffsets[i2].z()).add(origin),
				new Vec3d(vertexOffsets[i3].x(), vertexOffsets[i3].y(), vertexOffsets[i3].z()).add(origin),
				new Vec3d(vertexOffsets[i4].x(), vertexOffsets[i4].y(), vertexOffsets[i4].z()).add(origin),
				DrawStyle.filled(ColorHelper.fromFloats(0.25F, red, green, blue))
		);
	}
}
