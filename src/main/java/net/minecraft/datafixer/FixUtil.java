package net.minecraft.datafixer;

import com.mojang.datafixers.*;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Util;

import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Утилитарный класс с вспомогательными методами для DataFixer-фиксов.
 * Содержит инструменты для работы с BlockPos, BlockState, типами и цветами.
 */
public class FixUtil {

	/**
	 * Конвертирует устаревший формат BlockPos (отдельные поля X/Y/Z) в массив int[3].
	 * Если хотя бы одно из полей отсутствует — возвращает исходный Dynamic без изменений.
	 */
	public static Dynamic<?> fixBlockPos(Dynamic<?> dynamic) {
		Optional<Number> x = dynamic.get("X").asNumber().result();
		Optional<Number> y = dynamic.get("Y").asNumber().result();
		Optional<Number> z = dynamic.get("Z").asNumber().result();

		return x.isPresent() && y.isPresent() && z.isPresent()
			? createBlockPos(dynamic, x.get().intValue(), y.get().intValue(), z.get().intValue())
			: dynamic;
	}

	/**
	 * Объединяет три отдельных поля координат в одно поле BlockPos (массив int[3]),
	 * удаляя исходные поля xKey, yKey, zKey.
	 */
	public static Dynamic<?> consolidateBlockPos(
		Dynamic<?> dynamic,
		String xKey,
		String yKey,
		String zKey,
		String newPosKey
	) {
		Optional<Number> x = dynamic.get(xKey).asNumber().result();
		Optional<Number> y = dynamic.get(yKey).asNumber().result();
		Optional<Number> z = dynamic.get(zKey).asNumber().result();

		return x.isPresent() && y.isPresent() && z.isPresent()
			? dynamic.remove(xKey)
				.remove(yKey)
				.remove(zKey)
				.set(newPosKey, createBlockPos(dynamic, x.get().intValue(), y.get().intValue(), z.get().intValue()))
			: dynamic;
	}

	public static Dynamic<?> createBlockPos(Dynamic<?> dynamic, int x, int y, int z) {
		return dynamic.createIntList(IntStream.of(x, y, z));
	}

	@SuppressWarnings("unchecked")
	public static <T, R> Typed<R> withType(Type<R> type, Typed<T> typed) {
		return new Typed<>(type, (DynamicOps<Object>) typed.getOps(), (R) typed.getValue());
	}

	@SuppressWarnings("unchecked")
	public static <T> Typed<T> withType(Type<T> type, Object value, DynamicOps<?> ops) {
		return new Typed<>(type, (DynamicOps<Object>) ops, (T) value);
	}

	/**
	 * Создаёт новый тип, заменяя вхождения oldType на newType во всём дереве типов.
	 * Используется при изменении схемы данных без реального преобразования значений.
	 */
	public static Type<?> withTypeChanged(Type<?> type, Type<?> oldType, Type<?> newType) {
		return type.all(typeChangingRule(oldType, newType), true, false).view().newType();
	}

	private static <A, B> TypeRewriteRule typeChangingRule(Type<A> oldType, Type<B> newType) {
		RewriteResult<A, B> rewriteResult = RewriteResult.create(
			View.create(
				"Patcher", oldType, newType, ops -> object -> {
					throw new UnsupportedOperationException();
				}
			),
			new BitSet()
		);

		return TypeRewriteRule.everywhere(
			TypeRewriteRule.ifSame(oldType, rewriteResult),
			PointFreeRule.nop(),
			true,
			true
		);
	}

	/**
	 * Компонует несколько функций преобразования Typed в одну, применяя их последовательно.
	 */
	@SafeVarargs
	public static <T> Function<Typed<?>, Typed<?>> compose(Function<Typed<?>, Typed<?>>... fixes) {
		return typed -> {
			for (Function<Typed<?>, Typed<?>> fix : fixes) {
				typed = fix.apply(typed);
			}

			return typed;
		};
	}

	/**
	 * Создаёт Dynamic, представляющий BlockState с заданным идентификатором и свойствами.
	 */
	public static Dynamic<?> createBlockState(String id, Map<String, String> properties) {
		Dynamic<NbtElement> dynamic = new Dynamic<>(NbtOps.INSTANCE, new NbtCompound());
		Dynamic<NbtElement> result = dynamic.set("Name", dynamic.createString(id));

		if (!properties.isEmpty()) {
			result = result.set(
				"Properties",
				dynamic.createMap(
					properties.entrySet()
						.stream()
						.collect(Collectors.toMap(
							entry -> dynamic.createString(entry.getKey()),
							entry -> dynamic.createString(entry.getValue())
						))
				)
			);
		}

		return result;
	}

	public static Dynamic<?> createBlockState(String id) {
		return createBlockState(id, Map.of());
	}

	public static Dynamic<?> apply(Dynamic<?> dynamic, String fieldName, UnaryOperator<String> applier) {
		return dynamic.update(
			fieldName,
			value -> (Dynamic<?>) DataFixUtils.orElse(
				value.asString()
					.map(applier)
					.map(dynamic::createString)
					.result(),
				value
			)
		);
	}

	public static String getColorName(int index) {
		return switch (index) {
			case 1 -> "orange";
			case 2 -> "magenta";
			case 3 -> "light_blue";
			case 4 -> "yellow";
			case 5 -> "lime";
			case 6 -> "pink";
			case 7 -> "gray";
			case 8 -> "light_gray";
			case 9 -> "cyan";
			case 10 -> "purple";
			case 11 -> "blue";
			case 12 -> "brown";
			case 13 -> "green";
			case 14 -> "red";
			case 15 -> "black";
			default -> "white";
		};
	}

	public static <T> Typed<?> setTypedFromDynamic(Typed<?> typed, OpticFinder<T> opticFinder, Dynamic<?> dynamic) {
		return typed.set(opticFinder, Util.readTyped(opticFinder.type(), dynamic, true));
	}
}
