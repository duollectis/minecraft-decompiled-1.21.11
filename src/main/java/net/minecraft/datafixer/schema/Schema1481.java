package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1481: добавляет блок-сущность {@code minecraft:conduit} —
 * кондуит, появившийся в Update Aquatic (1.13).
 */
public class Schema1481 extends IdentifierNormalizingSchema {

	public Schema1481(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		schema.registerSimple(map, "minecraft:conduit");
		return map;
	}
}
