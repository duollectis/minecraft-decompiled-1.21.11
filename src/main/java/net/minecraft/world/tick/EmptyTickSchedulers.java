package net.minecraft.world.tick;

import net.minecraft.util.math.BlockPos;

/**
 * Фабрика пустых (no-op) реализаций планировщиков тиков.
 * Используется на клиентской стороне и в контекстах, где тики не нужны.
 */
public final class EmptyTickSchedulers {

	private static final BasicTickScheduler<Object> EMPTY_BASIC = new BasicTickScheduler<>() {
		@Override
		public void scheduleTick(OrderedTick<Object> orderedTick) {
		}

		@Override
		public boolean isQueued(BlockPos pos, Object type) {
			return false;
		}

		@Override
		public int getTickCount() {
			return 0;
		}
	};

	private static final QueryableTickScheduler<Object> EMPTY_QUERYABLE = new QueryableTickScheduler<>() {
		@Override
		public void scheduleTick(OrderedTick<Object> orderedTick) {
		}

		@Override
		public boolean isQueued(BlockPos pos, Object type) {
			return false;
		}

		@Override
		public boolean isTicking(BlockPos pos, Object type) {
			return false;
		}

		@Override
		public int getTickCount() {
			return 0;
		}
	};

	private EmptyTickSchedulers() {
	}

	/**
	 * @return пустой планировщик только для чтения (серверная сторона без активных тиков)
	 */
	@SuppressWarnings("unchecked")
	public static <T> BasicTickScheduler<T> getReadOnlyTickScheduler() {
		return (BasicTickScheduler<T>) EMPTY_BASIC;
	}

	/**
	 * @return пустой планировщик для клиентской стороны, всегда возвращающий {@code false}
	 */
	@SuppressWarnings("unchecked")
	public static <T> QueryableTickScheduler<T> getClientTickScheduler() {
		return (QueryableTickScheduler<T>) EMPTY_QUERYABLE;
	}
}
