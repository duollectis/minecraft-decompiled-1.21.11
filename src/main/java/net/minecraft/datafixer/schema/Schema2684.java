package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 2684 (Minecraft 1.17 — Caves & Cliffs, часть I).
 * <p>
 * Вводит тип данных {@code GAME_EVENT_NAME} для идентификаторов игровых событий
 * (используются системой вибраций/скалка). Также регистрирует блок-сущность
 * датчика скалка ({@code minecraft:sculk_sensor}), который прослушивает
 * игровые события через вложенную структуру {@code listener → event → game_event}.
 */
public class Schema2684 extends IdentifierNormalizingSchema {

	public Schema2684(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public void registerTypes(
		Schema schema,
		Map<String, Supplier<TypeTemplate>> entityTypes,
		Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(false, TypeReferences.GAME_EVENT_NAME, () -> DSL.constType(getIdentifierType()));
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:sculk_sensor",
			() -> DSL.optionalFields(
				"listener",
				DSL.optionalFields(
					"event",
					DSL.optionalFields("game_event", TypeReferences.GAME_EVENT_NAME.in(schema))
				)
			)
		);
		return blockEntityTypes;
	}
}
