package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;

/**
 * Исправляет угол поворота шалкера: при переходе на новый формат
 * первый элемент списка {@code Rotation} смещался на 180°, поэтому
 * здесь вычитается 180° для корректного отображения ориентации.
 */
public class EntityShulkerRotationFix extends ChoiceFix {

	public EntityShulkerRotationFix(Schema outputSchema) {
		super(outputSchema, false, "EntityShulkerRotationFix", TypeReferences.ENTITY, "minecraft:shulker");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixRotation);
	}

	private Dynamic<?> fixRotation(Dynamic<?> shulker) {
		List<Double> rotationValues = shulker.get("Rotation").asList(d -> d.asDouble(180.0));

		if (rotationValues.isEmpty()) {
			return shulker;
		}

		rotationValues.set(0, rotationValues.get(0) - 180.0);

		return shulker.set(
			"Rotation",
			shulker.createList(rotationValues.stream().map(shulker::createDouble))
		);
	}
}
