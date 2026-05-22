package net.minecraft.util;

import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Результат выполнения игрового действия (использование предмета, взаимодействие с блоком и т.д.).
 * <p>
 * Sealed-иерархия позволяет исчерпывающе обрабатывать все возможные исходы через {@code switch}.
 * Каждый вариант несёт собственную семантику: успех с анимацией, провал, пропуск или
 * делегирование стандартному обработчику блока.
 */
public sealed interface ActionResult permits ActionResult.Success, ActionResult.Fail, ActionResult.Pass, ActionResult.PassToDefaultBlockAction {

	ActionResult.Success SUCCESS = new ActionResult.Success(SwingSource.CLIENT, ItemContext.KEEP_HAND_STACK);

	ActionResult.Success SUCCESS_SERVER = new ActionResult.Success(SwingSource.SERVER, ItemContext.KEEP_HAND_STACK);

	ActionResult.Success CONSUME = new ActionResult.Success(SwingSource.NONE, ItemContext.KEEP_HAND_STACK);

	ActionResult.Fail FAIL = new ActionResult.Fail();

	ActionResult.Pass PASS = new ActionResult.Pass();

	ActionResult.PassToDefaultBlockAction PASS_TO_DEFAULT_BLOCK_ACTION = new ActionResult.PassToDefaultBlockAction();

	default boolean isAccepted() {
		return false;
	}

	/**
	 * Действие завершилось провалом — операция не была выполнена.
	 */
	record Fail() implements ActionResult {
	}

	/**
	 * Контекст изменения предмета в руке после успешного действия.
	 *
	 * @param incrementStat  нужно ли засчитывать статистику использования предмета
	 * @param newHandStack   новый стек предмета в руке, или {@code null} если предмет не меняется
	 */
	record ItemContext(boolean incrementStat, @Nullable ItemStack newHandStack) {

		static final ItemContext KEEP_HAND_STACK_NO_INCREMENT_STAT = new ItemContext(false, null);
		static final ItemContext KEEP_HAND_STACK = new ItemContext(true, null);
	}

	/**
	 * Действие пропущено — следующий обработчик в цепочке должен его обработать.
	 */
	record Pass() implements ActionResult {
	}

	/**
	 * Действие передаётся стандартному обработчику блока (например, открытие двери).
	 */
	record PassToDefaultBlockAction() implements ActionResult {
	}

	/**
	 * Действие выполнено успешно.
	 *
	 * @param swingSource  источник анимации взмаха рукой
	 * @param itemContext  контекст изменения предмета в руке
	 */
	record Success(SwingSource swingSource, ItemContext itemContext) implements ActionResult {

		@Override
		public boolean isAccepted() {
			return true;
		}

		/**
		 * Создаёт новый результат с заменённым предметом в руке и включённым счётчиком статистики.
		 *
		 * @param newHandStack новый стек предмета, который окажется в руке игрока
		 * @return новый {@code Success} с обновлённым контекстом предмета
		 */
		public ActionResult.Success withNewHandStack(ItemStack newHandStack) {
			return new ActionResult.Success(swingSource, new ItemContext(true, newHandStack));
		}

		/**
		 * Создаёт новый результат без увеличения счётчика статистики использования предмета.
		 *
		 * @return новый {@code Success} с отключённым инкрементом статистики
		 */
		public ActionResult.Success noIncrementStat() {
			return new ActionResult.Success(swingSource, ItemContext.KEEP_HAND_STACK_NO_INCREMENT_STAT);
		}

		public boolean shouldIncrementStat() {
			return itemContext.incrementStat();
		}

		public @Nullable ItemStack getNewHandStack() {
			return itemContext.newHandStack();
		}
	}

	/**
	 * Источник анимации взмаха рукой при успешном действии.
	 */
	enum SwingSource {
		/** Анимация не воспроизводится. */
		NONE,
		/** Анимация воспроизводится на клиенте. */
		CLIENT,
		/** Анимация воспроизводится на сервере и синхронизируется с клиентом. */
		SERVER
	}
}
