package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.function.UnaryOperator;

/**
 * Исправляет данные в формате DataFixer.
 */
public class RenameEntityAttributesFix extends DataFix {

	private final String description;
	private final UnaryOperator<String> renames;

	public RenameEntityAttributesFix(Schema outputSchema, String description, UnaryOperator<String> renames) {
		super(outputSchema, false);
		this.description = description;
		this.renames = renames;
	}

	protected TypeRewriteRule makeRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.ITEM_STACK);
		OpticFinder<?> opticFinder = type.findField("tag");
		return TypeRewriteRule.seq(
				fixTypeEverywhereTyped(
						this.description + " (ItemStack)",
						type,
						itemStackTyped -> itemStackTyped.updateTyped(opticFinder, this::updateAttributeModifiers)
				),
				new TypeRewriteRule[]{
						fixTypeEverywhereTyped(
								this.description + " (Entity)",
								getInputSchema().getType(TypeReferences.ENTITY),
								this::updateEntityAttributes
						),
						fixTypeEverywhereTyped(
								this.description + " (Player)",
								getInputSchema().getType(TypeReferences.PLAYER),
								this::updateEntityAttributes
						)
				}
		);
	}

	private Dynamic<?> updateAttributeName(Dynamic<?> attributeNameDynamic) {
		return (Dynamic<?>) DataFixUtils.orElse(
				attributeNameDynamic.asString().result().map(this.renames).map(attributeNameDynamic::createString),
				attributeNameDynamic
		);
	}

	private Typed<?> updateAttributeModifiers(Typed<?> tagTyped) {
		return tagTyped.update(
				DSL.remainderFinder(),
				tagDynamic -> tagDynamic.update(
						"AttributeModifiers",
						attributeModifiersDynamic -> (Dynamic) DataFixUtils.orElse(
								attributeModifiersDynamic.asStreamOpt()
								                         .result()
								                         .map(
										                         attributeModifiers -> attributeModifiers.map(
												                         attributeModifierDynamic -> attributeModifierDynamic.update(
														                         "AttributeName",
														                         this::updateAttributeName
												                         )
										                         )
								                         )
								                         .map(attributeModifiersDynamic::createList),
								attributeModifiersDynamic
						)
				)
		);
	}

	private Typed<?> updateEntityAttributes(Typed<?> entityTyped) {
		return entityTyped.update(
				DSL.remainderFinder(),
				entityDynamic -> entityDynamic.update(
						"Attributes",
						attributesDynamic -> (Dynamic) DataFixUtils.orElse(
								attributesDynamic.asStreamOpt()
								                 .result()
								                 .map(attributes -> attributes.map(attributeDynamic -> attributeDynamic.update(
										                 "Name",
										                 this::updateAttributeName
								                 )))
								                 .map(attributesDynamic::createList),
								attributesDynamic
						)
				)
		);
	}
}
