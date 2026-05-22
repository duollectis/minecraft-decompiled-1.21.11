package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3327 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Регистрирует типы данных для двух новых блок-сущностей обновления 1.20:
 * украшенного горшка ({@code minecraft:decorated_pot}) с черепками и вложенным предметом,
 * а также подозрительного песка ({@code minecraft:suspicious_sand}) с хранимым предметом,
 * который можно извлечь кисточкой.
 */
public class Schema3327 extends IdentifierNormalizingSchema {

	public Schema3327(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:decorated_pot",
			() -> DSL.optionalFields(
				"shards",
				DSL.list(TypeReferences.ITEM_NAME.in(schema)),
				"item",
				TypeReferences.ITEM_STACK.in(schema)
			)
		);
		schema.register(
			blockEntityTypes,
			"minecraft:suspicious_sand",
			() -> DSL.optionalFields("item", TypeReferences.ITEM_STACK.in(schema))
		);
		return blockEntityTypes;
	}
}
