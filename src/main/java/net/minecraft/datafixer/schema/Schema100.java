package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema100}.
 */
public class Schema100 extends Schema {

	public Schema100(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> entityTypes,
			Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(
				true,
				TypeReferences.ENTITY_EQUIPMENT,
				() -> DSL.and(
						DSL.optional(DSL.field("ArmorItems", DSL.list(TypeReferences.ITEM_STACK.in(schema)))),
						new TypeTemplate[]{
								DSL.optional(DSL.field("HandItems", DSL.list(TypeReferences.ITEM_STACK.in(schema)))),
								DSL.optional(DSL.field("body_armor_item", TypeReferences.ITEM_STACK.in(schema))),
								DSL.optional(DSL.field("saddle", TypeReferences.ITEM_STACK.in(schema)))
						}
				)
		);
	}
}
