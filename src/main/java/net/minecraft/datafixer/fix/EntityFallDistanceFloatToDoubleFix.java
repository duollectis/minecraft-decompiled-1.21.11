package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;

/**
 * Конвертирует поле {@code FallDistance} (float) в {@code fall_distance} (double)
 * для указанного типа сущности. Применяется к нескольким типам данных через {@code typeReference}.
 */
public class EntityFallDistanceFloatToDoubleFix extends DataFix {

	private final TypeReference typeReference;

	public EntityFallDistanceFloatToDoubleFix(Schema outputSchema, TypeReference typeReference) {
		super(outputSchema, false);
		this.typeReference = typeReference;
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"EntityFallDistanceFloatToDoubleFixFor" + typeReference.typeName(),
				getOutputSchema().getType(typeReference),
				EntityFallDistanceFloatToDoubleFix::fixFallDistance
		);
	}

	private static Typed<?> fixFallDistance(Typed<?> typed) {
		return typed.update(
				DSL.remainderFinder(),
				dynamic -> dynamic.renameAndFixField(
						"FallDistance",
						"fall_distance",
						fallDynamic -> fallDynamic.createDouble(fallDynamic.asFloat(0.0F))
				)
		);
	}
}
