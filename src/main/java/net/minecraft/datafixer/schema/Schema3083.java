package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3083 (Minecraft 1.19 — The Wild Update).
 * <p>
 * Регистрирует тип данных для сущности аллая ({@code minecraft:allay}),
 * добавленного в обновлении 1.19. Аллай хранит инвентарь предметов и
 * прослушивает игровые события через структуру {@code listener → event → game_event},
 * реагируя на музыкальные диски и заметки.
 */
public class Schema3083 extends IdentifierNormalizingSchema {

	public Schema3083(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> entityTypes = super.registerEntities(schema);
		schema.register(
			entityTypes,
			"minecraft:allay",
			() -> DSL.optionalFields(
				"Inventory",
				DSL.list(TypeReferences.ITEM_STACK.in(schema)),
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
