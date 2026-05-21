package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;

/**
 * {@code Schema4059}.
 */
public class Schema4059 extends IdentifierNormalizingSchema {

	public Schema4059(int i, Schema schema) {
		super(i, schema);
	}

	public static SequencedMap<String, Supplier<TypeTemplate>> createDataComponentsMap(Schema schema) {
		SequencedMap<String, Supplier<TypeTemplate>> sequencedMap = Schema3818_3.createDataComponentsMap(schema);
		sequencedMap.remove("minecraft:food");
		sequencedMap.put("minecraft:use_remainder", () -> TypeReferences.ITEM_STACK.in(schema));
		sequencedMap.put(
				"minecraft:equippable",
				() -> DSL.optionalFields(
						"allowed_entities",
						DSL.or(TypeReferences.ENTITY_NAME.in(schema), DSL.list(TypeReferences.ENTITY_NAME.in(schema)))
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
