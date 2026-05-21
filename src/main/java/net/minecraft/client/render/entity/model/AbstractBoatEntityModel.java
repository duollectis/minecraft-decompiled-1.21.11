package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.BoatEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code AbstractBoatEntityModel}.
 */
public abstract class AbstractBoatEntityModel extends EntityModel<BoatEntityRenderState> {

	private final ModelPart leftPaddle;
	private final ModelPart rightPaddle;

	public AbstractBoatEntityModel(ModelPart modelPart) {
		super(modelPart);
		this.leftPaddle = modelPart.getChild("left_paddle");
		this.rightPaddle = modelPart.getChild("right_paddle");
	}

	public void setAngles(BoatEntityRenderState boatEntityRenderState) {
		super.setAngles(boatEntityRenderState);
		setPaddleAngles(boatEntityRenderState.leftPaddleAngle, 0, this.leftPaddle);
		setPaddleAngles(boatEntityRenderState.rightPaddleAngle, 1, this.rightPaddle);
	}

	private static void setPaddleAngles(float angle, int paddle, ModelPart modelPart) {
		modelPart.pitch =
				MathHelper.clampedLerp(
						(MathHelper.sin(-angle) + 1.0F) / 2.0F,
						(float) (-Math.PI / 3),
						(float) (-Math.PI / 12)
				);
		modelPart.yaw =
				MathHelper.clampedLerp(
						(MathHelper.sin(-angle + 1.0F) + 1.0F) / 2.0F,
						(float) (-Math.PI / 4),
						(float) (Math.PI / 4)
				);
		if (paddle == 1) {
			modelPart.yaw = (float) Math.PI - modelPart.yaw;
		}
	}
}
