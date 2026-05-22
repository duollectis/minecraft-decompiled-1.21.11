package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

import java.util.Map;
import java.util.Optional;

/**
 * Конвертирует поле {@code CustomName} баннера-знамени (Ominous Banner) в компонент предмета
 * {@code minecraft:item_name}, добавляя также {@code minecraft:hide_additional_tooltip}.
 */
public class BannerCustomNameToItemNameFix extends DataFix {

	private static final String OMINOUS_BANNER_TRANSLATION_KEY = "block.minecraft.ominous_banner";

	public BannerCustomNameToItemNameFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public TypeRewriteRule makeRule() {
		Type<?> blockEntityType = getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
		TaggedChoiceType<?> choiceType = getInputSchema().findChoiceType(TypeReferences.BLOCK_ENTITY);
		OpticFinder<String> customNameFinder = (OpticFinder<String>) blockEntityType.findField("CustomName");
		OpticFinder<Pair<String, String>> textComponentFinder =
			(OpticFinder<Pair<String, String>>) DSL.typeFinder(getInputSchema().getType(TypeReferences.TEXT_COMPONENT));

		return fixTypeEverywhereTyped(
			"Banner entity custom_name to item_name component fix",
			blockEntityType,
			typed -> {
				Object entityId = ((Pair<?, ?>) typed.get(choiceType.finder())).getFirst();

				return entityId.equals("minecraft:banner")
					? fix(typed, textComponentFinder, customNameFinder)
					: typed;
			}
		);
	}

	private Typed<?> fix(
		Typed<?> typed,
		OpticFinder<Pair<String, String>> textComponentFinder,
		OpticFinder<String> customNameFinder
	) {
		Optional<String> customName = typed.getOptionalTyped(customNameFinder)
			.flatMap(inner -> inner.getOptional(textComponentFinder).map(Pair::getSecond));

		boolean isOminousBanner = customName
			.flatMap(TextFixes::getTranslate)
			.filter(OMINOUS_BANNER_TRANSLATION_KEY::equals)
			.isPresent();

		return isOminousBanner
			? Util.apply(
				typed,
				typed.getType(),
				dynamic -> {
					Dynamic<?> components = dynamic.createMap(
						Map.of(
							dynamic.createString("minecraft:item_name"), dynamic.createString(customName.get()),
							dynamic.createString("minecraft:hide_additional_tooltip"), dynamic.emptyMap()
						)
					);

					return dynamic.set("components", components).remove("CustomName");
				}
			)
			: typed;
	}
}
