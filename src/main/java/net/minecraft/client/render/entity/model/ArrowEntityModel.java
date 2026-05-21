package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.ProjectileEntityRenderState;
import net.minecraft.util.math.MathHelper;

import java.util.function.UnaryOperator;

@Environment(EnvType.CLIENT)
/**
 * {@code ArrowEntityModel}.
 */
public class ArrowEntityModel extends EntityModel<ProjectileEntityRenderState> {

	public ArrowEntityModel(ModelPart modelPart) {
		super(modelPart, RenderLayers::entityCutout);
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		modelPartData.addChild(
				"back",
				ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F),
				ModelTransform.of(-11.0F, 0.0F, 0.0F, (float) (Math.PI / 4), 0.0F, 0.0F).withScale(0.8F)
		);
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder
						.create()
						.uv(0, 0)
						.cuboid(-12.0F, -2.0F, 0.0F, 16.0F, 4.0F, 0.0F, Dilation.NONE, 1.0F, 0.8F);
		modelPartData.addChild("cross_1", modelPartBuilder, ModelTransform.rotation((float) (Math.PI / 4), 0.0F, 0.0F));
		modelPartData.addChild(
				"cross_2",
				modelPartBuilder,
				ModelTransform.rotation((float) (Math.PI * 3.0 / 4.0), 0.0F, 0.0F)
		);
		return TexturedModelData.of(
				modelData.transform((UnaryOperator<ModelTransform>) modelTransform -> modelTransform.scaled(0.9F)),
				32,
				32
		);
	}

	public void setAngles(ProjectileEntityRenderState projectileEntityRenderState) {
		super.setAngles(projectileEntityRenderState);
		if (projectileEntityRenderState.shake > 0.0F) {
			float f = -MathHelper.sin(projectileEntityRenderState.shake * 3.0F) * projectileEntityRenderState.shake;
			this.root.roll += f * (float) (Math.PI / 180.0);
		}
	}
}
