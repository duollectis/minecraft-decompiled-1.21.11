package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2511 подверсии 1 (Minecraft 1.16 — Nether Update).
 * <p>
 * Обновляет тип данных для сущности брошенного зелья ({@code minecraft:potion}):
 * добавляет ссылку на вложенный стек предмета в поле {@code Item},
 * что позволяет DataFixer корректно мигрировать данные зелья при обновлении.
 */
public class Schema2511_1 extends IdentifierNormalizingSchema {

	public Schema2511_1(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:potion",
			name -> DSL.optionalFields("Item", TypeReferences.ITEM_STACK.in(schema))
		);
		return entityTypes;
	}
}
