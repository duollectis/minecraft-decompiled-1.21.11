package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Устанавливает флаг {@code keepPacked = true} для блок-сущности, чтобы предотвратить
 * её распаковку при загрузке чанка до завершения генерации структуры.
 */
public class BlockEntityKeepPackedFix extends ChoiceFix {

	public BlockEntityKeepPackedFix(Schema schema, boolean changesType) {
		super(schema, changesType, "BlockEntityKeepPacked", TypeReferences.BLOCK_ENTITY, "DUMMY");
	}

	private static Dynamic<?> keepPacked(Dynamic<?> dynamic) {
		return dynamic.set("keepPacked", dynamic.createBoolean(true));
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(DSL.remainderFinder(), BlockEntityKeepPackedFix::keepPacked);
	}
}
