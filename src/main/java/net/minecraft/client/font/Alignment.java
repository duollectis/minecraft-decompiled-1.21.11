package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.OrderedText;

@Environment(EnvType.CLIENT)
/**
 * {@code Alignment}.
 */
public enum Alignment {
	LEFT {
		@Override
		public int getAdjustedX(int x, int width) {
			return x;
		}

		@Override
		public int getAdjustedX(int x, TextRenderer textRenderer, OrderedText text) {
			return x;
		}
	},
	CENTER {
		@Override
		public int getAdjustedX(int x, int width) {
			return x - width / 2;
		}
	},
	RIGHT {
		@Override
		public int getAdjustedX(int x, int width) {
			return x - width;
		}
	};

	public abstract int getAdjustedX(int x, int width);

	public int getAdjustedX(int x, TextRenderer textRenderer, OrderedText text) {
		return this.getAdjustedX(x, textRenderer.getWidth(text));
	}
}
