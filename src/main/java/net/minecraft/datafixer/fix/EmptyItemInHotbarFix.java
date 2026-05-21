package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * {@code EmptyItemInHotbarFix}.
 */
public class EmptyItemInHotbarFix extends DataFix {

	public EmptyItemInHotbarFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@SuppressWarnings("unchecked")
	public TypeRewriteRule makeRule() {
		OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>>
				opticFinder =
				(OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>>) DSL.typeFinder(
						this.getInputSchema().getType(TypeReferences.ITEM_STACK)
				);
		return this.fixTypeEverywhereTyped(
				"EmptyItemInHotbarFix",
				this.getInputSchema().getType(TypeReferences.HOTBAR),
				hotbarTyped -> hotbarTyped.update(
						opticFinder, itemPair -> itemPair.mapSecond(pairx -> {
							Optional<String>
									optional =
									((Either<Pair<String, String>, Unit>) pairx.getFirst())
											.left()
											.map(p -> p.getSecond());
							Dynamic<?> dynamic = (Dynamic<?>) ((Pair<?, ?>) pairx.getSecond()).getSecond();
							boolean isEmpty = optional.isEmpty() || optional.get().equals("minecraft:air");
							boolean hasNoCount = dynamic.get("Count").asInt(0) <= 0;
							return isEmpty || hasNoCount
							       ? Pair.of(
									Either.right(Unit.INSTANCE),
									Pair.of(Either.right(Unit.INSTANCE), dynamic.emptyMap())
							)
							       : pairx;
						})
				)
		);
	}
}
