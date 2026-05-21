package net.minecraft.client.option;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

@Environment(EnvType.CLIENT)
/**
 * {@code InactivityFpsLimit}.
 */
public enum InactivityFpsLimit implements StringIdentifiable {
	MINIMIZED("minimized", "options.inactivityFpsLimit.minimized"),
	AFK("afk", "options.inactivityFpsLimit.afk");

	public static final Codec<InactivityFpsLimit> CODEC = StringIdentifiable.createCodec(InactivityFpsLimit::values);
	private final String name;
	private final Text text;

	private InactivityFpsLimit(final String name, final String translationKey) {
		this.name = name;
		this.text = Text.translatable(translationKey);
	}

	public Text getText() {
		return this.text;
	}

	@Override
	public String asString() {
		return this.name;
	}
}
