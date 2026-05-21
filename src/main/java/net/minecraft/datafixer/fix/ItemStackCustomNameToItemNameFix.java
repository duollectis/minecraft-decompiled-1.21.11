package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@code ItemStackCustomNameToItemNameFix}.
 */
public class ItemStackCustomNameToItemNameFix extends DataFix {

	private static final Set<String> EXPLORER_MAP_NAMES = Set.of(
			"filled_map.buried_treasure",
			"filled_map.explorer_jungle",
			"filled_map.explorer_swamp",
			"filled_map.mansion",
			"filled_map.monument",
			"filled_map.trial_chambers",
			"filled_map.village_desert",
			"filled_map.village_plains",
			"filled_map.village_savanna",
			"filled_map.village_snowy",
			"filled_map.village_taiga"
	);

	public ItemStackCustomNameToItemNameFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	public final TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.ITEM_STACK);
		OpticFinder<Pair<String, String>> opticFinder = DSL.fieldFinder(
				"id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);
		OpticFinder<?> opticFinder2 = type.findField("components");
		return this.fixTypeEverywhereTyped(
				"ItemStack custom_name to item_name component fix",
				type,
				itemStackTyped -> {
					Optional<Pair<String, String>> optional = itemStackTyped.getOptional(opticFinder);
					Optional<String> optional2 = optional.map(Pair::getSecond);
					if (optional2.filter(itemId -> itemId.equals("minecraft:white_banner")).isPresent()) {
						return itemStackTyped.updateTyped(
								opticFinder2,
								ItemStackCustomNameToItemNameFix::fixOminousBanner
						);
					}
					else {
						return optional2.filter(itemId -> itemId.equals("minecraft:filled_map")).isPresent()
						       ? itemStackTyped.updateTyped(
								opticFinder2,
								ItemStackCustomNameToItemNameFix::fixExplorerMaps
						)
						       : itemStackTyped;
					}
				}
		);
	}

	private static <T> Typed<T> fixExplorerMaps(Typed<T> typed) {
		return fix(typed, EXPLORER_MAP_NAMES::contains);
	}

	private static <T> Typed<T> fixOminousBanner(Typed<T> typed) {
		return fix(typed, name -> name.equals("block.minecraft.ominous_banner"));
	}

	private static <T> Typed<T> fix(Typed<T> typed, Predicate<String> namePredicate) {
		return Util.apply(
				typed, typed.getType(), dynamic -> {
					OptionalDynamic<?> optionalDynamic = dynamic.get("minecraft:custom_name");
					Optional<String>
							optional =
							optionalDynamic.asString().result().flatMap(TextFixes::getTranslate).filter(namePredicate);
					return optional.isPresent() ? dynamic.renameField("minecraft:custom_name", "minecraft:item_name")
					                            : dynamic;
				}
		);
	}
}
