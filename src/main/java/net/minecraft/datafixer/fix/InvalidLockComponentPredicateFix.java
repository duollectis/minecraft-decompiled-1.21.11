package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Исправляет данные в формате DataFixer.
 */
public class InvalidLockComponentPredicateFix extends ComponentFix {

	private static final Optional<String> DOUBLE_QUOTES = Optional.of("\"\"");

	public InvalidLockComponentPredicateFix(Schema outputSchema) {
		super(outputSchema, "InvalidLockComponentPredicateFix", "minecraft:lock");
	}

	@Override
	protected <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> dynamic) {
		return validateLock(dynamic);
	}

	public static <T> @Nullable Dynamic<T> validateLock(Dynamic<T> dynamic) {
		return isLockInvalid(dynamic) ? null : dynamic;
	}

	private static <T> boolean isLockInvalid(Dynamic<T> dynamic) {
		return hasMatchingKey(
				dynamic,
				"components",
				componentsDynamic -> hasMatchingKey(
						componentsDynamic,
						"minecraft:custom_name",
						customNameDynamic -> customNameDynamic.asString().result().equals(DOUBLE_QUOTES)
				)
		);
	}

	private static <T> boolean hasMatchingKey(Dynamic<T> dynamic, String key, Predicate<Dynamic<T>> predicate) {
		Optional<Map<Dynamic<T>, Dynamic<T>>> optional = dynamic.getMapValues().result();
		return !optional.isEmpty() && optional.get().size() == 1 ? dynamic
		                                                           .get(key)
		                                                           .result()
		                                                           .filter(predicate)
		                                                           .isPresent() : false;
	}
}
