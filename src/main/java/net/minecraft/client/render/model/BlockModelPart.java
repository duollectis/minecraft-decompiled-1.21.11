package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockModelPart;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Запечённая часть блочной модели: предоставляет квады по направлению грани,
 * флаг ambient occlusion и спрайт частиц при разрушении блока.
 */
@Environment(EnvType.CLIENT)
public interface BlockModelPart extends FabricBlockModelPart {

	List<BakedQuad> getQuads(@Nullable Direction side);

	boolean useAmbientOcclusion();

	Sprite particleSprite();

	/**
	 * Незапечённая часть блочной модели, способная разрешать зависимости
	 * и запекаться в {@link BlockModelPart} через {@link Baker}.
	 */
	@Environment(EnvType.CLIENT)
	interface Unbaked extends ResolvableModel {

		BlockModelPart bake(Baker baker);
	}
}
