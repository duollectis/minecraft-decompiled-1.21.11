package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.function.UnaryOperator;

/**
 * Переименовывает критерии конкретного достижения по заданному маппингу {@code renamer}.
 */
public class AdvancementCriteriaRenameFix extends DataFix {

	private final String description;
	private final String advancementId;
	private final UnaryOperator<String> renamer;

	public AdvancementCriteriaRenameFix(
			Schema outputSchema,
			String description,
			String advancementId,
			UnaryOperator<String> renamer
	) {
		super(outputSchema, false);
		this.description = description;
		this.advancementId = advancementId;
		this.renamer = renamer;
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				description,
				getInputSchema().getType(TypeReferences.ADVANCEMENTS),
				typed -> typed.update(DSL.remainderFinder(), this::renameAdvancementCriteria)
		);
	}

	private Dynamic<?> renameAdvancementCriteria(Dynamic<?> advancements) {
		return advancements.update(
				advancementId,
				advancement -> advancement.update(
						"criteria",
						criteria -> criteria.updateMapValues(
								pair -> pair.mapFirst(
										key -> (Dynamic) DataFixUtils.orElse(
												key.asString()
														.map(keyString -> key.createString(renamer.apply(keyString)))
														.result(),
												key
										)
								)
						)
				)
		);
	}
}
