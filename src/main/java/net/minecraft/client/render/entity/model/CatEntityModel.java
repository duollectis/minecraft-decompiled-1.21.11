package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.CatEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code CatEntityModel}.
 */
public class CatEntityModel extends FelineEntityModel<CatEntityRenderState> {

	public static final ModelTransformer CAT_TRANSFORMER = ModelTransformer.scaling(0.8F);

	public CatEntityModel(ModelPart modelPart) {
		super(modelPart);
	}
}
