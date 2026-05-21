package net.minecraft.world.timer.stopwatch;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/**
 * {@code StopwatchPersistentState}.
 */
public class StopwatchPersistentState extends PersistentState {

	private static final Codec<StopwatchPersistentState> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.LONG)
	                                                                  .fieldOf("stopwatches")
	                                                                  .codec()
	                                                                  .xmap(
			                                                                  StopwatchPersistentState::fromElapsedTimes,
			                                                                  StopwatchPersistentState::toElapsedTimes
	                                                                  );
	public static final PersistentStateType<StopwatchPersistentState> STATE_TYPE = new PersistentStateType<>(
			"stopwatches", StopwatchPersistentState::new, CODEC, DataFixTypes.SAVED_DATA_STOPWATCHES
	);
	private final Map<Identifier, Stopwatch> stopwatches = new Object2ObjectOpenHashMap();

	private StopwatchPersistentState() {
	}

	private static StopwatchPersistentState fromElapsedTimes(Map<Identifier, Long> times) {
		StopwatchPersistentState stopwatchPersistentState = new StopwatchPersistentState();
		long l = getTimeMs();
		times.forEach((id, time) -> stopwatchPersistentState.stopwatches.put(id, new Stopwatch(l, time)));
		return stopwatchPersistentState;
	}

	private Map<Identifier, Long> toElapsedTimes() {
		long l = getTimeMs();
		Map<Identifier, Long> map = new TreeMap<>();
		this.stopwatches.forEach((id, stopwatch) -> map.put(id, stopwatch.getElapsedTimeMs(l)));
		return map;
	}

	public @Nullable Stopwatch get(Identifier id) {
		return this.stopwatches.get(id);
	}

	public boolean add(Identifier id, Stopwatch stopwatch) {
		if (this.stopwatches.putIfAbsent(id, stopwatch) == null) {
			this.markDirty();
			return true;
		}
		else {
			return false;
		}
	}

	public boolean update(Identifier id, UnaryOperator<Stopwatch> f) {
		if (this.stopwatches.computeIfPresent(id, (id_, stopwatch) -> f.apply(stopwatch)) != null) {
			this.markDirty();
			return true;
		}
		else {
			return false;
		}
	}

	public boolean remove(Identifier id) {
		boolean bl = this.stopwatches.remove(id) != null;
		if (bl) {
			this.markDirty();
		}

		return bl;
	}

	@Override
	public boolean isDirty() {
		return super.isDirty() || !this.stopwatches.isEmpty();
	}

	public List<Identifier> keys() {
		return List.copyOf(this.stopwatches.keySet());
	}

	public static long getTimeMs() {
		return Util.getMeasuringTimeMs();
	}
}
