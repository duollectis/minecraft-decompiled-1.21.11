package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code FeatureRendererContext}.
 */
public interface FeatureRendererContext<S extends EntityRenderState, M extends EntityModel<? super S>> {

	M getModel();
}
