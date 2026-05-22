package net.minecraft.entity.ai.brain;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Мозг сущности — центральная система управления поведением на основе памяти и задач.
 *
 * <p>Мозг хранит:
 * <ul>
 *   <li>Набор {@link Memory} — именованных значений с опциональным TTL</li>
 *   <li>Набор {@link Sensor} — датчиков, обновляющих память каждый тик</li>
 *   <li>Набор {@link Task} — задач, сгруппированных по {@link Activity} и приоритету</li>
 * </ul>
 *
 * <p>На каждом тике мозг: обновляет память → запускает датчики → стартует задачи → тикает задачи.
 *
 * @param <E> тип сущности, которой принадлежит мозг
 */
public class Brain<E extends LivingEntity> {

	static final Logger LOGGER = LogUtils.getLogger();

	private static final int ACTIVITY_REFRESH_COOLDOWN = 20;

	private final Supplier<Codec<Brain<E>>> codecSupplier;
	private final Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> memories = Maps.newHashMap();
	private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.newLinkedHashMap();
	private final Map<Integer, Map<Activity, Set<Task<? super E>>>> tasks = Maps.newTreeMap();
	private @Nullable EnvironmentAttribute<Activity> schedule;
	private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryModuleState>>> requiredActivityMemories = Maps.newHashMap();
	private final Map<Activity, Set<MemoryModuleType<?>>> forgettingActivityMemories = Maps.newHashMap();
	private Set<Activity> coreActivities = Sets.newHashSet();
	private final Set<Activity> possibleActivities = Sets.newHashSet();
	private Activity defaultActivity = Activity.IDLE;
	private long activityStartTime = -9999L;

	/**
	 * Создаёт профиль мозга с заданными модулями памяти и датчиками.
	 * Профиль содержит codec для сериализации и может создавать экземпляры мозга.
	 */
	public static <E extends LivingEntity> Brain.Profile<E> createProfile(
			Collection<? extends MemoryModuleType<?>> memoryModules,
			Collection<? extends SensorType<? extends Sensor<? super E>>> sensors
	) {
		return new Brain.Profile<>(memoryModules, sensors);
	}

	/**
	 * Создаёт codec для сериализации мозга с заданными модулями памяти и датчиками.
	 * Codec сериализует только те модули памяти, у которых есть собственный codec.
	 */
	public static <E extends LivingEntity> Codec<Brain<E>> createBrainCodec(
			Collection<? extends MemoryModuleType<?>> memoryModules,
			Collection<? extends SensorType<? extends Sensor<? super E>>> sensors
	) {
		final MutableObject<Codec<Brain<E>>> mutableObject = new MutableObject<>();
		mutableObject.setValue(
				(new MapCodec<Brain<E>>() {
					@Override
					@SuppressWarnings("unchecked")
					public <T> Stream<T> keys(DynamicOps<T> ops) {
						return memoryModules.stream()
								.flatMap(memoryType -> memoryType
										.getCodec()
										.map(codec -> Registries.MEMORY_MODULE_TYPE.getId(memoryType))
										.stream())
								.map(id -> (T) ops.createString(id.toString()));
					}

					@Override
					@SuppressWarnings("unchecked")
					public <T> DataResult<Brain<E>> decode(DynamicOps<T> ops, MapLike<T> map) {
						MutableObject<DataResult<Builder<Brain.MemoryEntry<?>>>> mutableObjectx =
								new MutableObject<>(DataResult.success(ImmutableList.builder()));

						map.entries().forEach(pair -> {
							DataResult<MemoryModuleType<?>> dataResult =
									Registries.MEMORY_MODULE_TYPE.getCodec().parse(ops, pair.getFirst());
							DataResult<? extends Brain.MemoryEntry<?>> dataResult2 =
									dataResult.flatMap(memoryType -> this.parse(memoryType, ops, (T) pair.getSecond()));
							mutableObjectx.setValue(mutableObjectx.get().apply2(Builder::add, dataResult2));
						});

						ImmutableList<Brain.MemoryEntry<?>> immutableList = mutableObjectx.get()
								.resultOrPartial(Brain.LOGGER::error)
								.map(Builder::build)
								.orElseGet(ImmutableList::of);

						return DataResult.success(new Brain<>(memoryModules, sensors, immutableList, mutableObject));
					}

					private <T, U> DataResult<Brain.MemoryEntry<U>> parse(
							MemoryModuleType<U> memoryType,
							DynamicOps<T> ops,
							T value
					) {
						return memoryType.getCodec()
								.map(DataResult::success)
								.orElseGet(() -> DataResult.error(() -> "No codec for memory: " + memoryType))
								.flatMap(codec -> codec.parse(ops, value))
								.map(data -> new Brain.MemoryEntry<>(memoryType, Optional.of(data)));
					}

					@Override
					public <T> RecordBuilder<T> encode(
							Brain<E> brain,
							DynamicOps<T> dynamicOps,
							RecordBuilder<T> recordBuilder
					) {
						brain.streamMemories().forEach(entry -> entry.serialize(dynamicOps, recordBuilder));
						return recordBuilder;
					}
				})
						.fieldOf("memories")
						.codec()
		);
		return mutableObject.get();
	}

