package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3689 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Регистрирует типы данных для новых сущностей обновления 1.21:
 * бриза ({@code minecraft:breeze}), заряда ветра ({@code minecraft:wind_charge})
 * и заряда ветра бриза ({@code minecraft:breeze_wind_charge}).
 * Также добавляет блок-сущность испытательного спаунера ({@code minecraft:trial_spawner})
 * с поддержкой потенциальных спаунеров и текущих данных спауна.
 */
public class Schema3689 extends IdentifierNormalizingSchema {

	public Schema3689(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:breeze");
		schema.registerSimple(entityTypes, "minecraft:wind_charge");
		schema.registerSimple(entityTypes, "minecraft:breeze_wind_charge");
		return entityTypes;
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:trial_spawner",
			() -> DSL.optionalFields(
				"spawn_potentials",
				DSL.list(DSL.fields("data", DSL.fields("entity", TypeReferences.ENTITY_TREE.in(schema)))),
				"spawn_data",
				DSL.fields("entity", TypeReferences.ENTITY_TREE.in(schema))
			)
		);
		return blockEntityTypes;
	}
}
