package net.minecraft.entity.ai.brain.task;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Unit;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.MemoryQuery;
import net.minecraft.entity.ai.brain.MemoryQueryResult;
import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.*;

/**
 * {@code TaskTriggerer}.
 */
public class TaskTriggerer<E extends LivingEntity, M> implements App<TaskTriggerer.K1<E>, M> {

	private final TaskTriggerer.TaskFunction<E, M> function;

	/**
	 * Cast.
	 *
	 * @param app app
	 *
	 * @return TaskTriggerer — результат операции
	 */
	public static <E extends LivingEntity, M> TaskTriggerer<E, M> cast(App<TaskTriggerer.K1<E>, M> app) {
		return (TaskTriggerer<E, M>) app;
	}

	public static <E extends LivingEntity> TaskTriggerer.TaskContext<E> newContext() {
		return new TaskTriggerer.TaskContext<>();
	}

	public static <E extends LivingEntity> SingleTickTask<E> task(
			Function<TaskTriggerer.TaskContext<E>, ? extends App<TaskTriggerer.K1<E>, TaskRunnable<E>>> creator
	) {
		final TaskTriggerer.TaskFunction<E, TaskRunnable<E>>
				taskFunction =
				getFunction((App<TaskTriggerer.K1<E>, TaskRunnable<E>>) creator.apply(newContext()));
		return new SingleTickTask<E>() {
			@Override
			public boolean trigger(ServerWorld serverWorld, E livingEntity, long l) {
				TaskRunnable<E> taskRunnable = taskFunction.run(serverWorld, livingEntity, l);
				return taskRunnable == null ? false : taskRunnable.trigger(serverWorld, livingEntity, l);
			}

			@Override
			public String getName() {
				return "OneShot[" + taskFunction.asString() + "]";
			}

			@Override
			public String toString() {
				return this.getName();
			}
		};
	}

	public static <E extends LivingEntity> SingleTickTask<E> runIf(
			TaskRunnable<? super E> predicate,
			TaskRunnable<? super E> task
	) {
		return task(context -> context.group(context.trigger(predicate)).apply(context, unit -> task::trigger));
	}

	public static <E extends LivingEntity> SingleTickTask<E> runIf(
			Predicate<E> predicate,
			SingleTickTask<? super E> task
	) {
		return runIf(predicate(predicate), task);
	}

	/**
	 * Predicate.
	 *
	 * @param predicate predicate
	 *
	 * @return SingleTickTask — результат операции
	 */
	public static <E extends LivingEntity> SingleTickTask<E> predicate(Predicate<E> predicate) {
		return task(context -> context.point((world, entity, time) -> predicate.test(entity)));
	}

	/**
	 * Predicate.
	 *
	 * @param predicate predicate
	 *
	 * @return SingleTickTask — результат операции
	 */
	public static <E extends LivingEntity> SingleTickTask<E> predicate(BiPredicate<ServerWorld, E> predicate) {
		return task(context -> context.point((world, entity, time) -> predicate.test(world, entity)));
	}

	static <E extends LivingEntity, M> TaskTriggerer.TaskFunction<E, M> getFunction(App<TaskTriggerer.K1<E>, M> app) {
		return cast(app).function;
	}

	TaskTriggerer(TaskTriggerer.TaskFunction<E, M> function) {
		this.function = function;
	}

	static <E extends LivingEntity, M> TaskTriggerer<E, M> of(TaskTriggerer.TaskFunction<E, M> function) {
		return new TaskTriggerer<>(function);
	}

	/**
	 * {@code K1}.
	 */
	public static final class K1<E extends LivingEntity> implements com.mojang.datafixers.kinds.K1 {
	}

	/**
	 * {@code QueryMemory}.
	 */
	static final class QueryMemory<E extends LivingEntity, F extends com.mojang.datafixers.kinds.K1, Value>
			extends TaskTriggerer<E, MemoryQueryResult<F, Value>> {

		QueryMemory(MemoryQuery<F, Value> query) {
			super(new TaskTriggerer.TaskFunction<E, MemoryQueryResult<F, Value>>() {
				/**
				 * Run.
				 *
				 * @param serverWorld server world
				 * @param livingEntity living entity
				 * @param l l
				 *
				 * @return @Nullable MemoryQueryResult — результат операции
				 */
				public @Nullable MemoryQueryResult<F, Value> run(ServerWorld serverWorld, E livingEntity, long l) {
					Brain<?> brain = livingEntity.getBrain();
					Optional<Value> optional = brain.getOptionalMemory(query.memory());
					return optional == null ? null : query.toQueryResult(brain, optional);
				}

				@Override
				public String asString() {
					return "M[" + query + "]";
				}

				@Override
				public String toString() {
					return this.asString();
				}
			});
		}
	}

