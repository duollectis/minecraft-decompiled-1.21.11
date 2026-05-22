package net.minecraft.client.render.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.util.math.*;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.TextGizmo;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code PathfindingDebugRenderer}.
 */
public class PathfindingDebugRenderer implements DebugRenderer.Renderer {

	private static final float RANGE = 80.0F;
	private static final int MAX_RENDER_DISTANCE = 8;
	private static final boolean SHOW_OPEN_NODES = false;
	private static final boolean SHOW_CLOSED_NODES = true;
	private static final boolean SHOW_PATH = false;
	private static final boolean SHOW_TARGET = false;
	private static final boolean SHOW_LABELS = true;
	private static final boolean SHOW_LINES = true;
	private static final float DRAWN_STRING_SIZE = 0.32F;

	@Override
	public void render(
			double cameraX,
			double cameraY,
			double cameraZ,
			DebugDataStore store,
			Frustum frustum,
			float tickProgress
	) {
		store.forEachEntityData(
				DebugSubscriptionTypes.ENTITY_PATHS,
				(entity, debugData) -> render(cameraX, cameraY, cameraZ, debugData.path(), debugData.maxNodeDistance())
		);
	}

	private static void render(double cameraX, double cameraY, double cameraZ, Path path, float maxNodeDistance) {
		drawPath(path, maxNodeDistance, true, true, cameraX, cameraY, cameraZ);
	}

	public static void drawPath(
			Path path,
			float maxNodeDistance,
			boolean bl,
			boolean bl2,
			double cameraX,
			double cameraY,
			double cameraZ
	) {
		drawPathLines(path, cameraX, cameraY, cameraZ);
		BlockPos blockPos = path.getTarget();
		if (getManhattanDistance(blockPos, cameraX, cameraY, cameraZ) <= RANGE) {
			GizmoDrawing.box(
					new Box(
							blockPos.getX() + 0.25F,
							blockPos.getY() + 0.25F,
							blockPos.getZ() + 0.25,
							blockPos.getX() + 0.75F,
							blockPos.getY() + 0.75F,
							blockPos.getZ() + 0.75F
					),
					DrawStyle.filled(ColorHelper.fromFloats(0.5F, 0.0F, 1.0F, 0.0F))
			);

			for (int i = 0; i < path.getLength(); i++) {
				PathNode pathNode = path.getNode(i);
				if (getManhattanDistance(pathNode.getBlockPos(), cameraX, cameraY, cameraZ) <= RANGE) {
					float f = i == path.getCurrentNodeIndex() ? 1.0F : 0.0F;
					float g = i == path.getCurrentNodeIndex() ? 0.0F : 1.0F;
					Box box = new Box(
							pathNode.x + 0.5F - maxNodeDistance,
							pathNode.y + 0.01F * i,
							pathNode.z + 0.5F - maxNodeDistance,
							pathNode.x + 0.5F + maxNodeDistance,
							pathNode.y + 0.25F + 0.01F * i,
							pathNode.z + 0.5F + maxNodeDistance
					);
					GizmoDrawing.box(box, DrawStyle.filled(ColorHelper.fromFloats(0.5F, f, 0.0F, g)));
				}
			}
		}

		Path.DebugNodeInfo debugNodeInfo = path.getDebugNodeInfos();
		if (bl && debugNodeInfo != null) {
			for (PathNode pathNode2 : debugNodeInfo.closedSet()) {
				if (getManhattanDistance(pathNode2.getBlockPos(), cameraX, cameraY, cameraZ) <= RANGE) {
					GizmoDrawing.box(
							new Box(
									pathNode2.x + 0.5F - maxNodeDistance / 2.0F,
									pathNode2.y + 0.01F,
									pathNode2.z + 0.5F - maxNodeDistance / 2.0F,
									pathNode2.x + 0.5F + maxNodeDistance / 2.0F,
									pathNode2.y + 0.1,
									pathNode2.z + 0.5F + maxNodeDistance / 2.0F
							),
							DrawStyle.filled(ColorHelper.fromFloats(0.5F, 1.0F, 0.8F, 0.8F))
					);
				}
			}

			for (PathNode pathNode2x : debugNodeInfo.openSet()) {
				if (getManhattanDistance(pathNode2x.getBlockPos(), cameraX, cameraY, cameraZ) <= RANGE) {
					GizmoDrawing.box(
							new Box(
									pathNode2x.x + 0.5F - maxNodeDistance / 2.0F,
									pathNode2x.y + 0.01F,
									pathNode2x.z + 0.5F - maxNodeDistance / 2.0F,
									pathNode2x.x + 0.5F + maxNodeDistance / 2.0F,
									pathNode2x.y + 0.1,
									pathNode2x.z + 0.5F + maxNodeDistance / 2.0F
							),
							DrawStyle.filled(ColorHelper.fromFloats(0.5F, 0.8F, 1.0F, 1.0F))
					);
				}
			}
		}

		if (bl2) {
			for (int j = 0; j < path.getLength(); j++) {
				PathNode pathNode3 = path.getNode(j);
				if (getManhattanDistance(pathNode3.getBlockPos(), cameraX, cameraY, cameraZ) <= RANGE) {
					GizmoDrawing.text(
							            String.valueOf(pathNode3.type),
							            new Vec3d(pathNode3.x + 0.5, pathNode3.y + 0.75, pathNode3.z + 0.5),
							            TextGizmo.Style.left().scaled(DRAWN_STRING_SIZE)
					            )
					            .ignoreOcclusion();
					GizmoDrawing.text(
							            String.format(Locale.ROOT, "%.2f", pathNode3.penalty),
							            new Vec3d(pathNode3.x + 0.5, pathNode3.y + 0.25, pathNode3.z + 0.5),
							            TextGizmo.Style.left().scaled(DRAWN_STRING_SIZE)
					            )
					            .ignoreOcclusion();
				}
			}
		}
	}

	/**
	 * Draw path lines.
	 *
	 * @param path path
	 * @param cameraX camera x
	 * @param cameraY camera y
	 * @param cameraZ camera z
	 */
	public static void drawPathLines(Path path, double cameraX, double cameraY, double cameraZ) {
		if (path.getLength() >= 2) {
			Vec3d vec3d = path.getNode(0).getPos();

			for (int i = 1; i < path.getLength(); i++) {
				PathNode pathNode = path.getNode(i);
				if (getManhattanDistance(pathNode.getBlockPos(), cameraX, cameraY, cameraZ) > RANGE) {
					vec3d = pathNode.getPos();
				}
				else {
					float f = (float) i / path.getLength() * 0.33F;
					int j = ColorHelper.fullAlpha(MathHelper.hsvToRgb(f, 0.9F, 0.9F));
					GizmoDrawing.arrow(vec3d.add(0.5, 0.5, 0.5), pathNode.getPos().add(0.5, 0.5, 0.5), j);
					vec3d = pathNode.getPos();
				}
			}
		}
	}

	private static float getManhattanDistance(BlockPos pos, double x, double y, double z) {
		return (float) (Math.abs(pos.getX() - x) + Math.abs(pos.getY() - y) + Math.abs(pos.getZ() - z));
	}
}
