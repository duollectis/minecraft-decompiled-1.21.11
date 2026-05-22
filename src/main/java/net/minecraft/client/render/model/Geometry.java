package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Функциональный интерфейс геометрии незапечённой модели.
 * Принимает контекст запекания и возвращает готовую {@link BakedGeometry}.
 * Пустая реализация {@link #EMPTY} используется для моделей без геометрии.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface Geometry {

	Geometry EMPTY = (textures, baker, settings, model) -> BakedGeometry.EMPTY;

	BakedGeometry bake(ModelTextures textures, Baker baker, ModelBakeSettings settings, SimpleModel model);
}
