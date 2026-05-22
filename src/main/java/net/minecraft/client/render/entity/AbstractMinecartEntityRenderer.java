package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.MinecartEntityModel;
import net.minecraft.client.render.entity.state.MinecartEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.DefaultMinecartController;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

/**
 * Базовый рендерер вагонетки. Обрабатывает два режима контроллера:
 * экспериментальный (lerp по позиции/углам) и стандартный (snap-to-rail с симуляцией движения).
 * Также рендерит содержимое блока внутри вагонетки и применяет wobble-анимацию при уроне.
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractMinecartEntityRenderer<T extends AbstractMinecartEntity, S extends MinecartEntityRenderState>
		extends EntityRenderer<T, S> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/minecart.png");
	private static final float CART_SCALE = 0.75F;
	private static final float WOBBLE_DIVISOR = 10.0F;

	protected final MinecartEntityModel model;

	public AbstractMinecartEntityRenderer(EntityRendererFactory.Context ctx, EntityModelLayer layer) {
		super(ctx);
		this.shadowRadius = 0.7F;
		this.model = new MinecartEntityModel(ctx.getPart(layer));
	}

	@Override
	public void render(
			S minecartEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		super.render(minecartEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
		matrixStack.push();

		long hash = minecartEntityRenderState.hash;
		float offsetX = (((float) (hash >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
		float offsetY = (((float) (hash >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
		float offsetZ = (((float) (hash >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
		matrixStack.translate(offsetX, offsetY, offsetZ);

		if (minecartEntityRenderState.usesExperimentalController) {
			transformExperimentalControllerMinecart(minecartEntityRenderState, matrixStack);
		} else {
			transformDefaultControllerMinecart(minecartEntityRenderState, matrixStack);
		}

		float wobbleTicks = minecartEntityRenderState.damageWobbleTicks;
		if (wobbleTicks > 0.0F) {
			matrixStack.multiply(
					RotationAxis.POSITIVE_X.rotationDegrees(
							MathHelper.sin(wobbleTicks) * wobbleTicks * minecartEntityRenderState.damageWobbleStrength
									/ WOBBLE_DIVISOR * minecartEntityRenderState.damageWobbleSide
					)
			);
		}

		BlockState containedBlock = minecartEntityRenderState.containedBlock;
		if (containedBlock.getRenderType() != BlockRenderType.INVISIBLE) {
			matrixStack.push();
			matrixStack.scale(CART_SCALE, CART_SCALE, CART_SCALE);
			matrixStack.translate(-0.5F, (minecartEntityRenderState.blockOffset - 8) / 16.0F, 0.5F);
			matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
			this.renderBlock(
					minecartEntityRenderState,
					containedBlock,
					matrixStack,
					orderedRenderCommandQueue,
					minecartEntityRenderState.light
			);
			matrixStack.pop();
		}

		matrixStack.scale(-1.0F, -1.0F, 1.0F);
		orderedRenderCommandQueue.submitModel(
				this.model,
				minecartEntityRenderState,
				matrixStack,
				this.model.getLayer(TEXTURE),
				minecartEntityRenderState.light,
				OverlayTexture.DEFAULT_UV,
				minecartEntityRenderState.outlineColor,
				null
		);
		matrixStack.pop();
	}

	private static <S extends MinecartEntityRenderState> void transformExperimentalControllerMinecart(
			S state,
			MatrixStack matrices
	) {
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.lerpedYaw));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-state.lerpedPitch));
		matrices.translate(0.0F, 0.375F, 0.0F);
	}

	/**
	 * Применяет трансформации для стандартного контроллера: вычисляет угол наклона
	 * по двум соседним точкам рельса (futurePos и pastPos) для плавного поворота вагонетки.
	 */
	private static <S extends MinecartEntityRenderState> void transformDefaultControllerMinecart(
			S state,
			MatrixStack matrices
	) {
		float pitch = state.lerpedPitch;
		float yaw = state.lerpedYaw;

		if (state.presentPos != null && state.futurePos != null && state.pastPos != null) {
			Vec3d futurePos = state.futurePos;
			Vec3d pastPos = state.pastPos;
			matrices.translate(state.presentPos.x - state.x, (futurePos.y + pastPos.y) / 2.0 - state.y, state.presentPos.z - state.z);

			Vec3d railDirection = pastPos.add(-futurePos.x, -futurePos.y, -futurePos.z);
			if (railDirection.length() != 0.0) {
				railDirection = railDirection.normalize();
				yaw = (float) (Math.atan2(railDirection.z, railDirection.x) * 180.0 / Math.PI);
				pitch = (float) (Math.atan(railDirection.y) * 73.0);
			}
		}

		matrices.translate(0.0F, 0.375F, 0.0F);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-pitch));
	}

	@Override
	public void updateRenderState(T abstractMinecartEntity, S minecartEntityRenderState, float tickProgress) {
		super.updateRenderState(abstractMinecartEntity, minecartEntityRenderState, tickProgress);

		if (abstractMinecartEntity.getController() instanceof ExperimentalMinecartController experimentalController) {
			updateFromExperimentalController(abstractMinecartEntity, experimentalController, minecartEntityRenderState, tickProgress);
			minecartEntityRenderState.usesExperimentalController = true;
		} else if (abstractMinecartEntity.getController() instanceof DefaultMinecartController defaultController) {
			updateFromDefaultController(abstractMinecartEntity, defaultController, minecartEntityRenderState, tickProgress);
			minecartEntityRenderState.usesExperimentalController = false;
		}

		long entityId = abstractMinecartEntity.getId() * 493286711L;
		minecartEntityRenderState.hash = entityId * entityId * 4392167121L + entityId * 98761L;
		minecartEntityRenderState.damageWobbleTicks = abstractMinecartEntity.getDamageWobbleTicks() - tickProgress;
		minecartEntityRenderState.damageWobbleSide = abstractMinecartEntity.getDamageWobbleSide();
		minecartEntityRenderState.damageWobbleStrength = Math.max(abstractMinecartEntity.getDamageWobbleStrength() - tickProgress, 0.0F);
		minecartEntityRenderState.blockOffset = abstractMinecartEntity.getBlockOffset();
		minecartEntityRenderState.containedBlock = abstractMinecartEntity.getContainedBlock();
	}

	private static <T extends AbstractMinecartEntity, S extends MinecartEntityRenderState> void updateFromExperimentalController(
			T minecart,
			ExperimentalMinecartController controller,
			S state,
			float tickProgress
	) {
		if (controller.hasCurrentLerpSteps()) {
			state.lerpedPos = controller.getLerpedPosition(tickProgress);
			state.lerpedPitch = controller.getLerpedPitch(tickProgress);
			state.lerpedYaw = controller.getLerpedYaw(tickProgress);
		} else {
			state.lerpedPos = null;
			state.lerpedPitch = minecart.getPitch();
			state.lerpedYaw = minecart.getYaw();
		}
	}

	private static <T extends AbstractMinecartEntity, S extends MinecartEntityRenderState> void updateFromDefaultController(
			T minecart,
			DefaultMinecartController controller,
			S state,
			float tickProgress
	) {
		state.lerpedPitch = minecart.getLerpedPitch(tickProgress);
		state.lerpedYaw = minecart.getLerpedYaw(tickProgress);

		Vec3d currentPos = controller.snapPositionToRail(state.x, state.y, state.z);
		if (currentPos != null) {
			state.presentPos = currentPos;
			state.futurePos = Objects.requireNonNullElse(controller.simulateMovement(state.x, state.y, state.z, 0.3F), currentPos);
			state.pastPos = Objects.requireNonNullElse(controller.simulateMovement(state.x, state.y, state.z, -0.3F), currentPos);
		} else {
			state.presentPos = null;
			state.futurePos = null;
			state.pastPos = null;
		}
	}

	protected void renderBlock(
			S state,
			BlockState blockState,
			MatrixStack matrices,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			int light
	) {
		orderedRenderCommandQueue.submitBlock(
				matrices,
				blockState,
				light,
				OverlayTexture.DEFAULT_UV,
				state.outlineColor
		);
	}

	@Override
	protected Box getBoundingBox(T abstractMinecartEntity) {
		Box box = super.getBoundingBox(abstractMinecartEntity);
		return !abstractMinecartEntity.getContainedBlock().isAir()
				? box.stretch(0.0, abstractMinecartEntity.getBlockOffset() * CART_SCALE / 16.0F, 0.0)
				: box;
	}

	@Override
	public Vec3d getPositionOffset(S minecartEntityRenderState) {
		Vec3d baseOffset = super.getPositionOffset(minecartEntityRenderState);
		return minecartEntityRenderState.usesExperimentalController && minecartEntityRenderState.lerpedPos != null
				? baseOffset.add(
						minecartEntityRenderState.lerpedPos.x - minecartEntityRenderState.x,
						minecartEntityRenderState.lerpedPos.y - minecartEntityRenderState.y,
						minecartEntityRenderState.lerpedPos.z - minecartEntityRenderState.z
				)
				: baseOffset;
	}
}
