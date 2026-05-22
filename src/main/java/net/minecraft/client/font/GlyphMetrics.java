package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Метрики глифа: ширина продвижения курсора, смещение тени и жирного начертания.
 */
@Environment(EnvType.CLIENT)
public interface GlyphMetrics {

	float getAdvance();

	default float getAdvance(boolean bold) {
		return getAdvance() + (bold ? getBoldOffset() : 0.0F);
	}

	default float getBoldOffset() {
		return 1.0F;
	}

	default float getShadowOffset() {
		return 1.0F;
	}

	static GlyphMetrics empty(float advance) {
		return () -> advance;
	}
}
