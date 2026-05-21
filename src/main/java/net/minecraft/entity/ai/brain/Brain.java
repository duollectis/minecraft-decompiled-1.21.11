package net.minecraft.entity.ai.brain;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;
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

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * {@code Brain}.
 */
public class Brain<E extends LivingEntity> {

	static final Logger LOGGER = LogUtils.getLogger();
	private final Supplier<Codec<Brain<E>>> codecSupplier;
	private static final int ACTIVITY_REFRESH_COOLDOWN = 20;
	private final Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> memories = Maps.newHashMap();
	private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.newLinkedHashMap();
	private final Map<Integer, Map<Activity, Set<Task<? super E>>>> tasks = Maps.newTreeMap();
	private @Nullable EnvironmentAttribute<Activity> schedule;
	private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryModuleState>>>
			requiredActivityMemories =
			Maps.newHashMap();
	private final Map<Activity, Set<MemoryModuleType<?>>> forgettingActivityMemories = Maps.newHashMap();
	private Set<Activity> coreActivities = Sets.newHashSet();
	private final Set<Activity> possibleActivities = Sets.newHashSet();
	private Activity defaultActivity = Activity.IDLE;
	private long activityStartTime = -9999L;

	public static <E extends LivingEntity> Brain.Profile<E> createProfile(
			Collection<? extends MemoryModuleType<?>> memoryModules,
			Collection<? extends SensorType<? extends Sensor<? super E>>> sensors
	) {
		return new Brain.Profile<>(memoryModules, sensors);
	}

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
						MutableObject<DataResult<Builder<Brain.MemoryEntry<?>>>>
								mutableObjectx =
								new MutableObject<>(DataResult.success(ImmutableList.builder()));
						map.entries().forEach(pair -> {
							DataResult<MemoryModuleType<?>>
									dataResult =
									Registries.MEMORY_MODULE_TYPE.getCodec().parse(ops, pair.getFirst());
							DataResult<? extends Brain.MemoryEntry<?>>
									dataResult2 =
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
				}
				)
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

		for (MemoryModuleType<?> memoryModuleType : memories) {
			this.memories.put(memoryModuleType, Optional.empty());
		}

		for (SensorType<? extends Sensor<? super E>> sensorType : sensors) {
			this.sensors.put(sensorType, (Sensor<? super E>) sensorType.create());
		}

		for (Sensor<? super E> sensor : this.sensors.values()) {
			for (MemoryModuleType<?> memoryModuleType2 : sensor.getOutputMemoryModules()) {
				this.memories.put(memoryModuleType2, Optional.empty());
			}
		}

		UnmodifiableIterator var11 = memoryEntries.iterator();

