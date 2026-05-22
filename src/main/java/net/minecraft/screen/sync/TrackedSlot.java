package net.minecraft.screen.sync;

import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Отслеживаемое состояние слота для определения необходимости синхронизации.
 * <p>
 * Хранит последнее подтверждённое клиентом состояние слота — либо полную копию
 * {@link ItemStack}, либо хэш-снимок {@link ItemStackHash}. Метод {@link #isInSync}
 * позволяет серверу определить, нужно ли отправлять обновление конкретному игроку.
 */
public interface TrackedSlot {

	/**
	 * Заглушка для слотов, которые всегда считаются синхронизированными
	 * (например, слоты, не отображаемые клиенту).
	 */
	TrackedSlot ALWAYS_IN_SYNC = new TrackedSlot() {
		@Override
		public void setReceivedHash(ItemStackHash receivedHash) {
		}

		@Override
		public void setReceivedStack(ItemStack receivedStack) {
		}

		@Override
		public boolean isInSync(ItemStack actualStack) {
			return true;
		}
	};

	void setReceivedStack(ItemStack receivedStack);

	void setReceivedHash(ItemStackHash receivedHash);

	boolean isInSync(ItemStack actualStack);

	/**
	 * Стандартная реализация отслеживания слота с поддержкой как полного стека,
	 * так и хэш-сравнения для оптимизации трафика.
	 */
	class Impl implements TrackedSlot {

		private final ComponentChangesHash.ComponentHasher hasher;
		private @Nullable ItemStack receivedStack;
		private @Nullable ItemStackHash receivedHash;

		public Impl(ComponentChangesHash.ComponentHasher hasher) {
			this.hasher = hasher;
		}

		@Override
		public void setReceivedStack(ItemStack receivedStack) {
			this.receivedStack = receivedStack.copy();
			receivedHash = null;
		}

		@Override
		public void setReceivedHash(ItemStackHash receivedHash) {
			receivedStack = null;
			this.receivedHash = receivedHash;
		}

		/**
		 * Проверяет синхронизацию слота с фактическим содержимым.
		 * Если синхронизация подтверждена через хэш — кэширует полный стек
		 * для ускорения последующих проверок.
		 *
		 * @param actualStack фактическое содержимое слота на сервере
		 * @return {@code true} если клиент уже имеет актуальное состояние
		 */
		@Override
		public boolean isInSync(ItemStack actualStack) {
			if (receivedStack != null) {
				return ItemStack.areEqual(receivedStack, actualStack);
			}

			if (receivedHash != null && receivedHash.hashEquals(actualStack, hasher)) {
				receivedStack = actualStack.copy();
				return true;
			}

			return false;
		}

		public void copyFrom(Impl slot) {
			receivedStack = slot.receivedStack;
			receivedHash = slot.receivedHash;
		}
	}
}
