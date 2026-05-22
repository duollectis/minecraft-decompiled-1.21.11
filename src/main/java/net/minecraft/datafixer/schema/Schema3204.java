package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3204 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Регистрирует тип данных для блок-сущности резного книжного шкафа
 * ({@code minecraft:chiseled_bookshelf}), добавленного в обновлении 1.20.
 * Шкаф хранит до 6 книг в поле {@code Items}.
 */
public class Schema3204 extends IdentifierNormalizingSchema {

	public Schema3204(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:chiseled_bookshelf",
			() -> DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema)))
		);
		return blockEntityTypes;
	}
}
