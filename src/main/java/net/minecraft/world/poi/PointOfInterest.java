package net.minecraft.world.poi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/**
 * Точка интереса (POI) — конкретная позиция в мире, связанная с определённым типом POI.
 * Хранит количество свободных «билетов» (слотов), которые могут занимать жители деревни
 * или другие сущности для привязки к данной точке.
 */
public class PointOfInterest {

	private final BlockPos pos;
	private final RegistryEntry<PointOfInterestType> type;
	private int freeTickets;
	private final Runnable updateListener;

	PointOfInterest(BlockPos pos, RegistryEntry<PointOfInterestType> type, int freeTickets, Runnable updateListener) {
		this.pos = pos.toImmutable();
		this.type = type;
		this.freeTickets = freeTickets;
		this.updateListener = updateListener;
	}

	public PointOfInterest(BlockPos pos, RegistryEntry<PointOfInterestType> type, Runnable updateListener) {
		this(pos, type, type.value().ticketCount(), updateListener);
	}

	public Serialized toSerialized() {
		return new Serialized(pos, type, freeTickets);
	}

	@Deprecated
	@Debug
	public int getFreeTickets() {
		return freeTickets;
	}

	/**
	 * Резервирует один билет, уменьшая счётчик свободных слотов.
	 * Вызывает {@code updateListener} при успешном резервировании.
	 *
	 * @return {@code true}, если билет успешно зарезервирован; {@code false}, если свободных слотов нет
	 */
	protected boolean reserveTicket() {
		if (freeTickets <= 0) {
			return false;
		}

		freeTickets--;
		updateListener.run();
		return true;
	}

	/**
	 * Освобождает один билет, увеличивая счётчик свободных слотов.
	 * Вызывает {@code updateListener} при успешном освобождении.
	 *
	 * @return {@code true}, если билет успешно освобождён; {@code false}, если все слоты уже свободны
	 */
	protected boolean releaseTicket() {
		if (freeTickets >= type.value().ticketCount()) {
			return false;
		}

		freeTickets++;
		updateListener.run();
		return true;
	}

	public boolean hasSpace() {
		return freeTickets > 0;
	}

	public boolean isOccupied() {
		return freeTickets != type.value().ticketCount();
	}

	public BlockPos getPos() {
		return pos;
	}

	public RegistryEntry<PointOfInterestType> getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o != null && getClass() == o.getClass()
			? Objects.equals(pos, ((PointOfInterest) o).pos)
			: false;
	}

	@Override
	public int hashCode() {
		return pos.hashCode();
	}

	/**
	 * Сериализованное представление {@link PointOfInterest} для записи на диск через Codec.
	 */
	public record Serialized(BlockPos pos, RegistryEntry<PointOfInterestType> poiType, int freeTickets) {

		public static final Codec<Serialized> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(Serialized::pos),
				RegistryFixedCodec
					.of(RegistryKeys.POINT_OF_INTEREST_TYPE)
					.fieldOf("type")
					.forGetter(Serialized::poiType),
				Codec.INT.fieldOf("free_tickets").orElse(0).forGetter(Serialized::freeTickets)
			).apply(instance, Serialized::new)
		);

		/**
		 * Восстанавливает {@link PointOfInterest} из сериализованного состояния.
		 *
		 * @param updateListener слушатель изменений, вызываемый при резервировании/освобождении билетов
		 * @return восстановленный объект {@link PointOfInterest}
		 */
		public PointOfInterest toPointOfInterest(Runnable updateListener) {
			return new PointOfInterest(pos, poiType, freeTickets, updateListener);
		}
	}
}
