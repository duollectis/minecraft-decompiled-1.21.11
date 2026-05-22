package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет цвет ошейника волка: в старом формате цвет хранился в инвертированном
 * виде (15 - реальный цвет), поэтому здесь применяется обратное преобразование.
 */
public class EntityWolfColorFix extends ChoiceFix {

	private static final int MAX_COLOR_INDEX = 15;

	public EntityWolfColorFix(Schema schema, boolean changesType) {
		super(schema, changesType, "EntityWolfColorFix", TypeReferences.ENTITY, "minecraft:wolf");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), this::fixCollarColor);
	}

	private Dynamic<?> fixCollarColor(Dynamic<?> wolf) {
		return wolf.update(
			"CollarColor",
			color -> color.createByte((byte) (MAX_COLOR_INDEX - color.asInt(0)))
		);
	}
}
