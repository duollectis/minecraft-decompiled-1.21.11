package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3448 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Обновляет тип данных украшенного горшка ({@code minecraft:decorated_pot}):
 * поле {@code shards} переименовывается в {@code sherds} для соответствия
 * официальной терминологии черепков гончарного дела.
 */
public class Schema3448 extends IdentifierNormalizingSchema {

	public Schema3448(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:decorated_pot",
			() -> DSL.optionalFields(
				"sherds",
				DSL.list(TypeReferences.ITEM_NAME.in(schema)),
				"item",
				TypeReferences.ITEM_STACK.in(schema)
			)
		);
		return blockEntityTypes;
	}
}
