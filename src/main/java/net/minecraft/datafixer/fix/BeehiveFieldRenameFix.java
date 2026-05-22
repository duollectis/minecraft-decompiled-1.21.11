package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;

/**
 * Переименовывает поля блок-сущности улья: удаляет {@code EntityData} пчёл,
 * переименовывает {@code TicksInHive} → {@code ticks_in_hive} и
 * {@code MinOccupationTicks} → {@code min_ticks_in_hive}.
 */
public class BeehiveFieldRenameFix extends DataFix {

	public BeehiveFieldRenameFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	@Override
	public TypeRewriteRule makeRule() {
		Type<?> beehiveInputType = getInputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, "minecraft:beehive");
		OpticFinder<?> beehiveFinder = DSL.namedChoice("minecraft:beehive", beehiveInputType);
		ListType<?> beesListType = (ListType<?>) beehiveInputType.findFieldType("Bees");
		Type<?> beeType = beesListType.getElement();
		OpticFinder<?> beesListFinder = DSL.fieldFinder("Bees", beesListType);
		OpticFinder<?> beeFinder = DSL.typeFinder(beeType);
		Type<?> blockEntityInputType = getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
		Type<?> blockEntityOutputType = getOutputSchema().getType(TypeReferences.BLOCK_ENTITY);

		return fixTypeEverywhereTyped(
			"BeehiveFieldRenameFix",
			blockEntityInputType,
			blockEntityOutputType,
			typed -> FixUtil.withType(
				blockEntityOutputType,
				typed.updateTyped(
					beehiveFinder,
					beehive -> beehive
						.update(DSL.remainderFinder(), this::removeBeesField)
						.updateTyped(
							beesListFinder,
							beesList -> beesList.updateTyped(
								beeFinder,
								bee -> bee.update(DSL.remainderFinder(), this::renameFields)
							)
						)
				)
			)
		);
	}

	private Dynamic<?> removeBeesField(Dynamic<?> dynamic) {
		return dynamic.remove("Bees");
	}

	private Dynamic<?> renameFields(Dynamic<?> dynamic) {
		return dynamic.remove("EntityData")
			.renameField("TicksInHive", "ticks_in_hive")
			.renameField("MinOccupationTicks", "min_ticks_in_hive");
	}
}
