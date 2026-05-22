package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3325 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Регистрирует типы данных для трёх новых сущностей-дисплеев, добавленных в 1.20:
 * дисплея предмета ({@code minecraft:item_display}) с полем {@code item},
 * дисплея блока ({@code minecraft:block_display}) с полем {@code block_state},
 * а также текстового дисплея ({@code minecraft:text_display}) с полем {@code text}.
 */
public class Schema3325 extends IdentifierNormalizingSchema {

	public Schema3325(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:item_display",
			name -> DSL.optionalFields("item", TypeReferences.ITEM_STACK.in(schema))
		);
		schema.register(
			entityTypes,
			"minecraft:block_display",
			name -> DSL.optionalFields("block_state", TypeReferences.BLOCK_STATE.in(schema))
		);
		schema.register(
			entityTypes,
			"minecraft:text_display",
			() -> DSL.optionalFields("text", TypeReferences.TEXT_COMPONENT.in(schema))
		);
		return entityTypes;
	}
}
