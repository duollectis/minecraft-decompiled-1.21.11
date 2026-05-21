package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema2568}.
 */
public class Schema2568 extends IdentifierNormalizingSchema {

	public Schema2568(int i, Schema schema) {
		super(i, schema);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:piglin_brute");
		return map;
	}
}
