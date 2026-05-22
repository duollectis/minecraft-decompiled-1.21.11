package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Удаляет устаревшее поле {@code Color} из блок-сущности шалкерового ящика.
 * После флаттенинга цвет кодируется в самом ID блока, а не в NBT.
 */
public class BlockEntityShulkerBoxColorFix extends ChoiceFix {

	public BlockEntityShulkerBoxColorFix(Schema schema, boolean changesType) {
		super(schema, changesType, "BlockEntityShulkerBoxColorFix", TypeReferences.BLOCK_ENTITY, "minecraft:shulker_box");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), dynamic -> dynamic.remove("Color"));
	}
}
