package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemDisplayContext;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelSettings}.
 */
public record ModelSettings(boolean usesBlockLight, Sprite particleIcon, ModelTransformation transforms) {

	/**
	 * Resolve settings.
	 *
	 * @param baker baker
	 * @param model model
	 * @param textures textures
	 *
	 * @return ModelSettings — результат операции
	 */
	public static ModelSettings resolveSettings(Baker baker, BakedSimpleModel model, ModelTextures textures) {
		Sprite sprite = model.getParticleTexture(textures, baker);
		return new ModelSettings(model.getGuiLight().isSide(), sprite, model.getTransformations());
	}

	/**
	 * Добавляет settings.
	 *
	 * @param state state
	 * @param mode mode
	 */
	public void addSettings(ItemRenderState.LayerRenderState state, ItemDisplayContext mode) {
		state.setUseLight(this.usesBlockLight);
		state.setParticle(this.particleIcon);
		state.setTransform(this.transforms.getTransformation(mode));
	}
}
