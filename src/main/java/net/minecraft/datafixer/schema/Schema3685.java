package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3685 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Обновляет типы данных для снарядов-стрел: трезубца ({@code minecraft:trident}),
 * спектральной стрелы ({@code minecraft:spectral_arrow}) и обычной стрелы
 * ({@code minecraft:arrow}). Все три теперь хранят поля {@code inBlockState}
 * (состояние блока, в котором застряла стрела) и {@code item} (предмет стрелы).
 */
public class Schema3685 extends IdentifierNormalizingSchema {

	public Schema3685(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	protected static TypeTemplate registerFields(Schema schema) {
		return DSL.optionalFields(
			"inBlockState",
			TypeReferences.BLOCK_STATE.in(schema),
			"item",
			TypeReferences.ITEM_STACK.in(schema)
		);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(entityTypes, "minecraft:trident", () -> registerFields(schema));
		schema.register(entityTypes, "minecraft:spectral_arrow", () -> registerFields(schema));
		schema.register(entityTypes, "minecraft:arrow", () -> registerFields(schema));
		return entityTypes;
	}
}
