package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Устанавливает цвет шалкера по умолчанию (фиолетовый, индекс 10), если поле {@code Color} отсутствует.
 */
public class EntityShulkerColorFix extends ChoiceFix {

	private static final byte DEFAULT_PURPLE_COLOR = 10;

	public EntityShulkerColorFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType, "EntityShulkerColorFix", TypeReferences.ENTITY, "minecraft:shulker");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixShulkerColor);
	}

	private Dynamic<?> fixShulkerColor(Dynamic<?> shulker) {
		return shulker.get("Color").map(Dynamic::asNumber).result().isEmpty()
				? shulker.set("Color", shulker.createByte(DEFAULT_PURPLE_COLOR))
				: shulker;
	}
}