	/**
	 * {@code Supply}.
	 */
	static final class Supply<E extends LivingEntity, A> extends TaskTriggerer<E, A> {

		Supply(A value) {
			this(value, () -> "C[" + value + "]");
		}

		Supply(A value, Supplier<String> nameSupplier) {
			super(new TaskTriggerer.TaskFunction<E, A>() {
				@Override
				public A run(ServerWorld world, E entity, long time) {
					return value;
				}

				@Override
				public String asString() {
					return nameSupplier.get();
				}

				@Override
				public String toString() {
					return this.asString();
				}
			});
		}
	}

	/**
	 * {@code TaskContext}.
	 */
	public static final class TaskContext<E extends LivingEntity> implements Applicative<TaskTriggerer.K1<E>, TaskTriggerer.TaskContext.Mu<E>> {

		public <Value> Optional<Value> getOptionalValue(MemoryQueryResult<com.mojang.datafixers.kinds.OptionalBox.Mu, Value> result) {
			return OptionalBox.unbox(result.getValue());
		}

		public <Value> Value getValue(MemoryQueryResult<com.mojang.datafixers.kinds.IdF.Mu, Value> result) {
			return (Value) IdF.get(result.getValue());
		}

		public <Value> TaskTriggerer<E, MemoryQueryResult<com.mojang.datafixers.kinds.OptionalBox.Mu, Value>> queryMemoryOptional(
				MemoryModuleType<Value> type
		) {
			return new TaskTriggerer.QueryMemory<>(new MemoryQuery.Optional<>(type));
		}

		public <Value> TaskTriggerer<E, MemoryQueryResult<com.mojang.datafixers.kinds.IdF.Mu, Value>> queryMemoryValue(
				MemoryModuleType<Value> type
		) {
			return new TaskTriggerer.QueryMemory<>(new MemoryQuery.MemoryValue<>(type));
		}

		public <Value> TaskTriggerer<E, MemoryQueryResult<com.mojang.datafixers.kinds.Const.Mu<Unit>, Value>> queryMemoryAbsent(
				MemoryModuleType<Value> type
		) {
			return new TaskTriggerer.QueryMemory<>(new MemoryQuery.Absent<>(type));
		}

		/**
		 * Trigger.
		 *
		 * @param runnable runnable
		 *
		 * @return TaskTriggerer — результат операции
		 */
		public TaskTriggerer<E, Unit> trigger(TaskRunnable<? super E> runnable) {
			return new TaskTriggerer.Trigger<>(runnable);
		}

		/**
		 * Point.
		 *
		 * @param object object
		 *
		 * @return TaskTriggerer — результат операции
		 */
		public <A> TaskTriggerer<E, A> point(A object) {
			return new TaskTriggerer.Supply<>(object);
		}

		/**
		 * Supply.
		 *
		 * @param nameSupplier name supplier
		 * @param value value
		 *
		 * @return TaskTriggerer — результат операции
		 */
		public <A> TaskTriggerer<E, A> supply(Supplier<String> nameSupplier, A value) {
			return new TaskTriggerer.Supply<>(value, nameSupplier);
		}

		public <A, R> Function<App<TaskTriggerer.K1<E>, A>, App<TaskTriggerer.K1<E>, R>> lift1(App<TaskTriggerer.K1<E>, Function<A, R>> app) {
			return app2 -> {
				final TaskTriggerer.TaskFunction<E, A>
						taskFunction =
						(TaskTriggerer.TaskFunction<E, A>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app2);
				final TaskTriggerer.TaskFunction<E, Function<A, R>> taskFunction2 = TaskTriggerer.getFunction(app);
				return TaskTriggerer.of(new TaskTriggerer.TaskFunction<E, R>() {
					@Override
					public R run(ServerWorld world, E entity, long time) {
						A object = (A) taskFunction.run(world, entity, time);
						if (object == null) {
							return null;
						}
						else {
							Function<A, R> function = (Function<A, R>) taskFunction2.run(world, entity, time);
							return (R) (function == null ? null : function.apply(object));
						}
					}

					@Override
					public String asString() {
						return taskFunction2.asString() + " * " + taskFunction.asString();
					}

					@Override
					public String toString() {
						return this.asString();
					}
				});
			};
		}

