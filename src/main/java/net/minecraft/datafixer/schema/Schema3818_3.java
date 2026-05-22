package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;

/**
 * Схема версии 3818 подверсии 3 (Minecraft 1.21 — Tricky Trials).
 * <p>
 * Вводит тип данных {@code DATA_COMPONENTS} — центральный реестр компонентов предметов
 * новой системы NBT-компонентов (Item Components), заменяющей разрозненные поля NBT.
 * Каждый компонент описывает отдельный аспект предмета: содержимое контейнера,
 * данные сущности, текстовые поля, рецепты и т.д. Порядок компонентов в карте
 * сохраняется через {@link LinkedHashMap} для детерминированной обработки.
 */
public class Schema3818_3 extends IdentifierNormalizingSchema {

	public Schema3818_3(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	/**
	 * Создаёт упорядоченную карту всех известных компонентов предметов с их шаблонами типов.
	 * Используется при регистрации типа {@code DATA_COMPONENTS} для описания структуры
	 * каждого компонента и вложенных ссылок на другие типы DataFixer.
	 *
	 * @param schema текущая схема DataFixer для разрешения ссылок на типы
	 * @return упорядоченная карта имён компонентов и их шаблонов типов
	 */
	public static SequencedMap<String, Supplier<TypeTemplate>> createDataComponentsMap(Schema schema) {
		SequencedMap<String, Supplier<TypeTemplate>> components = new LinkedHashMap<>();
		components.put(
			"minecraft:bees",
			() -> DSL.list(DSL.optionalFields("entity_data", TypeReferences.ENTITY_TREE.in(schema)))
		);
		components.put("minecraft:block_entity_data", () -> TypeReferences.BLOCK_ENTITY.in(schema));
		components.put("minecraft:bundle_contents", () -> DSL.list(TypeReferences.ITEM_STACK.in(schema)));
		components.put(
			"minecraft:can_break",
			() -> DSL.optionalFields(
				"predicates",
				DSL.list(DSL.optionalFields(
					"blocks",
					DSL.or(
						TypeReferences.BLOCK_NAME.in(schema),
						DSL.list(TypeReferences.BLOCK_NAME.in(schema))
					)
				))
			)
		);
		components.put(
			"minecraft:can_place_on",
			() -> DSL.optionalFields(
				"predicates",
				DSL.list(DSL.optionalFields(
					"blocks",
					DSL.or(
						TypeReferences.BLOCK_NAME.in(schema),
						DSL.list(TypeReferences.BLOCK_NAME.in(schema))
					)
				))
			)
		);
		components.put("minecraft:charged_projectiles", () -> DSL.list(TypeReferences.ITEM_STACK.in(schema)));
		components.put(
			"minecraft:container",
			() -> DSL.list(DSL.optionalFields("item", TypeReferences.ITEM_STACK.in(schema)))
		);
		components.put("minecraft:entity_data", () -> TypeReferences.ENTITY_TREE.in(schema));
		components.put("minecraft:pot_decorations", () -> DSL.list(TypeReferences.ITEM_NAME.in(schema)));
		components.put(
			"minecraft:food",
			() -> DSL.optionalFields("using_converts_to", TypeReferences.ITEM_STACK.in(schema))
		);
		components.put("minecraft:custom_name", () -> TypeReferences.TEXT_COMPONENT.in(schema));
		components.put("minecraft:item_name", () -> TypeReferences.TEXT_COMPONENT.in(schema));
		components.put("minecraft:lore", () -> DSL.list(TypeReferences.TEXT_COMPONENT.in(schema)));
		components.put(
			"minecraft:written_book_content",
			() -> DSL.optionalFields(
				"pages",
				DSL.list(
					DSL.or(
						DSL.optionalFields(
							"raw",
							TypeReferences.TEXT_COMPONENT.in(schema),
							"filtered",
							TypeReferences.TEXT_COMPONENT.in(schema)
						),
						TypeReferences.TEXT_COMPONENT.in(schema)
					)
				)
			)
		);
		return components;
	}

	@Override
	public void registerTypes(
		Schema schema,
		Map<String, Supplier<TypeTemplate>> entityTypes,
		Map<String, Supplier<TypeTemplate>> blockEntityTypes
	) {
		super.registerTypes(schema, entityTypes, blockEntityTypes);
		schema.registerType(true, TypeReferences.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(createDataComponentsMap(schema)));
	}
}
