package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * {@code BlockEntityKeepPackedFix}.
 */
public class BlockEntityKeepPackedFix extends ChoiceFix {

	public BlockEntityKeepPackedFix(Schema schema, boolean bl) {
		super(schema, bl, "BlockEntityKeepPacked", TypeReferences.BLOCK_ENTITY, "DUMMY");
	}

	private static Dynamic<?> keepPacked(Dynamic<?> dynamic) {
		return dynamic.set("keepPacked", dynamic.createBoolean(true));
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), BlockEntityKeepPackedFix::keepPacked);
	}
}
