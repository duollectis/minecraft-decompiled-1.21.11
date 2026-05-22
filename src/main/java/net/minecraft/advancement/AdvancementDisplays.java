package net.minecraft.advancement;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Утилитный класс для вычисления видимости достижений в дереве.
 * <p>
 * Достижение считается видимым, если оно само выполнено, либо если в пределах
 * {@link #DISPLAY_DEPTH} уровней вверх по дереву есть выполненное достижение.
 */
public class AdvancementDisplays {

	private static final int DISPLAY_DEPTH = 2;

	private static Status getStatus(Advancement advancement, boolean done) {
		Optional<AdvancementDisplay> display = advancement.display();
		if (display.isEmpty()) {
			return Status.HIDE;
		}
		if (done) {
			return Status.SHOW;
		}
		return display.get().isHidden() ? Status.HIDE : Status.NO_CHANGE;
	}

	private static boolean shouldDisplay(Stack<Status> statuses) {
		for (int depth = 0; depth <= DISPLAY_DEPTH; depth++) {
			Status status = statuses.peek(depth);
			if (status == Status.SHOW) {
				return true;
			}
			if (status == Status.HIDE) {
				return false;
			}
		}
		return false;
	}

	private static boolean shouldDisplay(
		PlacedAdvancement advancement,
		Stack<Status> statuses,
		Predicate<PlacedAdvancement> donePredicate,
		ResultConsumer consumer
	) {
		boolean done = donePredicate.test(advancement);
		Status status = getStatus(advancement.getAdvancement(), done);
		boolean anyChildDone = done;
		statuses.push(status);

		for (PlacedAdvancement child : advancement.getChildren()) {
			anyChildDone |= shouldDisplay(child, statuses, donePredicate, consumer);
		}

		boolean visible = anyChildDone || shouldDisplay(statuses);
		statuses.pop();
		consumer.accept(advancement, visible);
		return anyChildDone;
	}

	/**
	 * Рассчитывает видимость всех достижений в дереве, начиная с корня.
	 *
	 * @param advancement   любое достижение в дереве (корень будет найден автоматически)
	 * @param donePredicate предикат, возвращающий {@code true} если достижение выполнено
	 * @param consumer      получает каждое достижение и флаг его видимости
	 */
	public static void calculateDisplay(
		PlacedAdvancement advancement,
		Predicate<PlacedAdvancement> donePredicate,
		ResultConsumer consumer
	) {
		PlacedAdvancement root = advancement.getRoot();
		Stack<Status> stack = new ObjectArrayList<>();

		for (int depth = 0; depth <= DISPLAY_DEPTH; depth++) {
			stack.push(Status.NO_CHANGE);
		}

		shouldDisplay(root, stack, donePredicate, consumer);
	}

	@FunctionalInterface
	public interface ResultConsumer {

		void accept(PlacedAdvancement advancement, boolean shouldDisplay);
	}

	enum Status {
		SHOW,
		HIDE,
		NO_CHANGE
	}
}
