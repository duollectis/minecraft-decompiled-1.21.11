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

/**
 * Визуализирует состояние освещения секций чанков вокруг игрока.
 * Секции с полными данными освещения ({@code LIGHT_AND_DATA}) отображаются ярким цветом,
 * секции только с данными освещения ({@code LIGHT_ONLY}) — приглушённым.
 * Данные обновляются не чаще одного раза в {@code UPDATE_INTERVAL}.
 */
@Environment(EnvType.CLIENT)
public class LightDebugRenderer implements DebugRenderer.Renderer {

	private static final Duration UPDATE_INTERVAL = Duration.ofMillis(500L);
	private static final int RADIUS = 10;
	// Размер секции в блоках
	private static final int SECTION_SIZE = 16;
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
		Instant now = Instant.now();

		if (data == null || Duration.between(lastUpdateTime, now).compareTo(UPDATE_INTERVAL) > 0) {
			lastUpdateTime = now;
			data = new LightDebugRenderer.Data(
					client.world.getLightingProvider(),
					ChunkSectionPos.from(client.player.getBlockPos()),
					RADIUS,
					lightType
			);
		}

		drawEdges(data.readyShape, data.minSectionPos, READY_SHAPE_COLOR);
		drawEdges(data.shape, data.minSectionPos, DEFAULT_SHAPE_COLOR);
		drawFaces(data.readyShape, data.minSectionPos, READY_SHAPE_COLOR);
		drawFaces(data.shape, data.minSectionPos, DEFAULT_SHAPE_COLOR);
	}

	private static void drawFaces(VoxelSet voxelSet, ChunkSectionPos sectionPos, int color) {
		voxelSet.forEachDirection((direction, localX, localY, localZ) -> {
			int sectionX = localX + sectionPos.getX();
			int sectionY = localY + sectionPos.getY();
			int sectionZ = localZ + sectionPos.getZ();
			drawFace(direction, sectionX, sectionY, sectionZ, color);
		});
	}

	private static void drawEdges(VoxelSet voxelSet, ChunkSectionPos sectionPos, int color) {
		voxelSet.forEachEdge(
				(x1, y1, z1, x2, y2, z2) -> {
					int sectionX1 = x1 + sectionPos.getX();
					int sectionY1 = y1 + sectionPos.getY();
					int sectionZ1 = z1 + sectionPos.getZ();
					int sectionX2 = x2 + sectionPos.getX();
					int sectionY2 = y2 + sectionPos.getY();
					int sectionZ2 = z2 + sectionPos.getZ();
					drawEdge(sectionX1, sectionY1, sectionZ1, sectionX2, sectionY2, sectionZ2, color);
				}, true
		);
	}

	private static void drawFace(Direction direction, int sectionX, int sectionY, int sectionZ, int color) {
		Vec3d minCorner = new Vec3d(
				ChunkSectionPos.getBlockCoord(sectionX),
				ChunkSectionPos.getBlockCoord(sectionY),
				ChunkSectionPos.getBlockCoord(sectionZ)
		);
		Vec3d maxCorner = minCorner.add(SECTION_SIZE, SECTION_SIZE, SECTION_SIZE);
		GizmoDrawing.face(minCorner, maxCorner, direction, DrawStyle.filled(color));
	}

	private static void drawEdge(int x1, int y1, int z1, int x2, int y2, int z2, int color) {
		Vec3d from = new Vec3d(
				ChunkSectionPos.getBlockCoord(x1),
				ChunkSectionPos.getBlockCoord(y1),
				ChunkSectionPos.getBlockCoord(z1)
		);
		Vec3d to = new Vec3d(
				ChunkSectionPos.getBlockCoord(x2),
				ChunkSectionPos.getBlockCoord(y2),
				ChunkSectionPos.getBlockCoord(z2)
		);
		GizmoDrawing.line(from, to, ColorHelper.fullAlpha(color));
	}

	/** Снимок состояния освещения секций в радиусе {@code RADIUS} от игрока. */
	@Environment(EnvType.CLIENT)
	static final class Data {

		final VoxelSet readyShape;
		final VoxelSet shape;
		final ChunkSectionPos minSectionPos;

		Data(LightingProvider lightingProvider, ChunkSectionPos sectionPos, int radius, LightType lightType) {
			int diameter = radius * 2 + 1;
			readyShape = new BitSetVoxelSet(diameter, diameter, diameter);
			shape = new BitSetVoxelSet(diameter, diameter, diameter);

			for (int z = 0; z < diameter; z++) {
				for (int y = 0; y < diameter; y++) {
					for (int x = 0; x < diameter; x++) {
						ChunkSectionPos pos = ChunkSectionPos.from(
								sectionPos.getSectionX() + x - radius,
								sectionPos.getSectionY() + y - radius,
								sectionPos.getSectionZ() + z - radius
						);
						LightStorage.Status status = lightingProvider.getStatus(lightType, pos);

						if (status == LightStorage.Status.LIGHT_AND_DATA) {
							readyShape.set(x, y, z);
							shape.set(x, y, z);
						}
						else if (status == LightStorage.Status.LIGHT_ONLY) {
							shape.set(x, y, z);
						}
					}
				}
			}

			minSectionPos = ChunkSectionPos.from(
					sectionPos.getSectionX() - radius,
					sectionPos.getSectionY() - radius,
					sectionPos.getSectionZ() - radius
			);
		}
	}
}
