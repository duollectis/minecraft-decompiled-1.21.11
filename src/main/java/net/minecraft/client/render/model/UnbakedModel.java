package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code UnbakedModel}.
 */
public interface UnbakedModel {

	String PARTICLE_TEXTURE = "particle";

	default @Nullable Boolean ambientOcclusion() {
		return null;
	}

	default UnbakedModel.@Nullable GuiLight guiLight() {
		return null;
	}

	default @Nullable ModelTransformation transformations() {
		return null;
	}

	default ModelTextures.Textures textures() {
		return ModelTextures.Textures.EMPTY;
	}

	default @Nullable Geometry geometry() {
		return null;
	}

	default @Nullable Identifier parent() {
		return null;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code GuiLight}.
	 */
	public static enum GuiLight {
		ITEM("front"),
		BLOCK("side");

		private final String name;

		private GuiLight(final String name) {
			this.name = name;
		}

		public static UnbakedModel.GuiLight byName(String value) {
			for (UnbakedModel.GuiLight guiLight : values()) {
				if (guiLight.name.equals(value)) {
					return guiLight;
				}
			}

			throw new IllegalArgumentException("Invalid gui light: " + value);
		}

		public boolean isSide() {
			return this == BLOCK;
		}
	}
}
