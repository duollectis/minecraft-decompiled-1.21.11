package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * {@code Schema2509}.
 */
public class Schema2509 extends IdentifierNormalizingSchema {

	public Schema2509(int i, Schema schema) {
		super(i, schema);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		map.remove("minecraft:zombie_pigman");
		schema.registerSimple(map, "minecraft:zombified_piglin");
		return map;
	}
}
