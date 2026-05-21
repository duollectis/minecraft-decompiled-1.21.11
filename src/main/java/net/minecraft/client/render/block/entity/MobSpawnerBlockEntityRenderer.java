package net.minecraft.client.render.block.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.client.render.block.entity.state.MobSpawnerBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code MobSpawnerBlockEntityRenderer}.
 */
public class MobSpawnerBlockEntityRenderer implements BlockEntityRenderer<MobSpawnerBlockEntity, MobSpawnerBlockEntityRenderState> {

	private final EntityRenderManager entityRenderDispatcher;

	public MobSpawnerBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
		this.entityRenderDispatcher = ctx.entityRenderDispatcher();
	}

	public MobSpawnerBlockEntityRenderState createRenderState() {
		return new MobSpawnerBlockEntityRenderState();
	}

	public void updateRenderState(
			MobSpawnerBlockEntity mobSpawnerBlockEntity,
			MobSpawnerBlockEntityRenderState mobSpawnerBlockEntityRenderState,
			float f,
			Vec3d vec3d,
			ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumblingOverlayCommand
	) {
		BlockEntityRenderer.super.updateRenderState(
				mobSpawnerBlockEntity,
				mobSpawnerBlockEntityRenderState,
				f,
				vec3d,
				crumblingOverlayCommand
		);
		if (mobSpawnerBlockEntity.getWorld() != null) {
			MobSpawnerLogic mobSpawnerLogic = mobSpawnerBlockEntity.getLogic();
			Entity
					entity =
					mobSpawnerLogic.getRenderedEntity(mobSpawnerBlockEntity.getWorld(), mobSpawnerBlockEntity.getPos());
			TrialSpawnerBlockEntityRenderer.updateSpawnerRenderState(
					mobSpawnerBlockEntityRenderState,
					f,
					entity,
					this.entityRenderDispatcher,
					mobSpawnerLogic.getLastRotation(),
					mobSpawnerLogic.getRotation()
			);
		}
	}

	public void render(
			MobSpawnerBlockEntityRenderState mobSpawnerBlockEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		if (mobSpawnerBlockEntityRenderState.displayEntityRenderState != null) {
			renderDisplayEntity(
					matrixStack,
					orderedRenderCommandQueue,
					mobSpawnerBlockEntityRenderState.displayEntityRenderState,
					this.entityRenderDispatcher,
					mobSpawnerBlockEntityRenderState.displayEntityRotation,
					mobSpawnerBlockEntityRenderState.displayEntityScale,
					cameraRenderState
			);
		}
	}

	public static void renderDisplayEntity(
			MatrixStack matrices,
			OrderedRenderCommandQueue queue,
			EntityRenderState state,
			EntityRenderManager entityRenderDispatcher,
			float rotation,
			float scale,
			CameraRenderState cameraRenderState
	) {
		matrices.push();
		matrices.translate(0.5F, 0.4F, 0.5F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
		matrices.translate(0.0F, -0.2F, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
		matrices.scale(scale, scale, scale);
		entityRenderDispatcher.render(state, cameraRenderState, 0.0, 0.0, 0.0, matrices, queue);
		matrices.pop();
	}
}
