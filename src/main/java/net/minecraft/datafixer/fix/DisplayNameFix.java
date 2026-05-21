package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;

/**
 * {@code DisplayNameFix}.
 */
public class DisplayNameFix extends DataFix {

	private final String name;
	private final TypeReference typeReference;

	public DisplayNameFix(Schema outputSchema, String name, TypeReference typeReference) {
		super(outputSchema, false);
		this.name = name;
		this.typeReference = typeReference;
	}

	@SuppressWarnings("unchecked")
	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(this.typeReference);
		OpticFinder<?> opticFinder = type.findField("DisplayName");
		OpticFinder<Pair<String, String>>
				opticFinder2 =
				(OpticFinder<Pair<String, String>>) DSL.typeFinder(this
						.getInputSchema()
						.getType(TypeReferences.TEXT_COMPONENT));
		return this.fixTypeEverywhereTyped(
				this.name,
				type,
				typed -> typed.updateTyped(
						opticFinder,
						typedx -> typedx.update(opticFinder2, pair -> pair.mapSecond(TextFixes::text))
				)
		);
	}
}