		public <T, R> TaskTriggerer<E, R> map(
				Function<? super T, ? extends R> function,
				App<TaskTriggerer.K1<E>, T> app
		) {
			final TaskTriggerer.TaskFunction<E, T>
					taskFunction =
					(TaskTriggerer.TaskFunction<E, T>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app);
			return TaskTriggerer.of(new TaskTriggerer.TaskFunction<E, R>() {
				@Override
				public R run(ServerWorld world, E entity, long time) {
					T object = taskFunction.run(world, entity, time);
					return (R) (object == null ? null : function.apply(object));
				}

				@Override
				public String asString() {
					return taskFunction.asString() + ".map[" + function + "]";
				}

				@Override
				public String toString() {
					return this.asString();
				}
			});
		}

		public <A, B, R> TaskTriggerer<E, R> ap2(
				App<TaskTriggerer.K1<E>, BiFunction<A, B, R>> app,
				App<TaskTriggerer.K1<E>, A> app2,
				App<TaskTriggerer.K1<E>, B> app3
		) {
			final TaskTriggerer.TaskFunction<E, A>
					taskFunction =
					(TaskTriggerer.TaskFunction<E, A>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app2);
			final TaskTriggerer.TaskFunction<E, B>
					taskFunction2 =
					(TaskTriggerer.TaskFunction<E, B>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app3);
			final TaskTriggerer.TaskFunction<E, BiFunction<A, B, R>> taskFunction3 = TaskTriggerer.getFunction(app);
			return TaskTriggerer.of(new TaskTriggerer.TaskFunction<E, R>() {
				@Override
				public R run(ServerWorld world, E entity, long time) {
					A object = taskFunction.run(world, entity, time);
					if (object == null) {
						return null;
					}
					else {
						B object2 = taskFunction2.run(world, entity, time);
						if (object2 == null) {
							return null;
						}
						else {
							BiFunction<A, B, R> biFunction = taskFunction3.run(world, entity, time);
							return biFunction == null ? null : biFunction.apply(object, object2);
						}
					}
				}

				@Override
				public String asString() {
					return taskFunction3.asString() + " * " + taskFunction.asString() + " * "
							+ taskFunction2.asString();
				}

				@Override
				public String toString() {
					return this.asString();
				}
			});
		}

		public <T1, T2, T3, R> TaskTriggerer<E, R> ap3(
				App<TaskTriggerer.K1<E>, Function3<T1, T2, T3, R>> app,
				App<TaskTriggerer.K1<E>, T1> app2,
				App<TaskTriggerer.K1<E>, T2> app3,
				App<TaskTriggerer.K1<E>, T3> app4
		) {
			final TaskTriggerer.TaskFunction<E, T1>
					taskFunction =
					(TaskTriggerer.TaskFunction<E, T1>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app2);
			final TaskTriggerer.TaskFunction<E, T2>
					taskFunction2 =
					(TaskTriggerer.TaskFunction<E, T2>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app3);
			final TaskTriggerer.TaskFunction<E, T3>
					taskFunction3 =
					(TaskTriggerer.TaskFunction<E, T3>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app4);
			final TaskTriggerer.TaskFunction<E, Function3<T1, T2, T3, R>>
					taskFunction4 =
					TaskTriggerer.getFunction(app);
			return TaskTriggerer.of(new TaskTriggerer.TaskFunction<E, R>() {
				@Override
				public R run(ServerWorld world, E entity, long time) {
					T1 object = taskFunction.run(world, entity, time);
					if (object == null) {
						return null;
					}
					else {
						T2 object2 = taskFunction2.run(world, entity, time);
						if (object2 == null) {
							return null;
						}
						else {
							T3 object3 = taskFunction3.run(world, entity, time);
							if (object3 == null) {
								return null;
							}
							else {
								Function3<T1, T2, T3, R> function3 = taskFunction4.run(world, entity, time);
								return (R) (function3 == null ? null : function3.apply(object, object2, object3));
							}
						}
					}
				}

				@Override
				public String asString() {
					return taskFunction4.asString() + " * " + taskFunction.asString() + " * " + taskFunction2.asString()
							+ " * " + taskFunction3.asString();
				}

				@Override
				public String toString() {
					return this.asString();
				}
			});
		}

