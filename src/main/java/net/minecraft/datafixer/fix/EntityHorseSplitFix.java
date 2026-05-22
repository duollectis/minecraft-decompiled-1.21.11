package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

import java.util.Objects;

/**
 * Разделяет старый тип {@code EntityHorse} на отдельные типы лошадей по полю {@code Type}:
 * {@code Horse}, {@code Donkey}, {@code Mule}, {@code ZombieHorse}, {@code SkeletonHorse}.
 */
public class EntityHorseSplitFix extends EntityTransformFix {

	public EntityHorseSplitFix(Schema outputSchema, boolean changesType) {
		super("EntityHorseSplitFix", outputSchema, changesType);
	}

	@Override
	protected Pair<String, Typed<?>> transform(String choice, Typed<?> entityTyped) {
		if (!Objects.equals("EntityHorse", choice)) {
			return Pair.of(choice, entityTyped);
		}

		Dynamic<?> entityData = (Dynamic<?>) entityTyped.get(DSL.remainderFinder());
		int horseType = entityData.get("Type").asInt(0);

		String newEntityId = switch (horseType) {
			case 1 -> "Donkey";
			case 2 -> "Mule";
			case 3 -> "ZombieHorse";
			case 4 -> "SkeletonHorse";
			default -> "Horse";
		};

		Type<?> outputType = (Type<?>) getOutputSchema()
				.findChoiceType(TypeReferences.ENTITY)
				.types()
				.get(newEntityId);

		return Pair.of(newEntityId, Util.apply(entityTyped, outputType, d -> d.remove("Type")));
	}
}
