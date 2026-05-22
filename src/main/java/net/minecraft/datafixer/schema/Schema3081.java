package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3081 (Minecraft 1.19 — The Wild Update).
 * <p>
 * Регистрирует тип данных для сущности стража ({@code minecraft:warden}),
 * добавленного в обновлении 1.19. Страж прослушивает вибрации через систему
 * игровых событий, что отражено в структуре {@code listener → event → game_event}.
 */
public class Schema3081 extends IdentifierNormalizingSchema {

	public Schema3081(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:warden",
			() -> DSL.optionalFields(
				"listener",
				DSL.optionalFields(
					"event",
					DSL.optionalFields("game_event", TypeReferences.GAME_EVENT_NAME.in(schema))
				)
			)
		);
		return entityTypes;
	}
}
