package net.minecraft.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Звуковое событие, идентифицируемое по {@link Identifier} и опциональной
 * фиксированной дальностью слышимости.
 *
 * <p>Если {@link #fixedRange} не задан, дальность вычисляется динамически
 * на основе громкости воспроизведения: {@code volume > 1.0} масштабирует
 * базовую дальность 16 блоков, иначе дальность равна ровно 16 блокам.
 *
 * @param id         уникальный идентификатор звука в реестре
 * @param fixedRange фиксированная дальность слышимости в блоках, если задана
 */
public record SoundEvent(Identifier id, Optional<Float> fixedRange) {

	/** Базовая дальность слышимости звука при громкости ≤ 1.0. */
	private static final float BASE_DISTANCE = 16.0F;

	public static final Codec<SoundEvent> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Identifier.CODEC.fieldOf("sound_id").forGetter(SoundEvent::id),
			Codec.FLOAT.lenientOptionalFieldOf("range").forGetter(SoundEvent::fixedRange)
		).apply(instance, SoundEvent::of)
	);
	public static final Codec<RegistryEntry<SoundEvent>> ENTRY_CODEC =
		RegistryElementCodec.of(RegistryKeys.SOUND_EVENT, CODEC);
	public static final PacketCodec<ByteBuf, SoundEvent> PACKET_CODEC = PacketCodec.tuple(
		Identifier.PACKET_CODEC,
		SoundEvent::id,
		PacketCodecs.FLOAT.collect(PacketCodecs::optional),
		SoundEvent::fixedRange,
		SoundEvent::of
	);
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<SoundEvent>> ENTRY_PACKET_CODEC =
		PacketCodecs.registryEntry(RegistryKeys.SOUND_EVENT, PACKET_CODEC);

	public static SoundEvent of(Identifier id) {
		return new SoundEvent(id, Optional.empty());
	}

	public static SoundEvent of(Identifier id, float fixedRange) {
		return new SoundEvent(id, Optional.of(fixedRange));
	}

	/**
	 * Вычисляет дальность, на которой звук слышен при заданной громкости.
	 *
	 * <p>Если задана фиксированная дальность — возвращает её.
	 * Иначе: при {@code volume > 1.0} дальность масштабируется как {@code 16 * volume},
	 * при {@code volume ≤ 1.0} — всегда 16 блоков.
	 *
	 * @param volume громкость воспроизведения
	 * @return дальность слышимости в блоках
	 */
	public float getDistanceToTravel(float volume) {
		return fixedRange.orElse(volume > 1.0F ? BASE_DISTANCE * volume : BASE_DISTANCE);
	}

	private static SoundEvent of(Identifier id, Optional<Float> fixedRange) {
		return fixedRange
			.<SoundEvent>map(range -> of(id, range))
			.orElseGet(() -> of(id));
	}
}
