package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 1931 (Minecraft 1.14 — Village & Pillage).
 * <p>
 * Регистрирует тип данных для сущности лисы ({@code minecraft:fox}),
 * добавленной в обновлении 1.14.
 */
public class Schema1931 extends IdentifierNormalizingSchema {

	public Schema1931(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:fox");
		return entityTypes;
	}
}
