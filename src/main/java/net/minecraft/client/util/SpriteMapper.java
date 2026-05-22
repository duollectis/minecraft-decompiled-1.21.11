package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

/**
 * Маппер спрайтов: преобразует идентификатор в {@link SpriteIdentifier} с заданным атласом и префиксом пути.
 */
@Environment(EnvType.CLIENT)
public record SpriteMapper(Identifier sheet, String prefix) {

	public SpriteIdentifier map(Identifier id) {
		return new SpriteIdentifier(sheet, id.withPrefixedPath(prefix + "/"));
	}

	public SpriteIdentifier mapVanilla(String id) {
		return map(Identifier.ofVanilla(id));
	}
}
