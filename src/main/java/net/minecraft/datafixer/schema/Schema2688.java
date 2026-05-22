package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2688 (Minecraft 1.17 — Caves & Cliffs, часть I).
 * <p>
 * Регистрирует типы данных для двух новых сущностей обновления 1.17:
 * светящегося кальмара ({@code minecraft:glow_squid}) без дополнительных полей,
 * а также светящейся рамки для предметов ({@code minecraft:glow_item_frame}),
 * хранящей вложенный стек предмета в поле {@code Item}.
 */
public class Schema2688 extends IdentifierNormalizingSchema {

	public Schema2688(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.registerSimple(entityTypes, "minecraft:glow_squid");
		schema.register(
			entityTypes,
			"minecraft:glow_item_frame",
			name -> DSL.optionalFields("Item", TypeReferences.ITEM_STACK.in(schema))
		);
		return entityTypes;
	}
}