		public <T1, T2, T3, T4, R> TaskTriggerer<E, R> ap4(
				App<TaskTriggerer.K1<E>, Function4<T1, T2, T3, T4, R>> app,
				App<TaskTriggerer.K1<E>, T1> app2,
				App<TaskTriggerer.K1<E>, T2> app3,
				App<TaskTriggerer.K1<E>, T3> app4,
				App<TaskTriggerer.K1<E>, T4> app5
		) {
			final TaskTriggerer.TaskFunction<E, T1>
					taskFunction =
					(TaskTriggerer.TaskFunction<E, T1>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app2);
			final TaskTriggerer.TaskFunction<E, T2>
					taskFunction2 =
					(TaskTriggerer.TaskFunction<E, T2>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app3);
			final TaskTriggerer.TaskFunction<E, T3>
					taskFunction3 =
					(TaskTriggerer.TaskFunction<E, T3>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app4);
			final TaskTriggerer.TaskFunction<E, T4>
					taskFunction4 =
					(TaskTriggerer.TaskFunction<E, T4>) TaskTriggerer.getFunction((App<TaskTriggerer.K1<E>, ?>) app5);
			final TaskTriggerer.TaskFunction<E, Function4<T1, T2, T3, T4, R>>
					taskFunction5 =
					TaskTriggerer.getFunction(app);
			return TaskTriggerer.of(
					new TaskTriggerer.TaskFunction<E, R>() {
						@Override
						public R run(ServerWorld world, E entity, long time) {
							T1 object = taskFunction.run(world, entity, time);
							if (object == null) {
								return null;
							}
							else {
								T2 object2 = taskFunction2.run(world, entity, time);
								if (object2 == null) {
									return null;
								}
								else {
									T3 object3 = taskFunction3.run(world, entity, time);
									if (object3 == null) {
										return null;
									}
									else {
										T4 object4 = taskFunction4.run(world, entity, time);
										if (object4 == null) {
											return null;
										}
										else {
											Function4<T1, T2, T3, T4, R>
													function4 =
													taskFunction5.run(world, entity, time);
											return (R) (function4 == null ? null : function4.apply(
													object,
													object2,
													object3,
													object4
											)
											);
										}
									}
								}
							}
						}

						@Override
						public String asString() {
							return taskFunction5.asString()
									+ " * "
									+ taskFunction.asString()
									+ " * "
									+ taskFunction2.asString()
									+ " * "
									+ taskFunction3.asString()
									+ " * "
									+ taskFunction4.asString();
						}

						@Override
						public String toString() {
							return this.asString();
						}
					}
			);
		}

		/**
		 * {@code Mu}.
		 */
		static final class Mu<E extends LivingEntity> implements com.mojang.datafixers.kinds.Applicative.Mu {

			private Mu() {
			}
		}
	}

	/**
	 * {@code TaskFunction}.
	 */
	interface TaskFunction<E extends LivingEntity, R> {

		@Nullable R run(ServerWorld world, E entity, long time);

		String asString();
	}

	/**
	 * {@code Trigger}.
	 */
	static final class Trigger<E extends LivingEntity> extends TaskTriggerer<E, Unit> {

		Trigger(TaskRunnable<? super E> taskRunnable) {
			super(new TaskTriggerer.TaskFunction<E, Unit>() {
				/**
				 * Run.
				 *
				 * @param serverWorld server world
				 * @param livingEntity living entity
				 * @param l l
				 *
				 * @return @Nullable Unit — результат операции
				 */
				public @Nullable Unit run(ServerWorld serverWorld, E livingEntity, long l) {
					return taskRunnable.trigger(serverWorld, livingEntity, l) ? Unit.INSTANCE : null;
				}

				@Override
				public String asString() {
					return "T[" + taskRunnable + "]";
				}
			});
		}
	}
}
