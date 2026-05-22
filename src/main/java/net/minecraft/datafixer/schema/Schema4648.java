package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4648, регистрирующая новые морские сущности:
 * {@code minecraft:nautilus} и {@code minecraft:zombie_nautilus}.
 */
public class Schema4648 extends IdentifierNormalizingSchema {

	public Schema4648(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
		schema.registerSimple(map, "minecraft:nautilus");
		schema.registerSimple(map, "minecraft:zombie_nautilus");
		return map;
	}
}
