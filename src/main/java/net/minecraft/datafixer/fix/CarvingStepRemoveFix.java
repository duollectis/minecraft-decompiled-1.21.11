package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * Удаляет устаревший тег {@code CarvingMasks} из чанков, перенося маску воздуха
 * в новое поле {@code carving_mask}. Тег {@code AIR} внутри {@code CarvingMasks}
 * соответствует маске для воздушного прохода пещерного генератора.
 */
public class CarvingStepRemoveFix extends DataFix {

	public CarvingStepRemoveFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"CarvingStepRemoveFix",
				getInputSchema().getType(TypeReferences.CHUNK),
				CarvingStepRemoveFix::removeCarvingMasks
		);
	}

	private static Typed<?> removeCarvingMasks(Typed<?> typed) {
		return typed.update(
				DSL.remainderFinder(), dynamic -> {
					Dynamic<?> result = dynamic;
					Optional<? extends Dynamic<?>> carvingMasks = dynamic.get("CarvingMasks").result();

					if (carvingMasks.isPresent()) {
						Optional<? extends Dynamic<?>> airMask = carvingMasks.get().get("AIR").result();

						if (airMask.isPresent()) {
							result = dynamic.set("carving_mask", airMask.get());
						}
					}

					return result.remove("CarvingMasks");
				}
		);
	}
}