	public Brain(
			Collection<? extends MemoryModuleType<?>> memories,
			Collection<? extends SensorType<? extends Sensor<? super E>>> sensors,
			ImmutableList<Brain.MemoryEntry<?>> memoryEntries,
			Supplier<Codec<Brain<E>>> codecSupplier
	) {
		this.codecSupplier = codecSupplier;

		for (MemoryModuleType<?> memoryType : memories) {
			this.memories.put(memoryType, Optional.empty());
		}

		for (SensorType<? extends Sensor<? super E>> sensorType : sensors) {
			this.sensors.put(sensorType, (Sensor<? super E>) sensorType.create());
		}

		for (Sensor<? super E> sensor : this.sensors.values()) {
			for (MemoryModuleType<?> outputMemory : sensor.getOutputMemoryModules()) {
				this.memories.put(outputMemory, Optional.empty());
			}
		}

		for (Brain.MemoryEntry<?> memoryEntry : memoryEntries) {
			memoryEntry.apply(this);
		}
	}

	public <T> DataResult<T> encode(DynamicOps<T> ops) {
		return codecSupplier.get().encodeStart(ops, this);
	}

	Stream<Brain.MemoryEntry<?>> streamMemories() {
		return memories.entrySet().stream().map(entry -> Brain.MemoryEntry.of(entry.getKey(), entry.getValue()));
	}

	public boolean hasMemoryModule(MemoryModuleType<?> type) {
		return isMemoryInState(type, MemoryModuleState.VALUE_PRESENT);
	}

	public void forgetAll() {
		memories.keySet().forEach(type -> memories.put(type, Optional.empty()));
	}

	public <U> void forget(MemoryModuleType<U> type) {
		remember(type, Optional.empty());
	}

	public <U> void remember(MemoryModuleType<U> type, @Nullable U value) {
		remember(type, Optional.ofNullable(value));
	}

	public <U> void remember(MemoryModuleType<U> type, U value, long expiry) {
		setMemory(type, Optional.of(Memory.timed(value, expiry)));
	}

	public <U> void remember(MemoryModuleType<U> type, Optional<? extends U> value) {
		setMemory(type, value.map(Memory::permanent));
	}

	<U> void setMemory(MemoryModuleType<U> type, Optional<? extends Memory<?>> memory) {
		if (!memories.containsKey(type)) {
			return;
		}

		if (memory.isPresent() && isEmptyCollection(memory.get().getValue())) {
			forget(type);
		} else {
			memories.put(type, memory);
		}
	}

	/**
	 * Возвращает значение зарегистрированного модуля памяти.
	 *
	 * @throws IllegalStateException если модуль памяти не зарегистрирован в этом мозге
	 */
	@SuppressWarnings("unchecked")
	public <U> Optional<U> getOptionalRegisteredMemory(MemoryModuleType<U> type) {
		Optional<? extends Memory<?>> optional = memories.get(type);
		if (optional == null) {
			throw new IllegalStateException("Unregistered memory fetched: " + type);
		}

		return (Optional<U>) optional.map(Memory::getValue);
	}

