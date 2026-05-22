package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2686 (Minecraft 1.17 — Caves & Cliffs, часть I).
 * <p>
 * Регистрирует тип данных для сущности аксолотля ({@code minecraft:axolotl}),
 * добавленного в обновлении 1.17 как водный пассивный моб, обитающий в пещерах.
 */
public class Schema2686 extends IdentifierNormalizingSchema {

	public Schema2686(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:axolotl");
		return entityTypes;
	}
}
