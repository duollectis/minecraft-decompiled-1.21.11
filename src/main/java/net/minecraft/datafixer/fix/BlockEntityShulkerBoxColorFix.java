package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * {@code BlockEntityShulkerBoxColorFix}.
 */
public class BlockEntityShulkerBoxColorFix extends ChoiceFix {

	public BlockEntityShulkerBoxColorFix(Schema schema, boolean bl) {
		super(schema, bl, "BlockEntityShulkerBoxColorFix", TypeReferences.BLOCK_ENTITY, "minecraft:shulker_box");
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), dynamic -> dynamic.remove("Color"));
	}
}
