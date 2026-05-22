package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3808 подверсии 2 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Обновляет тип данных торговой ламы ({@code minecraft:trader_llama}): теперь она
 * хранит поля {@code Items} (инвентарь сундука) и {@code SaddleItem} (предмет седла),
 * аналогично обычной ламе в {@link Schema3808_1}.
 */
public class Schema3808_2 extends IdentifierNormalizingSchema {

	public Schema3808_2(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:trader_llama",
			name -> DSL.optionalFields(
				"Items",
				DSL.list(TypeReferences.ITEM_STACK.in(schema)),
				"SaddleItem",
				TypeReferences.ITEM_STACK.in(schema)
			)
		);
		return entityTypes;
	}
}
