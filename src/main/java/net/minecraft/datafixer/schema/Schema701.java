package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 701, добавляющая сущности {@code WitherSkeleton}
 * (скелет-иссушитель) и {@code Stray} (бродяга).
 */
public class Schema701 extends Schema {

	public Schema701(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "WitherSkeleton");
		schema.registerSimple(map, "Stray");
		return map;
	}
}
