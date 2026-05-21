package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * {@code OptionsForceVBOFix}.
 */
public class OptionsForceVBOFix extends DataFix {

	public OptionsForceVBOFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	public TypeRewriteRule makeRule() {
		return this.fixTypeEverywhereTyped(
				"OptionsForceVBOFix",
				this.getInputSchema().getType(TypeReferences.OPTIONS),
				optionsTyped -> optionsTyped.update(
						DSL.remainderFinder(),
						optionsDynamic -> optionsDynamic.set("useVbo", optionsDynamic.createString("true"))
				)
		);
	}
}