	@SuppressWarnings("unchecked")
	public <U> @Nullable Optional<U> getOptionalMemory(MemoryModuleType<U> type) {
		Optional<? extends Memory<?>> optional = memories.get(type);
		return optional == null ? null : (Optional<U>) optional.map(Memory::getValue);
	}

	public <U> long getMemoryExpiry(MemoryModuleType<U> type) {
		Optional<? extends Memory<?>> optional = memories.get(type);
		return optional.map(Memory::getExpiry).orElse(0L);
	}

	@Deprecated
	@Debug
	public Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> getMemories() {
		return memories;
	}

	public <U> boolean hasMemoryModuleWithValue(MemoryModuleType<U> type, U value) {
		if (!hasMemoryModule(type)) {
			return false;
		}

		return getOptionalRegisteredMemory(type)
				.filter(memoryValue -> memoryValue.equals(value))
				.isPresent();
	}

	/**
	 * Проверяет, находится ли модуль памяти в заданном состоянии.
	 * Возвращает {@code false}, если модуль не зарегистрирован.
	 */
	public boolean isMemoryInState(MemoryModuleType<?> type, MemoryModuleState state) {
		Optional<? extends Memory<?>> optional = memories.get(type);
		if (optional == null) {
			return false;
		}

		return state == MemoryModuleState.REGISTERED
				|| state == MemoryModuleState.VALUE_PRESENT && optional.isPresent()
				|| state == MemoryModuleState.VALUE_ABSENT && optional.isEmpty();
	}

	public void setSchedule(EnvironmentAttribute<Activity> schedule) {
		this.schedule = schedule;
	}

	public void setCoreActivities(Set<Activity> coreActivities) {
		this.coreActivities = coreActivities;
	}

	@Deprecated
	@Debug
	public Set<Activity> getPossibleActivities() {
		return possibleActivities;
	}

	@Deprecated
	@Debug
	public List<Task<? super E>> getRunningTasks() {
		List<Task<? super E>> running = new ObjectArrayList<>();

		for (Map<Activity, Set<Task<? super E>>> map : tasks.values()) {
			for (Set<Task<? super E>> set : map.values()) {
				for (Task<? super E> task : set) {
					if (task.getStatus() == MultiTickTask.Status.RUNNING) {
						running.add(task);
					}
				}
			}
		}

		return running;
	}

	/**
	 * Сбрасывает активные активности к активности по умолчанию.
	 */
	public void resetPossibleActivities() {
		resetPossibleActivities(defaultActivity);
	}

	public Optional<Activity> getFirstPossibleNonCoreActivity() {
		for (Activity activity : possibleActivities) {
			if (!coreActivities.contains(activity)) {
				return Optional.of(activity);
			}
		}

		return Optional.empty();
	}

	public void doExclusively(Activity activity) {
		if (canDoActivity(activity)) {
			resetPossibleActivities(activity);
		} else {
			resetPossibleActivities();
		}
	}

	private void resetPossibleActivities(Activity except) {
		if (hasActivity(except)) {
			return;
		}

		forgetIrrelevantMemories(except);
		possibleActivities.clear();
		possibleActivities.addAll(coreActivities);
		possibleActivities.add(except);
	}

	private void forgetIrrelevantMemories(Activity except) {
		for (Activity activity : possibleActivities) {
			if (activity == except) {
				continue;
			}

			Set<MemoryModuleType<?>> toForget = forgettingActivityMemories.get(activity);
			if (toForget == null) {
				continue;
			}

			for (MemoryModuleType<?> memoryType : toForget) {
				forget(memoryType);
			}
		}
	}

	/**
	 * Обновляет активную активность по расписанию, если прошло достаточно тиков.
	 * Переключение происходит не чаще одного раза в {@code ACTIVITY_REFRESH_COOLDOWN} тиков.
	 */
	public void refreshActivities(WorldEnvironmentAttributeAccess attributeAccess, long time, Vec3d pos) {
		if (time - activityStartTime <= ACTIVITY_REFRESH_COOLDOWN) {
			return;
		}

		activityStartTime = time;
		Activity activity = schedule != null ? attributeAccess.getAttributeValue(schedule, pos) : Activity.IDLE;

		if (!possibleActivities.contains(activity)) {
			doExclusively(activity);
		}
	}

