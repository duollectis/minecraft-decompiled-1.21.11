package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.TypeReferences;

/**
 * Конвертирует тип блок-энтити {@code minecraft:decorated_pot} в новую схему
 * (переименование полей обрабатывается на уровне схемы).
 */
public class DecoratedPotFieldRenameFix extends DataFix {

	private static final String DECORATED_POT_ID = "minecraft:decorated_pot";

	public DecoratedPotFieldRenameFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> inputType = getInputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, DECORATED_POT_ID);
		Type<?> outputType = getOutputSchema().getChoiceType(TypeReferences.BLOCK_ENTITY, DECORATED_POT_ID);
		return convertUnchecked("DecoratedPotFieldRenameFix", inputType, outputType);
	}
}
