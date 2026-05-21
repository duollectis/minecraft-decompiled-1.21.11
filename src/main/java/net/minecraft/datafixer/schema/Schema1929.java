package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema1929}.
 */
public class Schema1929 extends IdentifierNormalizingSchema {

	public Schema1929(int i, Schema schema) {
		super(i, schema);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.register(
				map,
				"minecraft:wandering_trader",
				name -> DSL.optionalFields(
						"Inventory",
						DSL.list(TypeReferences.ITEM_STACK.in(schema)),
						"Offers",
						DSL.optionalFields("Recipes", DSL.list(TypeReferences.VILLAGER_TRADE.in(schema)))
				)
		);
		schema.register(
				map,
				"minecraft:trader_llama",
				name -> DSL.optionalFields(
						"Items",
						DSL.list(TypeReferences.ITEM_STACK.in(schema)),
						"SaddleItem",
						TypeReferences.ITEM_STACK.in(schema),
						"DecorItem",
						TypeReferences.ITEM_STACK.in(schema)
				)
		);
		return map;
	}
}
