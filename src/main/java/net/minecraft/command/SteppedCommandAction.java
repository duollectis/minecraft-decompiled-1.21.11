package net.minecraft.command;

import java.util.List;

/**
 * Действие, выполняющее список подкоманд пошагово — по одной за итерацию
 * основного цикла очереди. Используется для больших списков источников,
 * чтобы не переполнять очередь за один шаг.
 *
 * @param <T> тип источника команды
 * @param <P> тип элемента подкоманды
 */
public class SteppedCommandAction<T, P> implements CommandAction<T> {

	private final ActionWrapper<T, P> wrapper;
	private final List<P> actions;
	private final CommandQueueEntry<T> selfCommandQueueEntry;
	private int nextActionIndex;

	private SteppedCommandAction(ActionWrapper<T, P> wrapper, List<P> actions, Frame frame) {
		this.wrapper = wrapper;
		this.actions = actions;
		selfCommandQueueEntry = new CommandQueueEntry<>(frame, this);
	}

	@Override
	public void execute(CommandExecutionContext<T> context, Frame frame) {
		P action = actions.get(nextActionIndex);
		context.enqueueCommand(wrapper.create(frame, action));
		if (++nextActionIndex < actions.size()) {
			context.enqueueCommand(selfCommandQueueEntry);
		}
	}

	/**
	 * Ставит в очередь все действия из списка. Для 0–2 элементов использует
	 * прямую постановку в очередь; для больших списков создаёт пошаговый итератор.
	 */
	public static <T, P> void enqueueCommands(
			CommandExecutionContext<T> context,
			Frame frame,
			List<P> actions,
			ActionWrapper<T, P> wrapper
	) {
		switch (actions.size()) {
			case 0 -> {}
			case 1 -> context.enqueueCommand(wrapper.create(frame, actions.get(0)));
			case 2 -> {
				context.enqueueCommand(wrapper.create(frame, actions.get(0)));
				context.enqueueCommand(wrapper.create(frame, actions.get(1)));
			}
			default -> context.enqueueCommand(new SteppedCommandAction<>(wrapper, actions, frame).selfCommandQueueEntry);
		}
	}

	/**
	 * Фабрика для создания записи очереди из фрейма и элемента подкоманды.
	 */
	@FunctionalInterface
	public interface ActionWrapper<T, P> {

		CommandQueueEntry<T> create(Frame frame, P action);
	}
}
