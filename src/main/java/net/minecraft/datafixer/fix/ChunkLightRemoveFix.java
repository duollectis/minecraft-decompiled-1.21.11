package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.TypeReferences;

/**
 * Удаляет устаревший флаг {@code isLightOn} из тега {@code Level} чанка.
 * После перехода на новый формат освещения этот флаг более не используется.
 */
public class ChunkLightRemoveFix extends DataFix {

	public ChunkLightRemoveFix(Schema schema, boolean changesType) {
		super(schema, changesType);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		Type<?> levelType = chunkType.findFieldType("Level");
		OpticFinder<?> levelFinder = DSL.fieldFinder("Level", levelType);

		return fixTypeEverywhereTyped(
				"ChunkLightRemoveFix",
				chunkType,
				getOutputSchema().getType(TypeReferences.CHUNK),
				chunkTyped -> chunkTyped.updateTyped(
						levelFinder,
						levelTyped -> levelTyped.update(
								DSL.remainderFinder(),
								levelDynamic -> levelDynamic.remove("isLightOn")
						)
				)
		);
	}
}
