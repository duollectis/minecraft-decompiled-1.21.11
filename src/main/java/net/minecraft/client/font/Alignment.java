package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.OrderedText;

/**
 * Выравнивание текста по горизонтали.
 * Определяет, как смещается начальная X-координата рендеринга
 * относительно переданной точки привязки.
 */
@Environment(EnvType.CLIENT)
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

	/**
	 * Вычисляет скорректированную X-координату начала рендеринга текста
	 * заданной ширины относительно точки привязки {@code x}.
	 *
	 * @param x     точка привязки по оси X
	 * @param width ширина текста в пикселях
	 * @return скорректированная X-координата начала рендеринга
	 */
	public abstract int getAdjustedX(int x, int width);

	/**
	 * Вычисляет скорректированную X-координату, измеряя ширину текста через {@code textRenderer}.
	 *
	 * @param x            точка привязки по оси X
	 * @param textRenderer рендерер для измерения ширины текста
	 * @param text         текст, ширина которого измеряется
	 * @return скорректированная X-координата начала рендеринга
	 */
	public int getAdjustedX(int x, TextRenderer textRenderer, OrderedText text) {
		return getAdjustedX(x, textRenderer.getWidth(text));
	}
}
