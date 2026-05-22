package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

import java.util.Objects;
import java.util.Optional;

/**
 * Устанавливает поле {@code Damage} предмета кровати в значение 14 (красный цвет),
 * если оно равно 0 — это соответствует поведению до флаттенинга, когда красная кровать была дефолтной.
 */
public class BedItemColorFix extends DataFix {

	/** Цвет кровати по умолчанию (красный = 14) при отсутствии явного значения Damage. */
	private static final short DEFAULT_BED_COLOR = 14;

	public BedItemColorFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	@Override
	public TypeRewriteRule makeRule() {
		OpticFinder<Pair<String, String>> idFinder = DSL.fieldFinder(
			"id",
			DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
		);

		return fixTypeEverywhereTyped(
			"BedItemColorFix",
			getInputSchema().getType(TypeReferences.ITEM_STACK),
			typed -> {
				Optional<Pair<String, String>> id = typed.getOptional(idFinder);

				if (id.isEmpty() || !Objects.equals(id.get().getSecond(), "minecraft:bed")) {
					return typed;
				}

				Dynamic<?> dynamic = (Dynamic<?>) typed.get(DSL.remainderFinder());

				if (dynamic.get("Damage").asInt(0) != 0) {
					return typed;
				}

				return typed.set(DSL.remainderFinder(), dynamic.set("Damage", dynamic.createShort(DEFAULT_BED_COLOR)));
			}
		);
	}
}
