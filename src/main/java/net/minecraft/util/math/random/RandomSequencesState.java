package net.minecraft.util.math.random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Персистентное состояние, хранящее именованные случайные последовательности мира.
 * Каждая последовательность идентифицируется по {@link Identifier} и может быть
 * воспроизведена при тех же параметрах сида мира, соли и флагов включения.
 */
public class RandomSequencesState extends PersistentState {

	public static final Codec<RandomSequencesState> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.INT.fieldOf("salt").forGetter(RandomSequencesState::getSalt),
					Codec.BOOL
							.optionalFieldOf("include_world_seed", true)
							.forGetter(RandomSequencesState::shouldIncludeWorldSeed),
					Codec.BOOL
							.optionalFieldOf("include_sequence_id", true)
							.forGetter(RandomSequencesState::shouldIncludeSequenceId),
					Codec.unboundedMap(Identifier.CODEC, RandomSequence.CODEC)
							.fieldOf("sequences")
							.forGetter(state -> state.sequences)
			).apply(instance, RandomSequencesState::new)
	);

	public static final PersistentStateType<RandomSequencesState> STATE_TYPE = new PersistentStateType<>(
			"random_sequences", RandomSequencesState::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
	);

	private int salt;
	private boolean includeWorldSeed = true;
	private boolean includeSequenceId = true;
	private final Map<Identifier, RandomSequence> sequences = new Object2ObjectOpenHashMap<>();

	public RandomSequencesState() {
	}

	private RandomSequencesState(
			int salt,
			boolean includeWorldSeed,
			boolean includeSequenceId,
			Map<Identifier, RandomSequence> sequences
	) {
		this.salt = salt;
		this.includeWorldSeed = includeWorldSeed;
		this.includeSequenceId = includeSequenceId;
		this.sequences.putAll(sequences);
	}

	public Random getOrCreate(Identifier id, long worldSeed) {
		Random random = sequences.computeIfAbsent(id, idx -> createSequence(idx, worldSeed)).getSource();
		return new WrappedRandom(random);
	}

	private RandomSequence createSequence(Identifier id, long worldSeed) {
		return createSequence(id, worldSeed, salt, includeWorldSeed, includeSequenceId);
	}

	private RandomSequence createSequence(
			Identifier id,
			long worldSeed,
			int salt,
			boolean includeWorldSeed,
			boolean includeSequenceId
	) {
		long seed = (includeWorldSeed ? worldSeed : 0L) ^ salt;
		return new RandomSequence(seed, includeSequenceId ? Optional.of(id) : Optional.empty());
	}

	public void forEachSequence(BiConsumer<Identifier, RandomSequence> consumer) {
		sequences.forEach(consumer);
	}

	public void setDefaultParameters(int salt, boolean includeWorldSeed, boolean includeSequenceId) {
		this.salt = salt;
		this.includeWorldSeed = includeWorldSeed;
		this.includeSequenceId = includeSequenceId;
	}

	public int resetAll() {
		int count = sequences.size();
		sequences.clear();
		return count;
	}

	public void reset(Identifier id, long worldSeed) {
		sequences.put(id, createSequence(id, worldSeed));
	}

	public void reset(Identifier id, long worldSeed, int salt, boolean includeWorldSeed, boolean includeSequenceId) {
		sequences.put(id, createSequence(id, worldSeed, salt, includeWorldSeed, includeSequenceId));
	}

	private int getSalt() {
		return salt;
	}

	private boolean shouldIncludeWorldSeed() {
		return includeWorldSeed;
	}

	private boolean shouldIncludeSequenceId() {
		return includeSequenceId;
	}

	/**
	 * Обёртка над {@link Random}, которая помечает состояние как изменённое
	 * при каждом обращении к генератору, обеспечивая корректное сохранение.
	 */
	class WrappedRandom implements Random {

		private final Random random;

		WrappedRandom(Random random) {
			this.random = random;
		}

		@Override
		public Random split() {
			RandomSequencesState.this.markDirty();
			return random.split();
		}

		@Override
		public RandomSplitter nextSplitter() {
			RandomSequencesState.this.markDirty();
			return random.nextSplitter();
		}

		@Override
		public void setSeed(long seed) {
			RandomSequencesState.this.markDirty();
			random.setSeed(seed);
		}

		@Override
		public int nextInt() {
			RandomSequencesState.this.markDirty();
			return random.nextInt();
		}

		@Override
		public int nextInt(int bound) {
			RandomSequencesState.this.markDirty();
			return random.nextInt(bound);
		}

		@Override
		public long nextLong() {
			RandomSequencesState.this.markDirty();
			return random.nextLong();
		}

		@Override
		public boolean nextBoolean() {
			RandomSequencesState.this.markDirty();
			return random.nextBoolean();
		}

		@Override
		public float nextFloat() {
			RandomSequencesState.this.markDirty();
			return random.nextFloat();
		}

		@Override
		public double nextDouble() {
			RandomSequencesState.this.markDirty();
			return random.nextDouble();
		}

		@Override
		public double nextGaussian() {
			RandomSequencesState.this.markDirty();
			return random.nextGaussian();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			return o instanceof WrappedRandom other
					? random.equals(other.random)
					: false;
		}
	}
}
