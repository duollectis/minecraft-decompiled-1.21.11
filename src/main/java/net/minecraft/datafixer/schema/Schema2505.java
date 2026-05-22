package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2505 (Minecraft 1.16 — Nether Update).
 * <p>
 * Регистрирует тип данных для сущности пиглина ({@code minecraft:piglin}),
 * добавленного в обновлении 1.16. Пиглин хранит инвентарь с предметами,
 * которые он подбирает или которыми торгует с игроком.
 */
public class Schema2505 extends IdentifierNormalizingSchema {

	public Schema2505(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:piglin",
			() -> DSL.optionalFields("Inventory", DSL.list(TypeReferences.ITEM_STACK.in(schema)))
		);
		return entityTypes;
	}
}
