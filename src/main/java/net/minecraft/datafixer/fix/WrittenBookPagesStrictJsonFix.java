package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;

/**
 * {@code WrittenBookPagesStrictJsonFix}.
 */
public class WrittenBookPagesStrictJsonFix extends ItemNbtFix {

	public WrittenBookPagesStrictJsonFix(Schema outputSchema) {
		super(outputSchema, "WrittenBookPagesStrictJsonFix", string -> string.equals("minecraft:written_book"));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Typed<?> fix(Typed<?> typed) {
		Type<Pair<String, String>>
				type =
				(Type<Pair<String, String>>) this.getInputSchema().getType(TypeReferences.TEXT_COMPONENT);
		Type<?> type2 = this.getInputSchema().getType(TypeReferences.ITEM_STACK);
		OpticFinder<?> opticFinder = type2.findField("tag");
		OpticFinder<?> opticFinder2 = opticFinder.type().findField("pages");
		OpticFinder<Pair<String, String>> opticFinder3 = DSL.typeFinder(type);
		return typed.updateTyped(
				opticFinder2,
				typedx -> typedx.update(opticFinder3, pair -> pair.mapSecond(TextFixes::parseLenientJson))
		);
	}
}
