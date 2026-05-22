package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет некорректный цвет шалкера: заменяет устаревший цветовой индекс {@code 10}
 * (пурпурный) на {@code 16} (бесцветный/дефолтный).
 */
public class ColorlessShulkerEntityFix extends ChoiceFix {

	private static final int OLD_PURPLE_COLOR = 10;
	private static final int DEFAULT_COLOR = 16;

	public ColorlessShulkerEntityFix(Schema schema, boolean changesType) {
		super(schema, changesType, "Colorless shulker entity fix", TypeReferences.ENTITY, "minecraft:shulker");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(
				DSL.remainderFinder(),
				shulker -> shulker.get("Color").asInt(0) == OLD_PURPLE_COLOR
						? shulker.set("Color", shulker.createByte((byte) DEFAULT_COLOR))
						: shulker
		);
	}
}
