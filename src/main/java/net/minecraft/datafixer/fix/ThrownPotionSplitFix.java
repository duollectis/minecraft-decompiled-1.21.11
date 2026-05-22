package net.minecraft.datafixer.fix;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.function.Supplier;

/**
 * Исправляет данные в формате DataFixer.
 */
public class ThrownPotionSplitFix extends EntityTransformFix {

	private final Supplier<ThrownPotionSplitFix.PotionItemFinder> potionItemFinderSupplier = Suppliers.memoize(
			() -> {
				Type<?> type = getInputSchema().getChoiceType(TypeReferences.ENTITY, "minecraft:potion");
				Type<?> type2 = FixUtil.withTypeChanged(
						type,
						getInputSchema().getType(TypeReferences.ENTITY),
						getOutputSchema().getType(TypeReferences.ENTITY)
				);
				OpticFinder<?> opticFinder = type2.findField("Item");
				OpticFinder<Pair<String, String>> opticFinder2 = DSL.fieldFinder(
						"id",
						DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
				);
				return new ThrownPotionSplitFix.PotionItemFinder(opticFinder, opticFinder2);
			}
	);

	public ThrownPotionSplitFix(Schema schema) {
		super("ThrownPotionSplitFix", schema, true);
	}

	@Override
	protected Pair<String, Typed<?>> transform(String choice, Typed<?> entityTyped) {
		if (!choice.equals("minecraft:potion")) {
			return Pair.of(choice, entityTyped);
		}
		else {
			String string = this.potionItemFinderSupplier.get().getItemId(entityTyped);
			return "minecraft:lingering_potion".equals(string)
			       ? Pair.of("minecraft:lingering_potion", entityTyped)
			       : Pair.of("minecraft:splash_potion", entityTyped);
		}
	}

	/**
 * Класс PotionItemFinder.
 */
	record PotionItemFinder(OpticFinder<?> itemFinder, OpticFinder<Pair<String, String>> itemIdFinder) {

		public String getItemId(Typed<?> typed) {
			return typed.getOptionalTyped(this.itemFinder)
			            .flatMap(typedx -> typedx.getOptional(this.itemIdFinder))
			            .<String>map(Pair::getSecond)
			            .map(IdentifierNormalizingSchema::normalize)
			            .orElse("");
		}
	}
}
