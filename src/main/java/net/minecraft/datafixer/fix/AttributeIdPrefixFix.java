package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.List;

/**
 * Удаляет устаревшие префиксы категорий атрибутов ({@code generic.}, {@code horse.},
 * {@code player.}, {@code zombie.}) и добавляет namespace {@code minecraft:}.
 */
public class AttributeIdPrefixFix extends AttributeRenameFix {

	private static final List<String> LEGACY_PREFIXES = List.of("generic.", "horse.", "player.", "zombie.");

	public AttributeIdPrefixFix(Schema outputSchema) {
		super(outputSchema, "AttributeIdPrefixFix", AttributeIdPrefixFix::removePrefix);
	}

	private static String removePrefix(String id) {
		String normalized = IdentifierNormalizingSchema.normalize(id);

		for (String prefix : LEGACY_PREFIXES) {
			String normalizedPrefix = IdentifierNormalizingSchema.normalize(prefix);

			if (normalized.startsWith(normalizedPrefix)) {
				return "minecraft:" + normalized.substring(normalizedPrefix.length());
			}
		}

		return id;
	}
}
