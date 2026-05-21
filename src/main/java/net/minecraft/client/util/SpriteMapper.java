package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code SpriteMapper}.
 */
public record SpriteMapper(Identifier sheet, String prefix) {

	public SpriteIdentifier map(Identifier id) {
		return new SpriteIdentifier(this.sheet, id.withPrefixedPath(this.prefix + "/"));
	}

	public SpriteIdentifier mapVanilla(String id) {
		return this.map(Identifier.ofVanilla(id));
	}
}
