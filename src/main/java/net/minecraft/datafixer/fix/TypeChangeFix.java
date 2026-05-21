package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.FixUtil;

/**
 * {@code TypeChangeFix}.
 */
public class TypeChangeFix extends ChoiceFix {

	public TypeChangeFix(Schema outputSchema, String name, TypeReference type, String choiceName) {
		super(outputSchema, true, name, type, choiceName);
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		Type<?> type = this.getOutputSchema().getChoiceType(this.type, this.choiceName);
		return FixUtil.withType(type, inputTyped);
	}
}
