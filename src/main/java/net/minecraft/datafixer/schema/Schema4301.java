package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema4301}.
 */
public class Schema4301 extends IdentifierNormalizingSchema {

	public Schema4301(int i, Schema schema) {
		super(i, schema);
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> map,
			Map<String, Supplier<TypeTemplate>> map2
	) {
		super.registerTypes(schema, map, map2);
		schema.registerType(
				true,
				TypeReferences.ENTITY_EQUIPMENT,
				() -> DSL.optional(
						DSL.field(
								"equipment",
								DSL.optionalFields(
										new Pair[]{
												Pair.of("mainhand", TypeReferences.ITEM_STACK.in(schema)),
												Pair.of("offhand", TypeReferences.ITEM_STACK.in(schema)),
												Pair.of("feet", TypeReferences.ITEM_STACK.in(schema)),
												Pair.of("legs", TypeReferences.ITEM_STACK.in(schema)),
												Pair.of("chest", TypeReferences.ITEM_STACK.in(schema)),
												Pair.of("head", TypeReferences.ITEM_STACK.in(schema)),
												Pair.of("body", TypeReferences.ITEM_STACK.in(schema)),
												Pair.of("saddle", TypeReferences.ITEM_STACK.in(schema))
										}
								)
						)
				)
		);
	}
}
