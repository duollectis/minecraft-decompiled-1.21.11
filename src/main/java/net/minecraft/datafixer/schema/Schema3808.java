package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3808 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Обновляет тип данных лошади ({@code minecraft:horse}): теперь она хранит
 * только поле {@code SaddleItem} (предмет седла), так как броня лошади
 * была перенесена в компоненты предмета в рамках обновления системы инвентаря.
 */
public class Schema3808 extends IdentifierNormalizingSchema {

	public Schema3808(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:horse",
			name -> DSL.optionalFields("SaddleItem", TypeReferences.ITEM_STACK.in(schema))
		);
		return entityTypes;
	}
}
