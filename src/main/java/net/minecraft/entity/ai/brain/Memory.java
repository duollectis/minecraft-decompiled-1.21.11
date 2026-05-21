package net.minecraft.entity.ai.brain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.annotation.Debug;

import java.util.Optional;

/**
 * {@code Memory}.
 */
public class Memory<T> {

	private final T value;
	private long expiry;

	public Memory(T value, long expiry) {
		this.value = value;
		this.expiry = expiry;
	}

	/**
	 * Tick.
	 */
	public void tick() {
		if (this.isTimed()) {
			this.expiry--;
		}
	}

	/**
	 * Permanent.
	 *
	 * @param value value
	 *
	 * @return Memory — результат операции
	 */
	public static <T> Memory<T> permanent(T value) {
		return new Memory<>(value, Long.MAX_VALUE);
	}

	/**
	 * Timed.
	 *
	 * @param value value
	 * @param expiry expiry
	 *
	 * @return Memory — результат операции
	 */
	public static <T> Memory<T> timed(T value, long expiry) {
		return new Memory<>(value, expiry);
	}

	public long getExpiry() {
		return this.expiry;
	}

	public T getValue() {
		return this.value;
	}

	public boolean isExpired() {
		return this.expiry <= 0L;
	}

	@Override
	public String toString() {
		return this.value + (this.isTimed() ? " (ttl: " + this.expiry + ")" : "");
	}

	@Debug
	public boolean isTimed() {
		return this.expiry != Long.MAX_VALUE;
	}

	/**
	 * Создаёт codec.
	 *
	 * @param codec codec
	 *
	 * @return Codec> — результат операции
	 */
	public static <T> Codec<Memory<T>> createCodec(Codec<T> codec) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
						                    codec.fieldOf("value").forGetter(memory -> memory.value),
						                    Codec.LONG
								                    .lenientOptionalFieldOf("ttl")
								                    .forGetter(memory -> memory.isTimed() ? Optional.of(memory.expiry) : Optional.empty())
				                    )
				                    .apply(
						                    instance,
						                    (value, expiry) -> new Memory<>(value, expiry.orElse(Long.MAX_VALUE))
				                    )
		);
	}
}
