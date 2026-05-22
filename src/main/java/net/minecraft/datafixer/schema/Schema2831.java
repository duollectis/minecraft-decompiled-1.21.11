package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2831 (Minecraft 1.18 — Caves & Cliffs, часть II).
 * <p>
 * Обновляет тип данных {@code UNTAGGED_SPAWNER} для спаунеров мобов.
 * Новая структура хранит потенциальных спаунеров в поле {@code SpawnPotentials}
 * (список с вложенными данными сущностей) и текущие данные спауна в {@code SpawnData}.
 * Флаг {@code true} означает, что тип является рекурсивным.
 */
public class Schema2831 extends IdentifierNormalizingSchema {

	public Schema2831(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public void registerTypes(
		Schema schema,
		Map<String, Supplier<TypeTemplate>> entityTypes,
		Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(
			true,
			TypeReferences.UNTAGGED_SPAWNER,
			() -> DSL.optionalFields(
				"SpawnPotentials",
				DSL.list(DSL.fields("data", DSL.fields("entity", TypeReferences.ENTITY_TREE.in(schema)))),
				"SpawnData",
				DSL.fields("entity", TypeReferences.ENTITY_TREE.in(schema))
			)
		);
	}
}
