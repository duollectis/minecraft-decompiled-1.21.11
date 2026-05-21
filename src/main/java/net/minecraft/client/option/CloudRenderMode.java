package net.minecraft.client.option;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

@Environment(EnvType.CLIENT)
/**
 * {@code CloudRenderMode}.
 */
public enum CloudRenderMode implements StringIdentifiable {
	OFF("false", "options.off"),
	FAST("fast", "options.clouds.fast"),
	FANCY("true", "options.clouds.fancy");

	public static final Codec<CloudRenderMode> CODEC = StringIdentifiable.createCodec(CloudRenderMode::values);
	private final String serializedId;
	private final Text text;

	private CloudRenderMode(final String serializedId, final String translationKey) {
		this.serializedId = serializedId;
		this.text = Text.translatable(translationKey);
	}

	public Text getText() {
		return this.text;
	}

	@Override
	public String asString() {
		return this.serializedId;
	}
}
