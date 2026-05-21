package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
/**
 * {@code EndCrystalEntityModel}.
 */
public class EndCrystalEntityModel extends EntityModel<EndCrystalEntityRenderState> {

	private static final String OUTER_GLASS = "outer_glass";
	private static final String INNER_GLASS = "inner_glass";
	private static final String BASE = "base";
	private static final float SIN_45 = (float) Math.sin(Math.PI / 4);
	public final ModelPart base;
	public final ModelPart outerGlass;
	public final ModelPart innerGlass;
	public final ModelPart cube;

	public EndCrystalEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.base = modelPart.getChild("base");
		this.outerGlass = modelPart.getChild("outer_glass");
		this.innerGlass = this.outerGlass.getChild("inner_glass");
		this.cube = this.innerGlass.getChild("cube");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		float f = 0.875F;
		ModelPartBuilder
				modelPartBuilder =
				ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F);
		ModelPartData
				modelPartData2 =
				modelPartData.addChild("outer_glass", modelPartBuilder, ModelTransform.origin(0.0F, 24.0F, 0.0F));
		ModelPartData
				modelPartData3 =
				modelPartData2.addChild("inner_glass", modelPartBuilder, ModelTransform.NONE.withScale(0.875F));
		modelPartData3.addChild(
				"cube",
				ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				ModelTransform.NONE.withScale(0.765625F)
		);
		modelPartData.addChild(
				"base",
				ModelPartBuilder.create().uv(0, 16).cuboid(-6.0F, 0.0F, -6.0F, 12.0F, 4.0F, 12.0F),
				ModelTransform.NONE
		);
		return TexturedModelData.of(modelData, 64, 32);
	}

	public void setAngles(EndCrystalEntityRenderState endCrystalEntityRenderState) {
		super.setAngles(endCrystalEntityRenderState);
		this.base.visible = endCrystalEntityRenderState.baseVisible;
		float f = endCrystalEntityRenderState.age * 3.0F;
		float g = EndCrystalEntityRenderer.getYOffset(endCrystalEntityRenderState.age) * 16.0F;
		this.outerGlass.originY += g / 2.0F;
		this.outerGlass.rotate(RotationAxis.POSITIVE_Y
				.rotationDegrees(f)
				.rotateAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45));
		this.innerGlass.rotate(new Quaternionf()
				.setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45)
				.rotateY(f * (float) (Math.PI / 180.0)));
		this.cube.rotate(new Quaternionf()
				.setAngleAxis((float) (Math.PI / 3), SIN_45, 0.0F, SIN_45)
				.rotateY(f * (float) (Math.PI / 180.0)));
	}
}
