package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.function.Function;

/**
 * Мигрирует устаревшее булево поле {@code player} стрел и трезубца в числовое поле {@code pickup}.
 * Значение 1 означает «может подобрать игрок», 0 — нельзя подобрать.
 */
public class ArrowPickupFix extends DataFix {

	public ArrowPickupFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
			"AbstractArrowPickupFix",
			getInputSchema().getType(TypeReferences.ENTITY),
			this::update
		);
	}

	private Typed<?> update(Typed<?> typed) {
		typed = updateEntity(typed, "minecraft:arrow", ArrowPickupFix::fixPickup);
		typed = updateEntity(typed, "minecraft:spectral_arrow", ArrowPickupFix::fixPickup);
		return updateEntity(typed, "minecraft:trident", ArrowPickupFix::fixPickup);
	}

	private static Dynamic<?> fixPickup(Dynamic<?> arrowData) {
		if (arrowData.get("pickup").result().isPresent()) {
			return arrowData;
		}

		boolean canPickup = arrowData.get("player").asBoolean(true);
		return arrowData.set("pickup", arrowData.createByte((byte) (canPickup ? 1 : 0))).remove("player");
	}

	private Typed<?> updateEntity(Typed<?> typed, String choiceName, Function<Dynamic<?>, Dynamic<?>> updater) {
		Type<?> inputType = getInputSchema().getChoiceType(TypeReferences.ENTITY, choiceName);
		Type<?> outputType = getOutputSchema().getChoiceType(TypeReferences.ENTITY, choiceName);

		return typed.updateTyped(
			DSL.namedChoice(choiceName, inputType),
			outputType,
			inner -> inner.update(DSL.remainderFinder(), updater)
		);
	}
}
