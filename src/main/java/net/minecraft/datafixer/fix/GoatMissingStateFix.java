package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Добавляет отсутствующие поля {@code HasLeftHorn} и {@code HasRightHorn}
 * козлу, устанавливая оба в {@code true} по умолчанию.
 * Поля могли отсутствовать в мирах, созданных до их введения.
 */
public class GoatMissingStateFix extends ChoiceFix {

	public GoatMissingStateFix(Schema outputSchema) {
		super(outputSchema, false, "EntityGoatMissingStateFix", TypeReferences.ENTITY, "minecraft:goat");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(
			DSL.remainderFinder(),
			goat -> goat
				.set("HasLeftHorn", goat.createBoolean(true))
				.set("HasRightHorn", goat.createBoolean(true))
		);
	}
}
