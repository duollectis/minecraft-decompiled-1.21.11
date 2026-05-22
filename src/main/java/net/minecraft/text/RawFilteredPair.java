package net.minecraft.text;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.filter.FilteredMessage;

import java.util.Optional;
import java.util.function.Function;

/**
 * Пара значений: исходное (raw) и опционально отфильтрованное (filtered).
 *
 * <p>Используется для передачи сообщений через систему фильтрации чата.
 * Если фильтрация не применялась или сообщение прошло без изменений,
 * {@code filtered} будет {@link Optional#empty()}.
 * Метод {@link #get(boolean)} позволяет выбрать нужный вариант в зависимости
 * от того, включена ли фильтрация для данного получателя.</p>
 *
 * @param <T> тип хранимых значений
 */
public record RawFilteredPair<T>(T raw, Optional<T> filtered) {

	/**
	 * Создаёт DFU codec для {@link RawFilteredPair}, поддерживающий два формата:
	 * полный (с полями {@code raw} и {@code filtered}) и сокращённый (только значение).
	 *
	 * @param baseCodec codec для типа {@code T}
	 * @return codec для {@code RawFilteredPair<T>}
	 */
	public static <T> Codec<RawFilteredPair<T>> createCodec(Codec<T> baseCodec) {
		Codec<RawFilteredPair<T>> fullCodec = RecordCodecBuilder.create(
				instance -> instance.group(
						baseCodec.fieldOf("raw").forGetter(RawFilteredPair::raw),
						baseCodec.optionalFieldOf("filtered").forGetter(RawFilteredPair::filtered)
				)
				.apply(instance, RawFilteredPair::new)
		);
		Codec<RawFilteredPair<T>> shortCodec = baseCodec.xmap(RawFilteredPair::of, RawFilteredPair::raw);
		return Codec.withAlternative(fullCodec, shortCodec);
	}

	/**
	 * Создаёт packet codec для {@link RawFilteredPair}, передающий оба поля по сети.
	 *
	 * @param basePacketCodec packet codec для типа {@code T}
	 * @return packet codec для {@code RawFilteredPair<T>}
	 */
	public static <B extends ByteBuf, T> PacketCodec<B, RawFilteredPair<T>> createPacketCodec(
			PacketCodec<B, T> basePacketCodec
	) {
		return PacketCodec.tuple(
				basePacketCodec,
				RawFilteredPair::raw,
				basePacketCodec.collect(PacketCodecs::optional),
				RawFilteredPair::filtered,
				RawFilteredPair::new
		);
	}

	/**
	 * Создаёт пару без отфильтрованного варианта (фильтрация не применялась).
	 *
	 * @param raw исходное значение
	 * @return пара с пустым {@code filtered}
	 */
	public static <T> RawFilteredPair<T> of(T raw) {
		return new RawFilteredPair<>(raw, Optional.empty());
	}

	/**
	 * Создаёт пару из {@link FilteredMessage}: raw берётся напрямую,
	 * filtered заполняется только если сообщение было изменено фильтром.
	 *
	 * @param message отфильтрованное сообщение от системы чата
	 * @return пара строк raw/filtered
	 */
	public static RawFilteredPair<String> of(FilteredMessage message) {
		return new RawFilteredPair<>(
				message.raw(),
				message.isFiltered() ? Optional.of(message.getString()) : Optional.empty()
		);
	}

	/**
	 * Возвращает нужный вариант значения в зависимости от флага фильтрации.
	 *
	 * @param shouldFilter {@code true} — вернуть отфильтрованный вариант (или raw если filtered пуст),
	 *                     {@code false} — всегда вернуть raw
	 * @return выбранное значение
	 */
	public T get(boolean shouldFilter) {
		return shouldFilter ? filtered.orElse(raw) : raw;
	}

	/**
	 * Применяет маппер к обоим значениям пары, создавая новую пару с преобразованным типом.
	 *
	 * @param mapper функция преобразования {@code T -> U}
	 * @return новая пара типа {@code RawFilteredPair<U>}
	 */
	public <U> RawFilteredPair<U> map(Function<T, U> mapper) {
		return new RawFilteredPair<>(mapper.apply(raw), filtered.map(mapper));
	}

	/**
	 * Применяет резолвер к обоим значениям пары, возвращая пустой Optional если хотя бы одно не разрешилось.
	 *
	 * <p>Если raw не разрешился — возвращает {@link Optional#empty()}.
	 * Если filtered присутствует, но не разрешился — также возвращает {@link Optional#empty()}.
	 * Это гарантирует, что результирующая пара всегда семантически согласована.</p>
	 *
	 * @param resolver функция {@code T -> Optional<U>}, возвращающая пустой Optional при неудаче
	 * @return {@code Optional<RawFilteredPair<U>>} — пустой если разрешение не удалось
	 */
	public <U> Optional<RawFilteredPair<U>> resolve(Function<T, Optional<U>> resolver) {
		Optional<U> resolvedRaw = resolver.apply(raw);

		if (resolvedRaw.isEmpty()) {
			return Optional.empty();
		}

		if (filtered.isPresent()) {
			Optional<U> resolvedFiltered = resolver.apply(filtered.get());

			return resolvedFiltered.isEmpty()
					? Optional.empty()
					: Optional.of(new RawFilteredPair<>(resolvedRaw.get(), resolvedFiltered));
		}

		return Optional.of(new RawFilteredPair<>(resolvedRaw.get(), Optional.empty()));
	}
}
