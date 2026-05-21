package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;

/**
 * {@code Schema3818_3}.
 */
public class Schema3818_3 extends IdentifierNormalizingSchema {

	public Schema3818_3(int i, Schema schema) {
		super(i, schema);
	}

	public static SequencedMap<String, Supplier<TypeTemplate>> createDataComponentsMap(Schema schema) {
		SequencedMap<String, Supplier<TypeTemplate>> sequencedMap = new LinkedHashMap<>();
		sequencedMap.put(
				"minecraft:bees",
				() -> DSL.list(DSL.optionalFields("entity_data", TypeReferences.ENTITY_TREE.in(schema)))
		);
		sequencedMap.put("minecraft:block_entity_data", () -> TypeReferences.BLOCK_ENTITY.in(schema));
		sequencedMap.put("minecraft:bundle_contents", () -> DSL.list(TypeReferences.ITEM_STACK.in(schema)));
		sequencedMap.put(
				"minecraft:can_break",
				() -> DSL.optionalFields(
						"predicates",
						DSL.list(DSL.optionalFields(
								"blocks",
								DSL.or(
										TypeReferences.BLOCK_NAME.in(schema),
										DSL.list(TypeReferences.BLOCK_NAME.in(schema))
								)
						))
				)
		);
		sequencedMap.put(
				"minecraft:can_place_on",
				() -> DSL.optionalFields(
						"predicates",
						DSL.list(DSL.optionalFields(
								"blocks",
								DSL.or(
										TypeReferences.BLOCK_NAME.in(schema),
										DSL.list(TypeReferences.BLOCK_NAME.in(schema))
								)
						))
				)
		);
		sequencedMap.put("minecraft:charged_projectiles", () -> DSL.list(TypeReferences.ITEM_STACK.in(schema)));
		sequencedMap.put(
				"minecraft:container",
				() -> DSL.list(DSL.optionalFields("item", TypeReferences.ITEM_STACK.in(schema)))
		);
		sequencedMap.put("minecraft:entity_data", () -> TypeReferences.ENTITY_TREE.in(schema));
		sequencedMap.put("minecraft:pot_decorations", () -> DSL.list(TypeReferences.ITEM_NAME.in(schema)));
		sequencedMap.put(
				"minecraft:food",
				() -> DSL.optionalFields("using_converts_to", TypeReferences.ITEM_STACK.in(schema))
		);
		sequencedMap.put("minecraft:custom_name", () -> TypeReferences.TEXT_COMPONENT.in(schema));
		sequencedMap.put("minecraft:item_name", () -> TypeReferences.TEXT_COMPONENT.in(schema));
		sequencedMap.put("minecraft:lore", () -> DSL.list(TypeReferences.TEXT_COMPONENT.in(schema)));
		sequencedMap.put(
				"minecraft:written_book_content",
				() -> DSL.optionalFields(
						"pages",
						DSL.list(
								DSL.or(
										DSL.optionalFields(
												"raw",
												TypeReferences.TEXT_COMPONENT.in(schema),
												"filtered",
												TypeReferences.TEXT_COMPONENT.in(schema)
										),
										TypeReferences.TEXT_COMPONENT.in(schema)
								)
						)
				)
		);
		return sequencedMap;
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> map,
			Map<String, Supplier<TypeTemplate>> map2
	) {
		super.registerTypes(schema, map, map2);
		schema.registerType(true, TypeReferences.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(createDataComponentsMap(schema)));
	}
}
