package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3078 (Minecraft 1.19 — The Wild Update).
 * <p>
 * Регистрирует типы данных для новых сущностей обновления 1.19:
 * лягушки ({@code minecraft:frog}) и головастика ({@code minecraft:tadpole}).
 * Также добавляет блок-сущность визжащего скалка ({@code minecraft:sculk_shrieker}),
 * который прослушивает игровые события через структуру {@code listener → event → game_event}.
 */
public class Schema3078 extends IdentifierNormalizingSchema {

	public Schema3078(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	protected static void targetEntityItems(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, String entityId) {
		schema.registerSimple(entityTypes, entityId);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		targetEntityItems(schema, entityTypes, "minecraft:frog");
		targetEntityItems(schema, entityTypes, "minecraft:tadpole");
		return entityTypes;
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		schema.register(
			blockEntityTypes,
			"minecraft:sculk_shrieker",
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
