package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2571 (Minecraft 1.17 — Caves & Cliffs, часть I).
 * <p>
 * Регистрирует тип данных для сущности козла ({@code minecraft:goat}),
 * добавленного в обновлении 1.17 как горный пассивный моб.
 */
public class Schema2571 extends IdentifierNormalizingSchema {

	public Schema2571(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:goat");
		return entityTypes;
	}
}
