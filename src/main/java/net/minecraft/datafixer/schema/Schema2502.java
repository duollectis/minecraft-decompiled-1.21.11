package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2502 (Minecraft 1.16 — Nether Update).
 * <p>
 * Регистрирует тип данных для сущности хоглина ({@code minecraft:hoglin}),
 * добавленного в обновлении 1.16 как агрессивный обитатель Нижнего мира.
 */
public class Schema2502 extends IdentifierNormalizingSchema {

	public Schema2502(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:hoglin");
		return entityTypes;
	}
}
