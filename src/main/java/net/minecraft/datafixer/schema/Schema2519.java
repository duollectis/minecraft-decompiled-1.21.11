package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2519 (Minecraft 1.16 — Nether Update).
 * <p>
 * Регистрирует тип данных для сущности страйдера ({@code minecraft:strider}) —
 * пассивного моба Нижнего мира, способного ходить по лаве.
 */
public class Schema2519 extends IdentifierNormalizingSchema {

	public Schema2519(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:strider");
		return entityTypes;
	}
}
