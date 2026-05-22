package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4656, регистрирующая новые сущности пустынной тематики:
 * {@code minecraft:camel_husk} и {@code minecraft:parched}.
 */
public class Schema4656 extends IdentifierNormalizingSchema {

	public Schema4656(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:camel_husk");
		schema.registerSimple(map, "minecraft:parched");
		return map;
	}
}
