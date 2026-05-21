package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;

import java.util.function.UnaryOperator;

/**
 * {@code AttributeRenameFix}.
 */
public class AttributeRenameFix extends DataFix {

	private final String name;
	private final UnaryOperator<String> renamer;

	public AttributeRenameFix(Schema outputSchema, String name, UnaryOperator<String> renamer) {
		super(outputSchema, false);
		this.name = name;
		this.renamer = renamer;
	}

	protected TypeRewriteRule makeRule() {
		return TypeRewriteRule.seq(
				this.fixTypeEverywhereTyped(
						this.name + " (Components)",
						this.getInputSchema().getType(TypeReferences.DATA_COMPONENTS),
						this::applyToComponents
				),
				new TypeRewriteRule[]{
						this.fixTypeEverywhereTyped(
								this.name + " (Entity)",
								this.getInputSchema().getType(TypeReferences.ENTITY),
								this::applyToEntity
						),
						this.fixTypeEverywhereTyped(
								this.name + " (Player)",
								this.getInputSchema().getType(TypeReferences.PLAYER),
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
						dynamicx -> dynamicx.update(
								"modifiers",
								dynamicxx -> (Dynamic) DataFixUtils.orElse(
										dynamicxx
												.asStreamOpt()
												.result()
												.map(stream -> stream.map(this::applyToTypeField))
												.map(dynamicxx::createList), dynamicxx
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
						dynamicx -> (Dynamic) DataFixUtils.orElse(
								dynamicx
										.asStreamOpt()
										.result()
										.map(stream -> stream.map(this::applyToIdField))
										.map(dynamicx::createList), dynamicx
						)
				)
		);
	}

	private Dynamic<?> applyToIdField(Dynamic<?> dynamic) {
		return FixUtil.apply(dynamic, "id", this.renamer);
	}

	private Dynamic<?> applyToTypeField(Dynamic<?> dynamic) {
		return FixUtil.apply(dynamic, "type", this.renamer);
	}
}
