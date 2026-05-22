package net.minecraft.client.font;

import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Глиф, готовый к загрузке на GPU-текстуру атласа.
 * Предоставляет размеры, смещения и метод {@link #upload} для записи пикселей в текстуру.
 * Значение {@link #DEFAULT_ASCENT} соответствует базовой линии шрифта высотой 8px (7 пикселей над ней).
 */
@Environment(EnvType.CLIENT)
public interface UploadableGlyph {

	float DEFAULT_ASCENT = 7.0F;

	int getWidth();

	int getHeight();

	void upload(int x, int y, GpuTexture texture);

	boolean hasColor();

	float getOversample();

	default float getXMin() {
		return getBearingX();
	}

	default float getXMax() {
		return getXMin() + getWidth() / getOversample();
	}

	default float getYMin() {
		return DEFAULT_ASCENT - getAscent();
	}

	default float getYMax() {
		return getYMin() + getHeight() / getOversample();
	}

	default float getBearingX() {
		return 0.0F;
	}

	default float getAscent() {
		return DEFAULT_ASCENT;
	}
}
