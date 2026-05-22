package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2704 (Minecraft 1.17 — Caves & Cliffs, часть I).
 * <p>
 * Повторно регистрирует тип данных для сущности козла ({@code minecraft:goat})
 * после изменений в структуре данных, внесённых в промежуточных снапшотах 1.17.
 */
public class Schema2704 extends IdentifierNormalizingSchema {

	public Schema2704(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:goat");
		return entityTypes;
	}
}
