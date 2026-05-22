package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Optional;

/**
 * Исправляет данные в формате DataFixer.
 */
public class RemoveEmptyItemInSuspiciousBlockFix extends ChoiceWriteReadFix {

	public RemoveEmptyItemInSuspiciousBlockFix(Schema outputSchema) {
		super(
				outputSchema,
				false,
				"RemoveEmptyItemInSuspiciousBlockFix",
				TypeReferences.BLOCK_ENTITY,
				"minecraft:brushable_block"
		);
	}

	@Override
	protected <T> Dynamic<T> transform(Dynamic<T> data) {
		Optional<Dynamic<T>> optional = data.get("item").result();
		return optional.isPresent() && shouldRemoveItem(optional.get()) ? data.remove("item") : data;
	}

	private static boolean shouldRemoveItem(Dynamic<?> dynamic) {
		String string = IdentifierNormalizingSchema.normalize(dynamic.get("id").asString("minecraft:air"));
		int i = dynamic.get("count").asInt(0);
		return string.equals("minecraft:air") || i == 0;
	}
}
