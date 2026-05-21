package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema4656}.
 */
public class Schema4656 extends IdentifierNormalizingSchema {

	public Schema4656(int i, Schema schema) {
		super(i, schema);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:camel_husk");
		schema.registerSimple(map, "minecraft:parched");
		return map;
	}
}
