package net.minecraft.entity.ai.brain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.annotation.Debug;

import java.util.Optional;

/**
 * Хранит одно значение в памяти мозга сущности с опциональным временем жизни (TTL).
 * Постоянные воспоминания имеют {@code expiry == Long.MAX_VALUE} и никогда не истекают.
 * Временные воспоминания уменьшают счётчик на каждом тике и удаляются при достижении нуля.
 */
public class Memory<T> {

	private final T value;
	private long expiry;

	public Memory(T value, long expiry) {
		this.value = value;
		this.expiry = expiry;
	}

	public void tick() {
		if (isTimed()) {
			expiry--;
		}
	}

	public static <T> Memory<T> permanent(T value) {
		return new Memory<>(value, Long.MAX_VALUE);
	}

	public static <T> Memory<T> timed(T value, long expiry) {
		return new Memory<>(value, expiry);
	}

	public long getExpiry() {
		return expiry;
	}

	public T getValue() {
		return value;
	}

	public boolean isExpired() {
		return expiry <= 0L;
	}

	@Override
	public String toString() {
		return value + (isTimed() ? " (ttl: " + expiry + ")" : "");
	}

	@Debug
	public boolean isTimed() {
		return expiry != Long.MAX_VALUE;
	}

	/**
	 * Сериализует память в codec с поддержкой опционального TTL.
	 * Если память постоянная (expiry == Long.MAX_VALUE), поле "ttl" не записывается.
	 */
	public static <T> Codec<Memory<T>> createCodec(Codec<T> codec) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
						codec.fieldOf("value").forGetter(memory -> memory.value),
						Codec.LONG
								.lenientOptionalFieldOf("ttl")
								.forGetter(memory -> memory.isTimed() ? Optional.of(memory.expiry) : Optional.empty())
				).apply(
						instance,
						(value, expiry) -> new Memory<>(value, expiry.orElse(Long.MAX_VALUE))
				)
		);
	}
}
