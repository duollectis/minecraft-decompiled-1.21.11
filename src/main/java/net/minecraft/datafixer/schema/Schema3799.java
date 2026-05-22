package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3799 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Регистрирует тип данных для сущности броненосца ({@code minecraft:armadillo}),
 * добавленного в обновлении 1.21 как пассивный моб саванны, способный
 * сворачиваться в шар при угрозе.
 */
public class Schema3799 extends IdentifierNormalizingSchema {

	public Schema3799(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:armadillo");
		return entityTypes;
	}
}
