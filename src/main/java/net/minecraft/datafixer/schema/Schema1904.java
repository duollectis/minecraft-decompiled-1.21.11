package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1904: регистрирует сущность {@code minecraft:cat} —
 * кошка была выделена в отдельный тип из {@code minecraft:ocelot}.
 */
public class Schema1904 extends IdentifierNormalizingSchema {

	public Schema1904(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:cat");
		return entityTypes;
	}
}
