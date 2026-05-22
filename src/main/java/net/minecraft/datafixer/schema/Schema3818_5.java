package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3818 подверсии 5 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Обновляет тип данных {@code ITEM_STACK} для новой системы компонентов предметов:
 * стек предмета теперь описывается через поле {@code id} (идентификатор предмета)
 * и поле {@code components} (карта компонентов из {@code DATA_COMPONENTS}),
 * заменяя устаревшую структуру с тегом {@code tag}.
 */
public class Schema3818_5 extends IdentifierNormalizingSchema {

	public Schema3818_5(int versionKey, Schema parent) {
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
			TypeReferences.ITEM_STACK,
			() -> DSL.optionalFields(
				"id",
				TypeReferences.ITEM_NAME.in(schema),
				"components",
				TypeReferences.DATA_COMPONENTS.in(schema)
			)
		);
	}
}
