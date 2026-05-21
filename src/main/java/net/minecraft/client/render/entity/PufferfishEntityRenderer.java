package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PufferfishEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.PufferfishEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code PufferfishEntityRenderer}.
 */
public class PufferfishEntityRenderer extends MobEntityRenderer<PufferfishEntity, PufferfishEntityRenderState, EntityModel<EntityRenderState>> {

	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/fish/pufferfish.png");
	private final EntityModel<EntityRenderState> smallModel;
	private final EntityModel<EntityRenderState> mediumModel;
	private final EntityModel<EntityRenderState> largeModel = this.getModel();

	public PufferfishEntityRenderer(EntityRendererFactory.Context context) {
		super(context, new LargePufferfishEntityModel(context.getPart(EntityModelLayers.PUFFERFISH_BIG)), 0.2F);
		this.mediumModel = new MediumPufferfishEntityModel(context.getPart(EntityModelLayers.PUFFERFISH_MEDIUM));
		this.smallModel = new SmallPufferfishEntityModel(context.getPart(EntityModelLayers.PUFFERFISH_SMALL));
	}

	public Identifier getTexture(PufferfishEntityRenderState pufferfishEntityRenderState) {
		return TEXTURE;
	}

	/**
	 * Создаёт render state.
	 *
	 * @return PufferfishEntityRenderState — результат операции
	 */
	public PufferfishEntityRenderState createRenderState() {
		return new PufferfishEntityRenderState();
	}

	protected float getShadowRadius(PufferfishEntityRenderState pufferfishEntityRenderState) {
		return 0.1F + 0.1F * pufferfishEntityRenderState.puffState;
	}

	public void render(
			PufferfishEntityRenderState pufferfishEntityRenderState,
			MatrixStack matrixStack,
			OrderedRenderCommandQueue orderedRenderCommandQueue,
			CameraRenderState cameraRenderState
	) {
		this.model = switch (pufferfishEntityRenderState.puffState) {
			case 0 -> this.smallModel;
			case 1 -> this.mediumModel;
			default -> this.largeModel;
		};
		super.render(pufferfishEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
	}

	public void updateRenderState(
			PufferfishEntity pufferfishEntity,
			PufferfishEntityRenderState pufferfishEntityRenderState,
			float f
	) {
		super.updateRenderState(pufferfishEntity, pufferfishEntityRenderState, f);
		pufferfishEntityRenderState.puffState = pufferfishEntity.getPuffState();
	}

	protected void setupTransforms(
			PufferfishEntityRenderState pufferfishEntityRenderState,
			MatrixStack matrixStack,
			float f,
			float g
	) {
		matrixStack.translate(0.0F, MathHelper.cos(pufferfishEntityRenderState.age * 0.05F) * 0.08F, 0.0F);
		super.setupTransforms(pufferfishEntityRenderState, matrixStack, f, g);
	}
}
