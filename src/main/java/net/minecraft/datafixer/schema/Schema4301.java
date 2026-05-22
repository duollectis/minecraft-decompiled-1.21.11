package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4301, переводящая снаряжение сущностей на новый формат:
 * вместо массива {@code Equipment} теперь используется объект {@code equipment}
 * с именованными слотами (mainhand, offhand, feet, legs, chest, head, body, saddle).
 */
public class Schema4301 extends IdentifierNormalizingSchema {

	public Schema4301(int versionKey, Schema parent) {
		super(versionKey, parent);
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
