package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;

@Environment(EnvType.CLIENT)
/**
 * {@code PiglinHeadEntityModel}.
 */
public class PiglinHeadEntityModel extends SkullBlockEntityModel {

	private final ModelPart head;
	private final ModelPart leftEar;
	private final ModelPart rightEar;

	public PiglinHeadEntityModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.leftEar = this.head.getChild("left_ear");
		this.rightEar = this.head.getChild("right_ear");
	}

	public static ModelData getModelData() {
		ModelData modelData = new ModelData();
		PiglinEntityModel.getModelPartData(Dilation.NONE, modelData);
		return modelData;
	}

	public void setAngles(SkullBlockEntityModel.SkullModelState skullModelState) {
		super.setAngles(skullModelState);
		this.head.yaw = skullModelState.yaw * (float) (Math.PI / 180.0);
		this.head.pitch = skullModelState.pitch * (float) (Math.PI / 180.0);
		float f = 1.2F;
		this.leftEar.roll =
				(float) (-(Math.cos(skullModelState.poweredTicks * (float) Math.PI * 0.2F * 1.2F) + 2.5)) * 0.2F;
		this.rightEar.roll = (float) (Math.cos(skullModelState.poweredTicks * (float) Math.PI * 0.2F) + 2.5) * 0.2F;
	}
}
