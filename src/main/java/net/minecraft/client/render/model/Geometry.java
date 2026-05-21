package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
/**
 * {@code Geometry}.
 */
public interface Geometry {

	Geometry EMPTY = (textures, baker, settings, model) -> BakedGeometry.EMPTY;

	BakedGeometry bake(ModelTextures textures, Baker baker, ModelBakeSettings settings, SimpleModel model);
}
