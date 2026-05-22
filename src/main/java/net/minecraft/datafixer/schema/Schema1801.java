package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1801: добавляет сущность {@code minecraft:illager_beast} —
 * временное имя равагера до его переименования в версии 1928.
 */
public class Schema1801 extends IdentifierNormalizingSchema {

	public Schema1801(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:illager_beast");
		return map;
	}
}
