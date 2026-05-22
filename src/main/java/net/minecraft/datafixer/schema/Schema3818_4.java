package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3818 подверсии 4 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Обновляет тип данных {@code PARTICLE} для поддержки новой системы компонентов:
 * частицы теперь могут содержать вложенный стек предмета ({@code item})
 * и состояние блока ({@code block_state}), что необходимо для корректной
 * миграции данных частиц при обновлении мира.
 */
public class Schema3818_4 extends IdentifierNormalizingSchema {

	public Schema3818_4(int versionKey, Schema parent) {
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
			TypeReferences.PARTICLE,
			() -> DSL.optionalFields(
				"item",
				TypeReferences.ITEM_STACK.in(schema),
				"block_state",
				TypeReferences.BLOCK_STATE.in(schema)
			)
		);
	}
}
