package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Codec;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;

/**
 * Удаляет избыточные теги шансов выпадения {@code HandDropChances} и {@code ArmorDropChances},
 * если все их значения равны нулю — такие теги не несут смысловой нагрузки.
 */
public class EntityRedundantChanceTagsFix extends DataFix {

	private static final int HAND_SLOTS_COUNT = 2;
	private static final int ARMOR_SLOTS_COUNT = 4;
	private static final Codec<List<Float>> FLOAT_LIST_CODEC = Codec.FLOAT.listOf();

	public EntityRedundantChanceTagsFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	@Override
	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"EntityRedundantChanceTagsFix",
				getInputSchema().getType(TypeReferences.ENTITY),
				typed -> typed.update(DSL.remainderFinder(), entity -> {
					if (hasZeroDropChance(entity.get("HandDropChances"), HAND_SLOTS_COUNT)) {
						entity = entity.remove("HandDropChances");
					}

					if (hasZeroDropChance(entity.get("ArmorDropChances"), ARMOR_SLOTS_COUNT)) {
						entity = entity.remove("ArmorDropChances");
					}

					return entity;
				})
		);
	}

	private static boolean hasZeroDropChance(OptionalDynamic<?> listTag, int expectedLength) {
		return listTag.flatMap(FLOAT_LIST_CODEC::parse)
				.map(chances -> chances.size() == expectedLength
						&& chances.stream().allMatch(chance -> chance == 0.0F))
				.result()
				.orElse(false);
	}
}
