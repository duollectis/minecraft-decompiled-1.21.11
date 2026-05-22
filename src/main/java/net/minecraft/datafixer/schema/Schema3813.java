package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3813 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Регистрирует тип данных {@code SAVED_DATA_MAP_DATA} для сохранённых данных карт.
 * Карты теперь хранят список баннеров с текстовыми компонентами в поле {@code name},
 * что позволяет DataFixer корректно мигрировать пользовательские названия баннеров.
 */
public class Schema3813 extends IdentifierNormalizingSchema {

	public Schema3813(int versionKey, Schema parent) {
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
			false,
			TypeReferences.SAVED_DATA_MAP_DATA,
			() -> DSL.optionalFields(
				"data",
				DSL.optionalFields(
					"banners",
					DSL.list(DSL.optionalFields("name", TypeReferences.TEXT_COMPONENT.in(schema)))
				)
			)
		);
	}
}
