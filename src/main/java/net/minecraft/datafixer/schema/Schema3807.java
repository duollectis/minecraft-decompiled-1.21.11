package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3807 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Регистрирует тип данных для блок-сущности хранилища ({@code minecraft:vault}),
 * добавленного в обновлении 1.21. Хранилище содержит три секции данных:
 * конфигурацию с ключевым предметом ({@code config}), серверные данные
 * с очередью выдачи предметов ({@code server_data}) и общие данные
 * с отображаемым предметом ({@code shared_data}).
 */
public class Schema3807 extends IdentifierNormalizingSchema {

	public Schema3807(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:vault",
			() -> DSL.optionalFields(
				"config",
				DSL.optionalFields("key_item", TypeReferences.ITEM_STACK.in(schema)),
				"server_data",
				DSL.optionalFields("items_to_eject", DSL.list(TypeReferences.ITEM_STACK.in(schema))),
				"shared_data",
				DSL.optionalFields("display_item", TypeReferences.ITEM_STACK.in(schema))
			)
		);
		return blockEntityTypes;
	}
}
