package net.minecraft.client.render.debug;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;

@Environment(EnvType.CLIENT)
/**
 * {@code SupportingBlockDebugRenderer}.
 */
public class SupportingBlockDebugRenderer implements DebugRenderer.Renderer {

	private final MinecraftClient client;
	private double lastEntityCheckTime = Double.MIN_VALUE;
	private List<Entity> entities = Collections.emptyList();

	public SupportingBlockDebugRenderer(MinecraftClient client) {
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
		double d = Util.getMeasuringTimeNano();
		if (d - this.lastEntityCheckTime > 1.0E8) {
			this.lastEntityCheckTime = d;
			Entity entity = this.client.gameRenderer.getCamera().getFocusedEntity();
			this.entities =
					ImmutableList.copyOf(entity
							.getEntityWorld()
							.getOtherEntities(entity, entity.getBoundingBox().expand(16.0)));
		}

		PlayerEntity playerEntity = this.client.player;
		if (playerEntity != null && playerEntity.supportingBlockPos.isPresent()) {
			this.renderBlockHighlights(playerEntity, () -> 0.0, -65536);
		}

		for (Entity entity2 : this.entities) {
			if (entity2 != playerEntity) {
				this.renderBlockHighlights(entity2, () -> this.getAdditionalDilation(entity2), -16711936);
			}
		}
	}

	private void renderBlockHighlights(Entity entity, DoubleSupplier doubleSupplier, int i) {
		entity.supportingBlockPos.ifPresent(blockPos -> {
			double d = doubleSupplier.getAsDouble();
			BlockPos blockPos2 = entity.getSteppingPos();
			this.renderBlockHighlight(blockPos2, 0.02 + d, i);
			BlockPos blockPos3 = entity.getLandingPos();
			if (!blockPos3.equals(blockPos2)) {
				this.renderBlockHighlight(blockPos3, 0.04 + d, -16711681);
			}
		});
	}

	private double getAdditionalDilation(Entity entity) {
		return 0.02 * (String.valueOf(entity.getId() + 0.132453657).hashCode() % 1000) / 1000.0;
	}

	private void renderBlockHighlight(BlockPos pos, double d, int i) {
		double e = pos.getX() - 2.0 * d;
		double f = pos.getY() - 2.0 * d;
		double g = pos.getZ() - 2.0 * d;
		double h = e + 1.0 + 4.0 * d;
		double j = f + 1.0 + 4.0 * d;
		double k = g + 1.0 + 4.0 * d;
		GizmoDrawing.box(new Box(e, f, g, h, j, k), DrawStyle.stroked(ColorHelper.withAlpha(0.4F, i)));
		VoxelShape
				voxelShape =
				this.client.world
						.getBlockState(pos)
						.getCollisionShape(this.client.world, pos, ShapeContext.absent())
						.offset(pos);
		DrawStyle drawStyle = DrawStyle.stroked(i);

		for (Box box : voxelShape.getBoundingBoxes()) {
			GizmoDrawing.box(box, drawStyle);
		}
	}
}
