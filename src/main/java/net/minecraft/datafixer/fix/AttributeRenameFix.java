package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;

import java.util.function.UnaryOperator;

/**
 * Базовый класс для переименования атрибутов в компонентах предметов и данных сущностей.
 * Применяет функцию {@code renamer} к полю {@code type} (в компонентах) и {@code id} (у сущностей).
 */
public class AttributeRenameFix extends DataFix {

	private final String name;
	private final UnaryOperator<String> renamer;

	public AttributeRenameFix(Schema outputSchema, String name, UnaryOperator<String> renamer) {
		super(outputSchema, false);
		this.name = name;
		this.renamer = renamer;
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return TypeRewriteRule.seq(
			fixTypeEverywhereTyped(
				name + " (Components)",
				getInputSchema().getType(TypeReferences.DATA_COMPONENTS),
				this::applyToComponents
			),
			new TypeRewriteRule[]{
				fixTypeEverywhereTyped(
					name + " (Entity)",
					getInputSchema().getType(TypeReferences.ENTITY),
					this::applyToEntity
				),
				fixTypeEverywhereTyped(
					name + " (Player)",
					getInputSchema().getType(TypeReferences.PLAYER),
					this::applyToEntity
				)
			}
		);
	}

	private Typed<?> applyToComponents(Typed<?> typed) {
		return typed.update(
			DSL.remainderFinder(),
			dynamic -> dynamic.update(
				"minecraft:attribute_modifiers",
				modifiers -> modifiers.update(
					"modifiers",
					list -> (Dynamic<?>) DataFixUtils.orElse(
						list.asStreamOpt()
							.result()
							.map(stream -> stream.map(this::applyToTypeField))
							.map(list::createList),
						list
					)
				)
			)
		);
	}

	private Typed<?> applyToEntity(Typed<?> typed) {
		return typed.update(
			DSL.remainderFinder(),
			dynamic -> dynamic.update(
				"attributes",
				attributes -> (Dynamic<?>) DataFixUtils.orElse(
					attributes.asStreamOpt()
						.result()
						.map(stream -> stream.map(this::applyToIdField))
						.map(attributes::createList),
					attributes
				)
			)
		);
	}

	private Dynamic<?> applyToIdField(Dynamic<?> dynamic) {
		return FixUtil.apply(dynamic, "id", renamer);
	}

	private Dynamic<?> applyToTypeField(Dynamic<?> dynamic) {
		return FixUtil.apply(dynamic, "type", renamer);
	}
}
