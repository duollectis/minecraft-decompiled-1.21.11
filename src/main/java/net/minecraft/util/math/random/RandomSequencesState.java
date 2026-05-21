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
 * {@code RandomSequencesState}.
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
					                    Codec
							                    .unboundedMap(Identifier.CODEC, RandomSequence.CODEC)
							                    .fieldOf("sequences")
							                    .forGetter(randomSequencesState -> randomSequencesState.sequences)
			                    )
			                    .apply(instance, RandomSequencesState::new)
	);
	public static final PersistentStateType<RandomSequencesState> STATE_TYPE = new PersistentStateType<>(
			"random_sequences", RandomSequencesState::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
	);
	private int salt;
	private boolean includeWorldSeed = true;
	private boolean includeSequenceId = true;
	private final Map<Identifier, RandomSequence> sequences = new Object2ObjectOpenHashMap();

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
		Random random = this.sequences.computeIfAbsent(id, idx -> this.createSequence(idx, worldSeed)).getSource();
		return new RandomSequencesState.WrappedRandom(random);
	}

	private RandomSequence createSequence(Identifier id, long worldSeed) {
		return this.createSequence(id, worldSeed, this.salt, this.includeWorldSeed, this.includeSequenceId);
	}

	private RandomSequence createSequence(
			Identifier id,
			long worldSeed,
			int salt,
			boolean includeWorldSeed,
			boolean includeSequenceId
	) {
		long l = (includeWorldSeed ? worldSeed : 0L) ^ salt;
		return new RandomSequence(l, includeSequenceId ? Optional.of(id) : Optional.empty());
	}

	public void forEachSequence(BiConsumer<Identifier, RandomSequence> consumer) {
		this.sequences.forEach(consumer);
	}

	public void setDefaultParameters(int salt, boolean includeWorldSeed, boolean includeSequenceId) {
		this.salt = salt;
		this.includeWorldSeed = includeWorldSeed;
		this.includeSequenceId = includeSequenceId;
	}

	public int resetAll() {
		int i = this.sequences.size();
		this.sequences.clear();
		return i;
	}

	public void reset(Identifier id, long worldSeed) {
		this.sequences.put(id, this.createSequence(id, worldSeed));
	}

	public void reset(Identifier id, long worldSeed, int salt, boolean includeWorldSeed, boolean includeSequenceId) {
		this.sequences.put(id, this.createSequence(id, worldSeed, salt, includeWorldSeed, includeSequenceId));
	}

	private int getSalt() {
		return this.salt;
	}

	private boolean shouldIncludeWorldSeed() {
		return this.includeWorldSeed;
	}

	private boolean shouldIncludeSequenceId() {
		return this.includeSequenceId;
	}

	/**
	 * {@code WrappedRandom}.
	 */
	class WrappedRandom implements Random {

		private final Random random;

		WrappedRandom(final Random random) {
			this.random = random;
		}

		@Override
		public Random split() {
			RandomSequencesState.this.markDirty();
			return this.random.split();
		}

		@Override
		public RandomSplitter nextSplitter() {
			RandomSequencesState.this.markDirty();
			return this.random.nextSplitter();
		}

		@Override
		public void setSeed(long seed) {
			RandomSequencesState.this.markDirty();
			this.random.setSeed(seed);
		}

		@Override
		public int nextInt() {
			RandomSequencesState.this.markDirty();
			return this.random.nextInt();
		}

		@Override
		public int nextInt(int bound) {
			RandomSequencesState.this.markDirty();
			return this.random.nextInt(bound);
		}

		@Override
		public long nextLong() {
			RandomSequencesState.this.markDirty();
			return this.random.nextLong();
		}

		@Override
		public boolean nextBoolean() {
			RandomSequencesState.this.markDirty();
			return this.random.nextBoolean();
		}

		@Override
		public float nextFloat() {
			RandomSequencesState.this.markDirty();
			return this.random.nextFloat();
		}

		@Override
		public double nextDouble() {
			RandomSequencesState.this.markDirty();
			return this.random.nextDouble();
		}

		@Override
		public double nextGaussian() {
			RandomSequencesState.this.markDirty();
			return this.random.nextGaussian();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			else {
				return o instanceof RandomSequencesState.WrappedRandom wrappedRandom
				       ? this.random.equals(wrappedRandom.random) : false;
			}
		}
	}
}
