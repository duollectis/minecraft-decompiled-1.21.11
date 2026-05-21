package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightStorage;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

@Environment(EnvType.CLIENT)
/**
 * {@code LightDebugRenderer}.
 */
public class LightDebugRenderer implements DebugRenderer.Renderer {

	private static final Duration UPDATE_INTERVAL = Duration.ofMillis(500L);
	private static final int RADIUS = 10;
	private static final int READY_SHAPE_COLOR = ColorHelper.fromFloats(0.25F, 1.0F, 1.0F, 0.0F);
	private static final int DEFAULT_SHAPE_COLOR = ColorHelper.fromFloats(0.125F, 0.25F, 0.125F, 0.0F);
	private final MinecraftClient client;
	private final LightType lightType;
	private Instant lastUpdateTime = Instant.now();
	private LightDebugRenderer.@Nullable Data data;

	public LightDebugRenderer(MinecraftClient client, LightType lightType) {
		this.client = client;
		this.lightType = lightType;
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
		Instant instant = Instant.now();
		if (this.data == null || Duration.between(this.lastUpdateTime, instant).compareTo(UPDATE_INTERVAL) > 0) {
			this.lastUpdateTime = instant;
			this.data = new LightDebugRenderer.Data(
					this.client.world.getLightingProvider(),
					ChunkSectionPos.from(this.client.player.getBlockPos()),
					10,
					this.lightType
			);
		}

		drawEdges(this.data.readyShape, this.data.minSectionPos, READY_SHAPE_COLOR);
		drawEdges(this.data.shape, this.data.minSectionPos, DEFAULT_SHAPE_COLOR);
		drawFaces(this.data.readyShape, this.data.minSectionPos, READY_SHAPE_COLOR);
		drawFaces(this.data.shape, this.data.minSectionPos, DEFAULT_SHAPE_COLOR);
	}

	private static void drawFaces(VoxelSet voxelSet, ChunkSectionPos chunkSectionPos, int i) {
		voxelSet.forEachDirection((direction, j, k, l) -> {
			int m = j + chunkSectionPos.getX();
			int n = k + chunkSectionPos.getY();
			int o = l + chunkSectionPos.getZ();
			drawFace(direction, m, n, o, i);
		});
	}

	private static void drawEdges(VoxelSet voxelSet, ChunkSectionPos chunkSectionPos, int i) {
		voxelSet.forEachEdge(
				(j, k, l, m, n, o) -> {
					int p = j + chunkSectionPos.getX();
					int q = k + chunkSectionPos.getY();
					int r = l + chunkSectionPos.getZ();
					int s = m + chunkSectionPos.getX();
					int t = n + chunkSectionPos.getY();
					int u = o + chunkSectionPos.getZ();
					drawEdge(p, q, r, s, t, u, i);
				}, true
		);
	}

	private static void drawFace(Direction direction, int i, int j, int k, int l) {
		Vec3d
				vec3d =
				new Vec3d(
						ChunkSectionPos.getBlockCoord(i),
						ChunkSectionPos.getBlockCoord(j),
						ChunkSectionPos.getBlockCoord(k)
				);
		Vec3d vec3d2 = vec3d.add(16.0, 16.0, 16.0);
		GizmoDrawing.face(vec3d, vec3d2, direction, DrawStyle.filled(l));
	}

	private static void drawEdge(int i, int j, int k, int l, int m, int n, int o) {
		double d = ChunkSectionPos.getBlockCoord(i);
		double e = ChunkSectionPos.getBlockCoord(j);
		double f = ChunkSectionPos.getBlockCoord(k);
		double g = ChunkSectionPos.getBlockCoord(l);
		double h = ChunkSectionPos.getBlockCoord(m);
		double p = ChunkSectionPos.getBlockCoord(n);
		int q = ColorHelper.fullAlpha(o);
		GizmoDrawing.line(new Vec3d(d, e, f), new Vec3d(g, h, p), q);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Data}.
	 */
	static final class Data {

		final VoxelSet readyShape;
		final VoxelSet shape;
		final ChunkSectionPos minSectionPos;

		Data(LightingProvider lightingProvider, ChunkSectionPos sectionPos, int radius, LightType lightType) {
			int i = radius * 2 + 1;
			this.readyShape = new BitSetVoxelSet(i, i, i);
			this.shape = new BitSetVoxelSet(i, i, i);

			for (int j = 0; j < i; j++) {
				for (int k = 0; k < i; k++) {
					for (int l = 0; l < i; l++) {
						ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(
								sectionPos.getSectionX() + l - radius,
								sectionPos.getSectionY() + k - radius,
								sectionPos.getSectionZ() + j - radius
						);
						LightStorage.Status status = lightingProvider.getStatus(lightType, chunkSectionPos);
						if (status == LightStorage.Status.LIGHT_AND_DATA) {
							this.readyShape.set(l, k, j);
							this.shape.set(l, k, j);
						}
						else if (status == LightStorage.Status.LIGHT_ONLY) {
							this.shape.set(l, k, j);
						}
					}
				}
			}

			this.minSectionPos =
					ChunkSectionPos.from(
							sectionPos.getSectionX() - radius,
							sectionPos.getSectionY() - radius,
							sectionPos.getSectionZ() - radius
					);
		}
	}
}
