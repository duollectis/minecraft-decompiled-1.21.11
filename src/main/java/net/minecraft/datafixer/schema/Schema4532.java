package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4532, регистрирующая блок-сущность
 * {@code minecraft:copper_golem_statue} — постамент медного голема.
 */
public class Schema4532 extends IdentifierNormalizingSchema {

	public Schema4532(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
		schema.registerSimple(map, "minecraft:copper_golem_statue");
		return map;
	}
}
