package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема DataFixer версии 4292, переводящая формат {@code TEXT_COMPONENT} на новый
 * синтаксис {@code hover_event} (snake_case вместо camelCase). Поле {@code show_item}
 * теперь напрямую ссылается на {@code ITEM_STACK}, а {@code show_entity} использует
 * поле {@code id} вместо {@code type}.
 */
public class Schema4292 extends IdentifierNormalizingSchema {

	public Schema4292(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	public void registerTypes(
			Schema schema,
			Map<String, Supplier<TypeTemplate>> map,
			Map<String, Supplier<TypeTemplate>> map2
	) {
		super.registerTypes(schema, map, map2);
		schema.registerType(
				true,
				TypeReferences.TEXT_COMPONENT,
				() -> DSL.or(
						DSL.or(DSL.constType(DSL.string()), DSL.list(TypeReferences.TEXT_COMPONENT.in(schema))),
						DSL.optionalFields(
								"extra",
								DSL.list(TypeReferences.TEXT_COMPONENT.in(schema)),
								"separator",
								TypeReferences.TEXT_COMPONENT.in(schema),
								"hover_event",
								DSL.taggedChoice(
										"action",
										DSL.string(),
										Map.of(
												"show_text",
												DSL.optionalFields("value", TypeReferences.TEXT_COMPONENT.in(schema)),
												"show_item",
												TypeReferences.ITEM_STACK.in(schema),
												"show_entity",
												DSL.optionalFields(
														"id",
														TypeReferences.ENTITY_NAME.in(schema),
														"name",
														TypeReferences.TEXT_COMPONENT.in(schema)
												)
										)
								)
						)
				)
		);
	}
}
