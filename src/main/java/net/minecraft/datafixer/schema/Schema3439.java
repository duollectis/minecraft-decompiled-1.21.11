package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Схема версии 3439 (Minecraft 1.20 — Trails & Tales).
 * <p>
 * Обновляет тип данных таблички ({@code minecraft:sign}) для поддержки
 * нового двустороннего формата: теперь табличка хранит отдельные тексты
 * для лицевой ({@code front_text}) и обратной ({@code back_text}) сторон,
 * каждая из которых содержит поля {@code messages} и {@code filtered_messages}.
 * Шаблон вынесен в статический метод {@link #createSignTemplate(Schema)}
 * для повторного использования в {@link Schema3439_1}.
 */
public class Schema3439 extends IdentifierNormalizingSchema {

	public Schema3439(int versionKey, Schema parent) {
		super(versionKey, parent);
	}

	/**
	 * Создаёт шаблон типа данных для двусторонней таблички.
	 * Описывает структуру с лицевым и обратным текстом, каждый из которых
	 * содержит основные и отфильтрованные сообщения в виде списков текстовых компонентов.
	 *
	 * @param schema текущая схема DataFixer для разрешения ссылок на типы
	 * @return шаблон типа данных таблички
	 */
	public static TypeTemplate createSignTemplate(Schema schema) {
		return DSL.optionalFields(
			"front_text",
			DSL.optionalFields(
				"messages",
				DSL.list(TypeReferences.TEXT_COMPONENT.in(schema)),
				"filtered_messages",
				DSL.list(TypeReferences.TEXT_COMPONENT.in(schema))
			),
			"back_text",
			DSL.optionalFields(
				"messages",
				DSL.list(TypeReferences.TEXT_COMPONENT.in(schema)),
				"filtered_messages",
				DSL.list(TypeReferences.TEXT_COMPONENT.in(schema))
			)
		);
	}

	@Override
	public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
		Map<String, Supplier<TypeTemplate>> blockEntityTypes = super.registerBlockEntities(schema);
		register(blockEntityTypes, "minecraft:sign", () -> createSignTemplate(schema));
		return blockEntityTypes;
	}
}
