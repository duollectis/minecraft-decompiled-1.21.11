package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Нормализует поле {@code type} лосося: допустимо только значение {@code large} или {@code medium}.
 * Все нераспознанные значения заменяются на {@code medium}.
 */
public class EntitySalmonSizeFix extends ChoiceFix {

	public EntitySalmonSizeFix(Schema outputSchema) {
		super(outputSchema, false, "EntitySalmonSizeFix", TypeReferences.ENTITY, "minecraft:salmon");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), dynamic -> {
			String sizeType = dynamic.get("type").asString("medium");

			return sizeType.equals("large")
					? dynamic
					: dynamic.set("type", dynamic.createString("medium"));
		});
	}
}
