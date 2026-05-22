package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Удаляет тег {@code blending_data} из чанков Нижнего мира и Края, где смешивание
 * рельефа не применяется — данные актуальны только для Overworld.
 */
public class BlendingDataRemoveFromNetherEndFix extends DataFix {

	private static final String OVERWORLD_DIMENSION = "minecraft:overworld";

	public BlendingDataRemoveFromNetherEndFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@Override
	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getOutputSchema().getType(TypeReferences.CHUNK);

		return fixTypeEverywhereTyped(
			"BlendingDataRemoveFromNetherEndFix",
			chunkType,
			typed -> typed.update(
				DSL.remainderFinder(),
				chunk -> removeInapplicableBlendingData(chunk, chunk.get("__context"))
			)
		);
	}

	private static Dynamic<?> removeInapplicableBlendingData(Dynamic<?> chunk, OptionalDynamic<?> context) {
		boolean isOverworld = OVERWORLD_DIMENSION.equals(
			context.get("dimension").asString().result().orElse("")
		);

		return isOverworld ? chunk : chunk.remove("blending_data");
	}
}
