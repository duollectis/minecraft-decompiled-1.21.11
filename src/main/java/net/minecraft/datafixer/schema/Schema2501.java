package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema2501}.
 */
public class Schema2501 extends IdentifierNormalizingSchema {

	public Schema2501(int i, Schema schema) {
		super(i, schema);
	}

	private static void registerFurnace(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
		schema.register(
				map,
				name,
				() -> DSL.optionalFields(
						"Items",
						DSL.list(TypeReferences.ITEM_STACK.in(schema)),
						"CustomName",
						TypeReferences.TEXT_COMPONENT.in(schema),
						"RecipesUsed",
						DSL.compoundList(TypeReferences.RECIPE.in(schema), DSL.constType(DSL.intType()))
				)
		);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		registerFurnace(schema, map, "minecraft:furnace");
		registerFurnace(schema, map, "minecraft:smoker");
		registerFurnace(schema, map, "minecraft:blast_furnace");
		return map;
	}
}