		while (var11.hasNext()) {
			Brain.MemoryEntry<?> memoryEntry = (Brain.MemoryEntry<?>) var11.next();
			memoryEntry.apply(this);
		}
	}

	/**
	 * Encode.
	 *
	 * @param ops ops
	 *
	 * @return DataResult — результат операции
	 */
	public <T> DataResult<T> encode(DynamicOps<T> ops) {
		return this.codecSupplier.get().encodeStart(ops, this);
	}

	Stream<Brain.MemoryEntry<?>> streamMemories() {
		return this.memories.entrySet().stream().map(entry -> Brain.MemoryEntry.of(entry.getKey(), entry.getValue()));
	}

	public boolean hasMemoryModule(MemoryModuleType<?> type) {
		return this.isMemoryInState(type, MemoryModuleState.VALUE_PRESENT);
	}

	/**
	 * Forget all.
	 */
	public void forgetAll() {
		this.memories.keySet().forEach(type -> this.memories.put((MemoryModuleType<?>) type, Optional.empty()));
	}

	/**
	 * Forget.
	 *
	 * @param type type
	 *
	 * @return void — результат операции
	 */
	public <U> void forget(MemoryModuleType<U> type) {
		this.remember(type, Optional.empty());
	}

	/**
	 * Remember.
	 *
	 * @param type type
	 * @param value value
	 *
	 * @return void — результат операции
	 */
	public <U> void remember(MemoryModuleType<U> type, @Nullable U value) {
		this.remember(type, Optional.ofNullable(value));
	}

	/**
	 * Remember.
	 *
	 * @param type type
	 * @param value value
	 * @param expiry expiry
	 *
	 * @return void — результат операции
	 */
	public <U> void remember(MemoryModuleType<U> type, U value, long expiry) {
		this.setMemory(type, Optional.of(Memory.timed(value, expiry)));
	}

	/**
	 * Remember.
	 *
	 * @param type type
	 * @param value value
	 *
	 * @return void — результат операции
	 */
	public <U> void remember(MemoryModuleType<U> type, Optional<? extends U> value) {
		this.setMemory(type, value.map(Memory::permanent));
	}

	<U> void setMemory(MemoryModuleType<U> type, Optional<? extends Memory<?>> memory) {
		if (this.memories.containsKey(type)) {
			if (memory.isPresent() && this.isEmptyCollection(memory.get().getValue())) {
				this.forget(type);
			}
			else {
				this.memories.put(type, memory);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <U> Optional<U> getOptionalRegisteredMemory(MemoryModuleType<U> type) {
		Optional<? extends Memory<?>> optional = this.memories.get(type);
		if (optional == null) {
			throw new IllegalStateException("Unregistered memory fetched: " + type);
		}
		return (Optional<U>) optional.map(Memory::getValue);
	}

	@SuppressWarnings("unchecked")
	public <U> @Nullable Optional<U> getOptionalMemory(MemoryModuleType<U> type) {
		Optional<? extends Memory<?>> optional = this.memories.get(type);
		return optional == null ? null : (Optional<U>) optional.map(Memory::getValue);
	}

	public <U> long getMemoryExpiry(MemoryModuleType<U> type) {
		Optional<? extends Memory<?>> optional = this.memories.get(type);
		return optional.map(Memory::getExpiry).orElse(0L);
	}

	@Deprecated
	@Debug
	public Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> getMemories() {
		return this.memories;
	}

	public <U> boolean hasMemoryModuleWithValue(MemoryModuleType<U> type, U value) {
		return !this.hasMemoryModule(type) ? false : this
		                                             .getOptionalRegisteredMemory(type)
		                                             .filter(memoryValue -> memoryValue.equals(value))
		                                             .isPresent();
	}

	public boolean isMemoryInState(MemoryModuleType<?> type, MemoryModuleState state) {
		Optional<? extends Memory<?>> optional = this.memories.get(type);
		return optional == null
		       ? false
		       : state == MemoryModuleState.REGISTERED
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
		return this.possibleActivities;
	}

	@Deprecated
	@Debug
	public List<Task<? super E>> getRunningTasks() {
		List<Task<? super E>> list = new ObjectArrayList();

		for (Map<Activity, Set<Task<? super E>>> map : this.tasks.values()) {
			for (Set<Task<? super E>> set : map.values()) {
				for (Task<? super E> task : set) {
					if (task.getStatus() == MultiTickTask.Status.RUNNING) {
						list.add(task);
					}
				}
			}
		}

		return list;
	}

	/**
	 * Сбрасывает possible activities.
	 */
	public void resetPossibleActivities() {
		this.resetPossibleActivities(this.defaultActivity);
	}

	public Optional<Activity> getFirstPossibleNonCoreActivity() {
		for (Activity activity : this.possibleActivities) {
			if (!this.coreActivities.contains(activity)) {
				return Optional.of(activity);
			}
		}

		return Optional.empty();
	}

	/**
	 * Do exclusively.
	 *
	 * @param activity activity
	 */
	public void doExclusively(Activity activity) {
		if (this.canDoActivity(activity)) {
			this.resetPossibleActivities(activity);
		}
		else {
			this.resetPossibleActivities();
		}
	}

	private void resetPossibleActivities(Activity except) {
		if (!this.hasActivity(except)) {
			this.forgetIrrelevantMemories(except);
			this.possibleActivities.clear();
			this.possibleActivities.addAll(this.coreActivities);
			this.possibleActivities.add(except);
		}
	}

	private void forgetIrrelevantMemories(Activity except) {
		for (Activity activity : this.possibleActivities) {
			if (activity != except) {
				Set<MemoryModuleType<?>> set = this.forgettingActivityMemories.get(activity);
				if (set != null) {
					for (MemoryModuleType<?> memoryModuleType : set) {
						this.forget(memoryModuleType);
					}
				}
			}
		}
	}

	/**
	 * Refresh activities.
	 *
	 * @param attributeAccess attribute access
	 * @param time time
	 * @param pos pos
	 */
	public void refreshActivities(WorldEnvironmentAttributeAccess attributeAccess, long time, Vec3d pos) {
		if (time - this.activityStartTime > 20L) {
			this.activityStartTime = time;
			Activity
					activity =
					this.schedule != null ? attributeAccess.getAttributeValue(this.schedule, pos) : Activity.IDLE;
			if (!this.possibleActivities.contains(activity)) {
				this.doExclusively(activity);
			}
		}
	}

	/**
	 * Сбрасывает possible activities.
	 *
	 * @param activities activities
	 */
	public void resetPossibleActivities(List<Activity> activities) {
		for (Activity activity : activities) {
			if (this.canDoActivity(activity)) {
				this.resetPossibleActivities(activity);
				break;
			}
		}
	}

	public void setDefaultActivity(Activity activity) {
		this.defaultActivity = activity;
	}

	public void setTaskList(Activity activity, int begin, ImmutableList<? extends Task<? super E>> list) {
		this.setTaskList(activity, this.indexTaskList(begin, list));
	}

	public void setTaskList(
			Activity activity,
			int begin,
			ImmutableList<? extends Task<? super E>> tasks,
			MemoryModuleType<?> memoryType
	) {
		Set<Pair<MemoryModuleType<?>, MemoryModuleState>>
				set =
				ImmutableSet.of(Pair.of(memoryType, MemoryModuleState.VALUE_PRESENT));
		Set<MemoryModuleType<?>> set2 = ImmutableSet.of(memoryType);
		this.setTaskList(activity, this.indexTaskList(begin, tasks), set, set2);
	}

	public void setTaskList(
			Activity activity,
			ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks
	) {
		this.setTaskList(activity, indexedTasks, ImmutableSet.of(), Sets.newHashSet());
	}

	public void setTaskList(
			Activity activity,
			int begin,
			ImmutableList<? extends Task<? super E>> tasks,
			Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories
	) {
		this.setTaskList(activity, this.indexTaskList(begin, tasks), requiredMemories);
	}

	public void setTaskList(
			Activity activity,
			ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks,
			Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories
	) {
		this.setTaskList(activity, indexedTasks, requiredMemories, Sets.newHashSet());
	}

	public void setTaskList(
			Activity activity,
			ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexedTasks,
			Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories,
			Set<MemoryModuleType<?>> forgettingMemories
	) {
		this.requiredActivityMemories.put(activity, requiredMemories);
		if (!forgettingMemories.isEmpty()) {
			this.forgettingActivityMemories.put(activity, forgettingMemories);
		}

		UnmodifiableIterator var5 = indexedTasks.iterator();

		while (var5.hasNext()) {
			Pair<Integer, ? extends Task<? super E>> pair = (Pair<Integer, ? extends Task<? super E>>) var5.next();
			this.tasks
					.computeIfAbsent((Integer) pair.getFirst(), index -> Maps.newHashMap())
					.computeIfAbsent(activity, activity2 -> Sets.newLinkedHashSet())
					.add((Task<? super E>) pair.getSecond());
		}
	}

	@VisibleForTesting
	/**
	 * Clear.
	 */
	public void clear() {
		this.tasks.clear();
	}

	public boolean hasActivity(Activity activity) {
		return this.possibleActivities.contains(activity);
	}

	/**
	 * Copy.
	 *
	 * @return Brain — результат операции
	 */
	public Brain<E> copy() {
		Brain<E>
				brain =
				new Brain<>(this.memories.keySet(), this.sensors.keySet(), ImmutableList.of(), this.codecSupplier);

		for (Entry<MemoryModuleType<?>, Optional<? extends Memory<?>>> entry : this.memories.entrySet()) {
			MemoryModuleType<?> memoryModuleType = entry.getKey();
			if (entry.getValue().isPresent()) {
				brain.memories.put(memoryModuleType, entry.getValue());
			}
		}

		return brain;
	}

	/**
	 * Tick.
	 *
	 * @param world world
	 * @param entity entity
	 */
	public void tick(ServerWorld world, E entity) {
		this.tickMemories();
		this.tickSensors(world, entity);
		this.startTasks(world, entity);
		this.updateTasks(world, entity);
	}

	private void tickSensors(ServerWorld world, E entity) {
		for (Sensor<? super E> sensor : this.sensors.values()) {
			sensor.tick(world, entity);
		}
	}

	private void tickMemories() {
		for (Entry<MemoryModuleType<?>, Optional<? extends Memory<?>>> entry : this.memories.entrySet()) {
			if (entry.getValue().isPresent()) {
				Memory<?> memory = (Memory<?>) entry.getValue().get();
				if (memory.isExpired()) {
					this.forget(entry.getKey());
				}

				memory.tick();
			}
		}
	}

	/**
	 * Останавливает all tasks.
	 *
	 * @param world world
	 * @param entity entity
	 */
	public void stopAllTasks(ServerWorld world, E entity) {
		long l = entity.getEntityWorld().getTime();

		for (Task<? super E> task : this.getRunningTasks()) {
			task.stop(world, entity, l);
		}
	}

	private void startTasks(ServerWorld world, E entity) {
		long l = world.getTime();

		for (Map<Activity, Set<Task<? super E>>> map : this.tasks.values()) {
			for (Entry<Activity, Set<Task<? super E>>> entry : map.entrySet()) {
				Activity activity = entry.getKey();
				if (this.possibleActivities.contains(activity)) {
					for (Task<? super E> task : entry.getValue()) {
						if (task.getStatus() == MultiTickTask.Status.STOPPED) {
							task.tryStarting(world, entity, l);
						}
					}
				}
			}
		}
	}

	private void updateTasks(ServerWorld world, E entity) {
		long l = world.getTime();

		for (Task<? super E> task : this.getRunningTasks()) {
			task.tick(world, entity, l);
		}
	}

	private boolean canDoActivity(Activity activity) {
		if (!this.requiredActivityMemories.containsKey(activity)) {
			return false;
		}
		else {
			for (Pair<MemoryModuleType<?>, MemoryModuleState> pair : this.requiredActivityMemories.get(activity)) {
				MemoryModuleType<?> memoryModuleType = (MemoryModuleType<?>) pair.getFirst();
				MemoryModuleState memoryModuleState = (MemoryModuleState) pair.getSecond();
				if (!this.isMemoryInState(memoryModuleType, memoryModuleState)) {
					return false;
				}
			}

			return true;
		}
	}

	private boolean isEmptyCollection(Object value) {
		return value instanceof Collection && ((Collection) value).isEmpty();
	}

	ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> indexTaskList(
			int begin,
			ImmutableList<? extends Task<? super E>> tasks
	) {
		int i = begin;
		Builder<Pair<Integer, ? extends Task<? super E>>> builder = ImmutableList.builder();
		UnmodifiableIterator var5 = tasks.iterator();

		while (var5.hasNext()) {
			Task<? super E> task = (Task<? super E>) var5.next();
			builder.add(Pair.of(i++, task));
		}

		return builder.build();
	}

	public boolean isEmpty() {
		return this.memories.isEmpty() && this.sensors.isEmpty() && this.tasks.isEmpty();
	}

	/**
	 * {@code MemoryEntry}.
	 */
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
			brain.setMemory(this.type, this.data);
		}

		/**
		 * Serialize.
		 *
		 * @param ops ops
		 * @param builder builder
		 *
		 * @return void — результат операции
		 */
		public <T> void serialize(DynamicOps<T> ops, RecordBuilder<T> builder) {
			this.type
					.getCodec()
					.ifPresent(
							codec -> this.data
									.ifPresent(data -> builder.add(
											Registries.MEMORY_MODULE_TYPE
													.getCodec()
													.encodeStart(ops, this.type), codec.encodeStart(ops, data)
									))
					);
		}
	}

	/**
	 * {@code Profile}.
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
		 * Deserialize.
		 *
		 * @param data data
		 *
		 * @return Brain — результат операции
		 */
		public Brain<E> deserialize(Dynamic<?> data) {
			return this.codec
					.parse(data)
					.resultOrPartial(Brain.LOGGER::error)
					.orElseGet(() -> new Brain<>(
							this.memoryModules,
							this.sensors,
							ImmutableList.of(),
							() -> this.codec
					));
		}
	}
}
