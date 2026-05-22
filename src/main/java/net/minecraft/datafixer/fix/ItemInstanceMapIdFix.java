package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Objects;
import java.util.Optional;

/**
 * Исправляет данные в формате DataFixer.
 */
public class ItemInstanceMapIdFix extends DataFix {

	public ItemInstanceMapIdFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	public TypeRewriteRule makeRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.ITEM_STACK);
		OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder(
				"id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);
		OpticFinder<?> opticFinder2 = type.findField("tag");
		return fixTypeEverywhereTyped(
				"ItemInstanceMapIdFix", type, itemStack -> {
					Optional<Pair<String, String>> optional = itemStack.getOptional(opticFinder);
					if (optional.isPresent() && Objects.equals(optional.get().getSecond(), "minecraft:filled_map")) {
						Dynamic<?> dynamic = (Dynamic<?>) itemStack.get(DSL.remainderFinder());
						Typed<?> typed = itemStack.getOrCreateTyped(opticFinder2);
						Dynamic<?> dynamic2 = (Dynamic<?>) typed.get(DSL.remainderFinder());
						dynamic2 = dynamic2.set("map", dynamic2.createInt(dynamic.get("Damage").asInt(0)));
						return itemStack.set(opticFinder2, typed.set(DSL.remainderFinder(), dynamic2));
					}
					else {
						return itemStack;
					}
				}
		);
	}
}
