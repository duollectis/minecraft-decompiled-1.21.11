package net.minecraft.world.event.listener;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.event.Vibrations;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

/**
 * Выбирает наиболее приоритетную вибрацию из нескольких, поступивших за один тик.
 * <p>
 * Приоритет определяется по следующим правилам (в порядке убывания важности):
 * <ol>
 *   <li>Вибрация должна быть из того же тика, что и текущая.</li>
 *   <li>Меньшее расстояние до слушателя имеет приоритет.</li>
 *   <li>При равном расстоянии — большая частота события имеет приоритет.</li>
 * </ol>
 */
public class VibrationSelector {

	public static final Codec<VibrationSelector> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			Vibration.CODEC
				.lenientOptionalFieldOf("event")
				.forGetter(selector -> selector.current.map(Pair::getLeft)),
			Codec.LONG
				.fieldOf("tick")
				.forGetter(selector -> selector.current.<Long>map(Pair::getRight).orElse(-1L))
		).apply(instance, VibrationSelector::new)
	);

	private Optional<Pair<Vibration, Long>> current;

	public VibrationSelector(Optional<Vibration> vibration, long tick) {
		current = vibration.map(v -> Pair.of(v, tick));
	}

	public VibrationSelector() {
		current = Optional.empty();
	}

	/**
	 * Пытается принять новую вибрацию, если она приоритетнее текущей.
	 *
	 * @param vibration кандидат на принятие
	 * @param tick      игровой тик, в котором произошла вибрация
	 */
	public void tryAccept(Vibration vibration, long tick) {
		if (shouldSelect(vibration, tick)) {
			current = Optional.of(Pair.of(vibration, tick));
		}
	}

	/**
	 * Возвращает накопленную вибрацию, если она готова к обработке
	 * (т.е. тик её регистрации уже прошёл).
	 *
	 * @param currentTick текущий игровой тик
	 */
	public Optional<Vibration> getVibrationToTick(long currentTick) {
		if (current.isEmpty()) {
			return Optional.empty();
		}

		return current.get().getRight() < currentTick
			? Optional.of(current.get().getLeft())
			: Optional.empty();
	}

	public void clear() {
		current = Optional.empty();
	}

	/**
	 * Определяет, должна ли новая вибрация заменить текущую.
	 * Вибрации из разных тиков не конкурируют — принимается только из текущего тика.
	 */
	private boolean shouldSelect(Vibration candidate, long tick) {
		if (current.isEmpty()) {
			return true;
		}

		Pair<Vibration, Long> existing = current.get();
		long existingTick = existing.getRight();
		if (tick != existingTick) {
			return false;
		}

		Vibration existingVibration = existing.getLeft();
		if (candidate.distance() < existingVibration.distance()) {
			return true;
		}

		if (candidate.distance() > existingVibration.distance()) {
			return false;
		}

		return Vibrations.getFrequency(candidate.gameEvent()) > Vibrations.getFrequency(existingVibration.gameEvent());
	}
}
