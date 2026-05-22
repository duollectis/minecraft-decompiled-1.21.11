package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

import java.util.Locale;
import java.util.function.Function;

/**
 * Базовый класс для фиксов, которые преобразуют тип сущности и/или её данные.
 * Использует {@link TaggedChoiceType} для разрешения входного и выходного типов
 * по строковому ключу (идентификатору сущности), затем делегирует трансформацию
 * в {@link #transform(String, Typed)}.
 */
public abstract class EntityTransformFix extends DataFix {

	protected final String name;

	public EntityTransformFix(String name, Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	@Override
	public TypeRewriteRule makeRule() {
		TaggedChoiceType<String> inputChoiceType =
			(TaggedChoiceType<String>) getInputSchema().findChoiceType(TypeReferences.ENTITY);
		TaggedChoiceType<String> outputChoiceType =
			(TaggedChoiceType<String>) getOutputSchema().findChoiceType(TypeReferences.ENTITY);

		Function<String, Type<?>> typeResolver = Util.memoize(entityId -> {
			Type<?> inputType = (Type<?>) inputChoiceType.types().get(entityId);
			return FixUtil.withTypeChanged(inputType, inputChoiceType, outputChoiceType);
		});

		return fixTypeEverywhere(
			name,
			inputChoiceType,
			outputChoiceType,
			dynamicOps -> pair -> {
				String entityId = (String) pair.getFirst();
				Type<?> resolvedType = typeResolver.apply(entityId);
				Pair<String, Typed<?>> result = transform(
					entityId,
					makeTyped(pair.getSecond(), dynamicOps, resolvedType)
				);

				String newEntityId = result.getFirst();
				Typed<?> newTyped = result.getSecond();
				Type<?> expectedOutputType = (Type<?>) outputChoiceType.types().get(newEntityId);

				if (expectedOutputType.equals(((Typed) newTyped).getType(), true, true)) {
					return Pair.of(newEntityId, ((Typed) newTyped).getValue());
				}

				throw new IllegalStateException(
					String.format(
						Locale.ROOT,
						"Dynamic type check failed: %s not equal to %s",
						expectedOutputType,
						((Typed) newTyped).getType()
					)
				);
			}
		);
	}

	private <A> Typed<A> makeTyped(Object value, DynamicOps<?> dynamicOps, Type<A> type) {
		return new Typed<>(type, dynamicOps, (A) value);
	}

	protected abstract Pair<String, Typed<?>> transform(String choice, Typed<?> entityTyped);
}
