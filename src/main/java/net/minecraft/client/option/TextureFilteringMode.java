package net.minecraft.client.option;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;

import java.util.function.IntFunction;

@Environment(EnvType.CLIENT)
/**
 * {@code TextureFilteringMode}.
 */
public enum TextureFilteringMode {
	NONE(0, "options.textureFiltering.none"),
	RGSS(1, "options.textureFiltering.rgss"),
	ANISOTROPIC(2, "options.textureFiltering.anisotropic");

	private static final IntFunction<TextureFilteringMode> BY_ID = ValueLists.createIndexToValueFunction(
			(TextureFilteringMode mode) -> mode.id, values(), ValueLists.OutOfBoundsHandling.WRAP
	);
	public static final Codec<TextureFilteringMode> CODEC = Codec.INT.xmap(BY_ID::apply, mode -> mode.id);
	private final int id;
	private final Text text;

	private TextureFilteringMode(final int id, final String translationKey) {
		this.id = id;
		this.text = Text.translatable(translationKey);
	}

	public Text getText() {
		return this.text;
	}
}
