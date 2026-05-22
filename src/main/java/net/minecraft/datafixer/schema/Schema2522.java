package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2522 (Minecraft 1.16 — Nether Update).
 * <p>
 * Регистрирует тип данных для сущности зоглина ({@code minecraft:zoglin}) —
 * зомбифицированной версии хоглина, агрессивного обитателя Нижнего мира.
 */
public class Schema2522 extends IdentifierNormalizingSchema {

	public Schema2522(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:zoglin");
		return entityTypes;
	}
}
