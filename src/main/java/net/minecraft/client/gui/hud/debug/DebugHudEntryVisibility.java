package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringIdentifiable;

@Environment(EnvType.CLIENT)
/**
 * {@code DebugHudEntryVisibility}.
 */
public enum DebugHudEntryVisibility implements StringIdentifiable {
	ALWAYS_ON("alwaysOn"),
	IN_OVERLAY("inOverlay"),
	NEVER("never");

	public static final StringIdentifiable.EnumCodec<DebugHudEntryVisibility>
			CODEC =
			StringIdentifiable.createCodec(DebugHudEntryVisibility::values);
	private final String id;

	private DebugHudEntryVisibility(final String id) {
		this.id = id;
	}

	@Override
	public String asString() {
		return this.id;
	}
}
