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
 * Заменяет пустые или невалидные предметы в хотбаре (воздух или нулевой Count)
 * на пустые слоты, чтобы избежать ошибок при загрузке инвентаря.
 */
public class EmptyItemInHotbarFix extends DataFix {

	public EmptyItemInHotbarFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@SuppressWarnings("unchecked")
	public TypeRewriteRule makeRule() {
		OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>>
				itemFinder =
				(OpticFinder<Pair<String, Pair<Either<Pair<String, String>, Unit>, Pair<Either<?, Unit>, Dynamic<?>>>>>) DSL.typeFinder(
						getInputSchema().getType(TypeReferences.ITEM_STACK)
				);

		return fixTypeEverywhereTyped(
				"EmptyItemInHotbarFix",
				getInputSchema().getType(TypeReferences.HOTBAR),
				hotbarTyped -> hotbarTyped.update(
						itemFinder,
						itemPair -> itemPair.mapSecond(inner -> {
							Optional<String> itemId = ((Either<Pair<String, String>, Unit>) inner.getFirst())
									.left()
									.map(Pair::getSecond);
							Dynamic<?> nbt = (Dynamic<?>) ((Pair<?, ?>) inner.getSecond()).getSecond();
							boolean isEmpty = itemId.isEmpty() || "minecraft:air".equals(itemId.get());
							boolean hasNoCount = nbt.get("Count").asInt(0) <= 0;

							return isEmpty || hasNoCount
									? Pair.of(
											Either.right(Unit.INSTANCE),
											Pair.of(Either.right(Unit.INSTANCE), nbt.emptyMap())
									)
									: inner;
						})
				)
		);
	}
}
