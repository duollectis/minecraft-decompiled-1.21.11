package net.minecraft.loot.operator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;

/**
 * Оператор, ограничивающий целочисленное значение диапазоном [min, max].
 * Поддерживает динамические границы через {@link LootNumberProvider}.
 * Сериализуется компактно как одно число, если min == max == константа.
 */
public class BoundedIntUnaryOperator {

	private static final Codec<BoundedIntUnaryOperator> OPERATOR_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			LootNumberProviderTypes.CODEC.optionalFieldOf("min").forGetter(op -> Optional.ofNullable(op.min)),
			LootNumberProviderTypes.CODEC.optionalFieldOf("max").forGetter(op -> Optional.ofNullable(op.max))
		).apply(instance, BoundedIntUnaryOperator::new)
	);

	public static final Codec<BoundedIntUnaryOperator> CODEC = Codec.either(Codec.INT, OPERATOR_CODEC).xmap(
		either -> either.map(BoundedIntUnaryOperator::create, Function.identity()),
		operator -> {
			OptionalInt constant = operator.getConstantValue();
			return constant.isPresent()
				? Either.left(constant.getAsInt())
				: Either.right(operator);
		}
	);

	private final @Nullable LootNumberProvider min;
	private final @Nullable LootNumberProvider max;
	private final BoundedIntUnaryOperator.Applier applier;
	private final BoundedIntUnaryOperator.Tester tester;

	public Set<ContextParameter<?>> getRequiredParameters() {
		Builder<ContextParameter<?>> builder = ImmutableSet.builder();

		if (min != null) {
			builder.addAll(min.getAllowedParameters());
		}

		if (max != null) {
			builder.addAll(max.getAllowedParameters());
		}

		return builder.build();
	}

	private BoundedIntUnaryOperator(Optional<LootNumberProvider> min, Optional<LootNumberProvider> max) {
		this(min.orElse(null), max.orElse(null));
	}

	private BoundedIntUnaryOperator(@Nullable LootNumberProvider min, @Nullable LootNumberProvider max) {
		this.min = min;
		this.max = max;

		if (min == null) {
			if (max == null) {
				this.applier = (context, value) -> value;
				this.tester = (context, value) -> true;
			} else {
				this.applier = (context, value) -> Math.min(max.nextInt(context), value);
				this.tester = (context, value) -> value <= max.nextInt(context);
			}
		} else if (max == null) {
			this.applier = (context, value) -> Math.max(min.nextInt(context), value);
			this.tester = (context, value) -> value >= min.nextInt(context);
		} else {
			this.applier = (context, value) -> MathHelper.clamp(value, min.nextInt(context), max.nextInt(context));
			this.tester = (context, value) -> value >= min.nextInt(context) && value <= max.nextInt(context);
		}
	}

	public static BoundedIntUnaryOperator create(int value) {
		ConstantLootNumberProvider constant = ConstantLootNumberProvider.create(value);
		return new BoundedIntUnaryOperator(Optional.of(constant), Optional.of(constant));
	}

	public static BoundedIntUnaryOperator create(int min, int max) {
		return new BoundedIntUnaryOperator(
			Optional.of(ConstantLootNumberProvider.create(min)),
			Optional.of(ConstantLootNumberProvider.create(max))
		);
	}

	public static BoundedIntUnaryOperator createMin(int min) {
		return new BoundedIntUnaryOperator(Optional.of(ConstantLootNumberProvider.create(min)), Optional.empty());
	}

	public static BoundedIntUnaryOperator createMax(int max) {
		return new BoundedIntUnaryOperator(Optional.empty(), Optional.of(ConstantLootNumberProvider.create(max)));
	}

	public int apply(LootContext context, int value) {
		return applier.apply(context, value);
	}

	public boolean test(LootContext context, int value) {
		return tester.test(context, value);
	}

	private OptionalInt getConstantValue() {
		if (!Objects.equals(min, max) || !(min instanceof ConstantLootNumberProvider constant)) {
			return OptionalInt.empty();
		}

		return Math.floor(constant.value()) == constant.value()
			? OptionalInt.of((int) constant.value())
			: OptionalInt.empty();
	}

	/** Функциональный интерфейс для применения ограничения к значению. */
	@FunctionalInterface
	interface Applier {

		int apply(LootContext context, int value);
	}

	/** Функциональный интерфейс для проверки попадания значения в диапазон. */
	@FunctionalInterface
	interface Tester {

		boolean test(LootContext context, int value);
	}
}
