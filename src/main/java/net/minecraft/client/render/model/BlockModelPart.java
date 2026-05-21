package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockModelPart;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockModelPart}.
 */
public interface BlockModelPart extends FabricBlockModelPart {

	List<BakedQuad> getQuads(@Nullable Direction side);

	boolean useAmbientOcclusion();

	Sprite particleSprite();

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Unbaked}.
	 */
	public interface Unbaked extends ResolvableModel {

		BlockModelPart bake(Baker baker);
	}
}
