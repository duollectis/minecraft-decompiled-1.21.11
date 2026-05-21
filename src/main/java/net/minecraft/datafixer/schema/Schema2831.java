package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema2831}.
 */
public class Schema2831 extends IdentifierNormalizingSchema {

	public Schema2831(int i, Schema schema) {
		super(i, schema);
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> entityTypes,
			Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(
				true,
				TypeReferences.UNTAGGED_SPAWNER,
				() -> DSL.optionalFields(
						"SpawnPotentials",
						DSL.list(DSL.fields("data", DSL.fields("entity", TypeReferences.ENTITY_TREE.in(schema)))),
						"SpawnData",
						DSL.fields("entity", TypeReferences.ENTITY_TREE.in(schema))
				)
		);
	}
}
