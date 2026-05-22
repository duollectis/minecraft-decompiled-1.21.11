package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;

/**
 * Предустановленный профиль отладочного HUD (набор видимых записей).
 */
@Environment(EnvType.CLIENT)
public enum DebugProfileType implements StringIdentifiable {
	DEFAULT("default", "debug.options.profile.default"),
	PERFORMANCE("performance", "debug.options.profile.performance");

	public static final StringIdentifiable.EnumCodec<DebugProfileType> CODEC =
		StringIdentifiable.createCodec(DebugProfileType::values);

	private final String id;
	private final String translationKey;

	DebugProfileType(final String id, final String translationKey) {
		this.id = id;
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return translationKey;
	}

	@Override
	public String asString() {
		return id;
	}
}
