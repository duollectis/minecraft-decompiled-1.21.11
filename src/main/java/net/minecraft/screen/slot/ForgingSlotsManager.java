package net.minecraft.screen.slot;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Управляет конфигурацией слотов для экранов ковки (наковальня, точильный камень, кузнечный стол).
 * <p>
 * Хранит описание входных слотов и слота результата, включая их позиции и предикаты допустимых предметов.
 * Создаётся через {@link Builder} и передаётся в конструктор {@link net.minecraft.screen.ForgingScreenHandler}.
 */
public class ForgingSlotsManager {

	private final List<ForgingSlot> inputSlots;
	private final ForgingSlot resultSlot;

	ForgingSlotsManager(List<ForgingSlot> inputSlots, ForgingSlot resultSlot) {
		if (inputSlots.isEmpty() || resultSlot.equals(ForgingSlot.DEFAULT)) {
			throw new IllegalArgumentException("Need to define both inputSlots and resultSlot");
		}

		this.inputSlots = inputSlots;
		this.resultSlot = resultSlot;
	}

	public static Builder builder() {
		return new Builder();
	}

	public ForgingSlot getInputSlot(int index) {
		return inputSlots.get(index);
	}

	public ForgingSlot getResultSlot() {
		return resultSlot;
	}

	public List<ForgingSlot> getInputSlots() {
		return inputSlots;
	}

	public int getInputSlotCount() {
		return inputSlots.size();
	}

	public int getResultSlotIndex() {
		return getInputSlotCount();
	}

	/**
	 * Строитель конфигурации слотов ковки.
	 * <p>
	 * Слоты должны добавляться с непрерывными индексами, начиная с 0.
	 * Слот результата должен следовать сразу за последним входным слотом.
	 */
	public static class Builder {

		private final List<ForgingSlot> inputs = new ArrayList<>();
		private ForgingSlot resultSlot = ForgingSlot.DEFAULT;

		public Builder input(int slotId, int x, int y, Predicate<ItemStack> mayPlace) {
			inputs.add(new ForgingSlot(slotId, x, y, mayPlace));
			return this;
		}

		public Builder output(int slotId, int x, int y) {
			resultSlot = new ForgingSlot(slotId, x, y, stack -> false);
			return this;
		}

		public ForgingSlotsManager build() {
			int inputCount = inputs.size();

			for (int index = 0; index < inputCount; index++) {
				ForgingSlot slot = inputs.get(index);
				if (slot.slotId != index) {
					throw new IllegalArgumentException("Expected input slots to have continous indexes");
				}
			}

			if (resultSlot.slotId != inputCount) {
				throw new IllegalArgumentException("Expected result slot index to follow last input slot");
			}

			return new ForgingSlotsManager(inputs, resultSlot);
		}
	}

	/**
	 * Описание одного слота ковки: его идентификатор, экранные координаты и предикат допустимых предметов.
	 */
	public record ForgingSlot(int slotId, int x, int y, Predicate<ItemStack> mayPlace) {

		static final ForgingSlot DEFAULT = new ForgingSlot(0, 0, 0, stack -> true);
	}
}
