package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Конвертирует блок-стейты в шаблонах структур из устаревшего числового формата
 * в новый именованный формат через {@link BlockStateFlattening#lookupState}.
 */
public class BlockStateStructureTemplateFix extends DataFix {

	public BlockStateStructureTemplateFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"BlockStateStructureTemplateFix",
				getInputSchema().getType(TypeReferences.BLOCK_STATE),
				typed -> typed.update(DSL.remainderFinder(), BlockStateFlattening::lookupState)
		);
	}
}