	public void resetPossibleActivities(List<Activity> activities) {
		for (Activity activity : activities) {
			if (canDoActivity(activity)) {
				resetPossibleActivities(activity);
				break;
			}
		}
	}

	public void setDefaultActivity(Activity activity) {
		defaultActivity = activity;
	}

	public void setTaskList(Activity activity, int begin, ImmutableList<? extends Task<? super E>> list) {
		setTaskList(activity, indexTaskList(begin, list));
	}

	public void setTaskList(
			Activity activity,
			int begin,
			ImmutableList<? extends Task<? super E>> taskList,
			MemoryModuleType<?> memoryType
	) {
		Set<Pair<MemoryModuleType<?>, MemoryModuleState>> required = ImmutableSet.of(
				Pair.of(memoryType, MemoryModuleState.VALUE_PRESENT)
		);
		Set<MemoryModuleType<?>> forgetting = ImmutableSet.of(memoryType);
		setTaskList(activity, indexTaskList(begin, taskList), required, forgetting);
	}

	public void setTaskList(
			Activity activity,
			ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks
	) {
		setTaskList(activity, indexedTasks, ImmutableSet.of(), Sets.newHashSet());
	}

	public void setTaskList(
			Activity activity,
			int begin,
			ImmutableList<? extends Task<? super E>> taskList,
			Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories
	) {
		setTaskList(activity, indexTaskList(begin, taskList), requiredMemories);
	}

	public void setTaskList(
			Activity activity,
			ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks,
			Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories
	) {
		setTaskList(activity, indexedTasks, requiredMemories, Sets.newHashSet());
	}

	public void setTaskList(
			Activity activity,
			ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks,
			Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories,
			Set<MemoryModuleType<?>> forgettingMemories
	) {
		requiredActivityMemories.put(activity, requiredMemories);

		if (!forgettingMemories.isEmpty()) {
			forgettingActivityMemories.put(activity, forgettingMemories);
		}

		for (Pair<Integer, ? extends Task<? super E>> pair : indexedTasks) {
			tasks.computeIfAbsent(pair.getFirst(), index -> Maps.newHashMap())
					.computeIfAbsent(activity, a -> Sets.newLinkedHashSet())
					.add(pair.getSecond());
		}
	}

	@VisibleForTesting
	public void clear() {
		tasks.clear();
	}

	public boolean hasActivity(Activity activity) {
		return possibleActivities.contains(activity);
	}

	public Brain<E> copy() {
		Brain<E> copy = new Brain<>(memories.keySet(), sensors.keySet(), ImmutableList.of(), codecSupplier);

		for (Entry<MemoryModuleType<?>, Optional<? extends Memory<?>>> entry : memories.entrySet()) {
			if (entry.getValue().isPresent()) {
				copy.memories.put(entry.getKey(), entry.getValue());
			}
		}

		return copy;
	}

	public void tick(ServerWorld world, E entity) {
		tickMemories();
		tickSensors(world, entity);
		startTasks(world, entity);
		updateTasks(world, entity);
	}

	private void tickSensors(ServerWorld world, E entity) {
		for (Sensor<? super E> sensor : sensors.values()) {
			sensor.tick(world, entity);
		}
	}

