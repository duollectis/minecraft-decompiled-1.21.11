package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;

/**
 * Мигрирует шансы выпадения снаряжения из отдельных полей {@code ArmorDropChances},
 * {@code HandDropChances} и {@code body_armor_drop_chance} в единый объект {@code drop_chances}.
 * Слоты со значением по умолчанию ({@link #DEFAULT_DROP_CHANCE}) не записываются.
 */
public class DropChancesFormatFix extends DataFix {

	private static final List<String> ARMOR_SLOT_NAMES = List.of("feet", "legs", "chest", "head");
	private static final List<String> HAND_SLOT_NAMES = List.of("mainhand", "offhand");
	private static final float DEFAULT_DROP_CHANCE = 0.085F;

	public DropChancesFormatFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"DropChancesFormatFix",
				getInputSchema().getType(TypeReferences.ENTITY),
				typed -> typed.update(
						DSL.remainderFinder(),
						dynamic -> {
							List<Float> armorChances = readDropChanceList(dynamic.get("ArmorDropChances"));
							List<Float> handChances = readDropChanceList(dynamic.get("HandDropChances"));
							float bodyChance = dynamic.get("body_armor_drop_chance")
									.asNumber()
									.result()
									.map(Number::floatValue)
									.orElse(DEFAULT_DROP_CHANCE);

							dynamic = dynamic
									.remove("ArmorDropChances")
									.remove("HandDropChances")
									.remove("body_armor_drop_chance");

							Dynamic<?> dropChances = dynamic.emptyMap();
							dropChances = applyDropChances(dropChances, armorChances, ARMOR_SLOT_NAMES);
							dropChances = applyDropChances(dropChances, handChances, HAND_SLOT_NAMES);

							if (bodyChance != DEFAULT_DROP_CHANCE) {
								dropChances = dropChances.set("body", dynamic.createFloat(bodyChance));
							}

							return dropChances.equals(dynamic.emptyMap())
									? dynamic
									: dynamic.set("drop_chances", dropChances);
						}
				)
		);
	}

	private static Dynamic<?> applyDropChances(Dynamic<?> target, List<Float> chances, List<String> slotNames) {
		for (int i = 0; i < slotNames.size() && i < chances.size(); i++) {
			float chance = chances.get(i);

			if (chance != DEFAULT_DROP_CHANCE) {
				target = target.set(slotNames.get(i), target.createFloat(chance));
			}
		}

		return target;
	}

	private static List<Float> readDropChanceList(OptionalDynamic<?> optionalDynamic) {
		return optionalDynamic.asStream().map(entry -> entry.asFloat(DEFAULT_DROP_CHANCE)).toList();
	}
}
