package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.render.entity.state.WitherSkullEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
/**
 * {@code WitherSkullEntityRenderer}.
 */
public class WitherSkullEntityRenderer extends EntityRenderer<WitherSkullEntity, WitherSkullEntityRenderState> {

	private static final Identifier
			INVULNERABLE_TEXTURE =
			Identifier.ofVanilla("textures/entity/wither/wither_invulnerable.png");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/wither/wither.png");
	private final SkullEntityModel model;

	public WitherSkullEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.model = new SkullEntityModel(context.getPart(EntityModelLayers.WITHER_SKULL));
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"head",
				ModelPartBuilder.create().uv(0, 35).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 64, 64);
	}

	protected int getBlockLight(WitherSkullEntity witherSkullEntity, BlockPos blockPos) {
		return 15;
	}

	public void render(
			WitherSkullEntityRenderState witherSkullEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		matrixStack.push();
		matrixStack.scale(-1.0F, -1.0F, 1.0F);
		orderedRenderCommandQueue.submitModel(
				this.model,
				witherSkullEntityRenderState.skullState,
				matrixStack,
				this.model.getLayer(this.getTexture(witherSkullEntityRenderState)),
				witherSkullEntityRenderState.light,
				OverlayTexture.DEFAULT_UV,
				witherSkullEntityRenderState.outlineColor,
				null
		);
		matrixStack.pop();
		super.render(witherSkullEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
	}

	private Identifier getTexture(WitherSkullEntityRenderState state) {
		return state.charged ? INVULNERABLE_TEXTURE : TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return WitherSkullEntityRenderState — результат операции
	 */
	public WitherSkullEntityRenderState createRenderState() {
		return new WitherSkullEntityRenderState();
	}

	public void updateRenderState(
			WitherSkullEntity witherSkullEntity,
			WitherSkullEntityRenderState witherSkullEntityRenderState,
			float f
	) {
		super.updateRenderState(witherSkullEntity, witherSkullEntityRenderState, f);
		witherSkullEntityRenderState.charged = witherSkullEntity.isCharged();
		witherSkullEntityRenderState.skullState.poweredTicks = 0.0F;
		witherSkullEntityRenderState.skullState.yaw = witherSkullEntity.getLerpedYaw(f);
		witherSkullEntityRenderState.skullState.pitch = witherSkullEntity.getLerpedPitch(f);
	}
}