	private void tickMemories() {
		for (Entry<MemoryModuleType<?>, Optional<? extends Memory<?>>> entry : memories.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}

			Memory<?> memory = entry.getValue().get();
			if (memory.isExpired()) {
				forget(entry.getKey());
			}

			memory.tick();
		}
	}

	/**
	 * Останавливает все активные задачи сущности.
	 */
	public void stopAllTasks(ServerWorld world, E entity) {
		long time = entity.getEntityWorld().getTime();

		for (Task<? super E> task : getRunningTasks()) {
			task.stop(world, entity, time);
		}
	}

	private void startTasks(ServerWorld world, E entity) {
		long time = world.getTime();

		for (Map<Activity, Set<Task<? super E>>> map : tasks.values()) {
			for (Entry<Activity, Set<Task<? super E>>> entry : map.entrySet()) {
				Activity activity = entry.getKey();
				if (!possibleActivities.contains(activity)) {
					continue;
				}

				for (Task<? super E> task : entry.getValue()) {
					if (task.getStatus() == MultiTickTask.Status.STOPPED) {
						task.tryStarting(world, entity, time);
					}
				}
			}
		}
	}

	private void updateTasks(ServerWorld world, E entity) {
		long time = world.getTime();

		for (Task<? super E> task : getRunningTasks()) {
			task.tick(world, entity, time);
		}
	}

	/**
	 * Проверяет, выполнены ли все требования к памяти для запуска активности.
	 */
	private boolean canDoActivity(Activity activity) {
		if (!requiredActivityMemories.containsKey(activity)) {
			return false;
		}

		for (Pair<MemoryModuleType<?>, MemoryModuleState> pair : requiredActivityMemories.get(activity)) {
			MemoryModuleType<?> memoryType = pair.getFirst();
			MemoryModuleState requiredState = pair.getSecond();
			if (!isMemoryInState(memoryType, requiredState)) {
				return false;
			}
		}

		return true;
	}

	@SuppressWarnings("rawtypes")
	private boolean isEmptyCollection(Object value) {
		return value instanceof Collection collection && collection.isEmpty();
	}

	ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexTaskList(
			int begin,
			ImmutableList<? extends Task<? super E>> taskList
	) {
		int index = begin;
		Builder<Pair<Integer, ? extends Task<? super E>>> builder = ImmutableList.builder();

		for (Task<? super E> task : taskList) {
			builder.add(Pair.of(index++, task));
		}

		return builder.build();
	}

	public boolean isEmpty() {
		return memories.isEmpty() && sensors.isEmpty() && tasks.isEmpty();
	}

	static final class MemoryEntry<U> {

		private final MemoryModuleType<U> type;
		private final Optional<? extends Memory<U>> data;

		static <U> Brain.MemoryEntry<U> of(MemoryModuleType<U> type, Optional<? extends Memory<?>> data) {
			return new Brain.MemoryEntry<>(type, (Optional<? extends Memory<U>>) data);
		}

		MemoryEntry(MemoryModuleType<U> type, Optional<? extends Memory<U>> data) {
			this.type = type;
			this.data = data;
		}

		void apply(Brain<?> brain) {
			brain.setMemory(type, data);
		}

		public <T> void serialize(DynamicOps<T> ops, RecordBuilder<T> builder) {
			type.getCodec()
					.ifPresent(
							codec -> data.ifPresent(
									d -> builder.add(
											Registries.MEMORY_MODULE_TYPE.getCodec().encodeStart(ops, type),
											codec.encodeStart(ops, d)
									)
							)
					);
		}
	}

	/**
	 * Профиль мозга — фабрика для создания экземпляров {@link Brain} с заданными
	 * модулями памяти и датчиками. Содержит codec для десериализации из NBT.
	 */
	public static final class Profile<E extends LivingEntity> {

		private final Collection<? extends MemoryModuleType<?>> memoryModules;
		private final Collection<? extends SensorType<? extends Sensor<? super E>>> sensors;
		private final Codec<Brain<E>> codec;

		Profile(
				Collection<? extends MemoryModuleType<?>> memoryModules,
				Collection<? extends SensorType<? extends Sensor<? super E>>> sensors
		) {
			this.memoryModules = memoryModules;
			this.sensors = sensors;
			this.codec = Brain.createBrainCodec(memoryModules, sensors);
		}

		/**
		 * Десериализует мозг из динамических данных (NBT).
		 * При ошибке парсинга возвращает пустой мозг с зарегистрированными модулями.
		 */
		public Brain<E> deserialize(Dynamic<?> data) {
			return codec.parse(data)
					.resultOrPartial(Brain.LOGGER::error)
					.orElseGet(() -> new Brain<>(
							memoryModules,
							sensors,
							ImmutableList.of(),
							() -> codec
					));
		}
	}
}
