package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;

/**
 * Режим видимости записи отладочного HUD:
 * всегда, только при открытом F3-оверлее, или никогда.
 */
@Environment(EnvType.CLIENT)
public enum DebugHudEntryVisibility implements StringIdentifiable {
	ALWAYS_ON("alwaysOn"),
	IN_OVERLAY("inOverlay"),
	NEVER("never");

	public static final StringIdentifiable.EnumCodec<DebugHudEntryVisibility> CODEC =
		StringIdentifiable.createCodec(DebugHudEntryVisibility::values);

	private final String id;

	DebugHudEntryVisibility(final String id) {
		this.id = id;
	}

	@Override
	public String asString() {
		return id;
	}
}
