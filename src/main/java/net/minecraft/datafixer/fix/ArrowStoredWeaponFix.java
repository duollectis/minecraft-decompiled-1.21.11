package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * {@code ArrowStoredWeaponFix}.
 */
public class ArrowStoredWeaponFix extends DataFix {

	public ArrowStoredWeaponFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> type = this.getInputSchema().getType(TypeReferences.ENTITY);
		Type<?> type2 = this.getOutputSchema().getType(TypeReferences.ENTITY);
		return this.fixTypeEverywhereTyped(
				"Fix Arrow stored weapon",
				type,
				type2,
				FixUtil.compose(this.fixFor("minecraft:arrow"), this.fixFor("minecraft:spectral_arrow"))
		);
	}

	private Function<Typed<?>, Typed<?>> fixFor(String entityId) {
		Type<?> type = this.getInputSchema().getChoiceType(TypeReferences.ENTITY, entityId);
		Type<?> type2 = this.getOutputSchema().getChoiceType(TypeReferences.ENTITY, entityId);
		return createEntityFixer(entityId, type, type2);
	}

	private static <T> Function<Typed<?>, Typed<?>> createEntityFixer(String name, Type<?> type, Type<T> type2) {
		OpticFinder<?> opticFinder = DSL.namedChoice(name, type);
		return typed -> typed.updateTyped(
				opticFinder,
				type2,
				typedx -> Util.apply(typedx, type2, UnaryOperator.identity())
		);
	}
}
