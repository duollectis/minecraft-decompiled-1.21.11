package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2568 (Minecraft 1.16.2 — Nether Update).
 * <p>
 * Регистрирует тип данных для сущности пиглина-головореза ({@code minecraft:piglin_brute}),
 * добавленного в патче 1.16.2 как более сильная и агрессивная разновидность пиглина,
 * охраняющая бастионы Нижнего мира.
 */
public class Schema2568 extends IdentifierNormalizingSchema {

	public Schema2568(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:piglin_brute");
		return entityTypes;
	}
}
