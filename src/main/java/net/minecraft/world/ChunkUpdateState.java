package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.datafixer.DataFixTypes;

/**
 * {@code ChunkUpdateState}.
 */
public class ChunkUpdateState extends PersistentState {

	private final LongSet all;
	private final LongSet remaining;
	private static final Codec<LongSet>
			LONG_SET_CODEC =
			Codec.LONG_STREAM.xmap(LongOpenHashSet::toSet, LongCollection::longStream);
	public static final Codec<ChunkUpdateState> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    LONG_SET_CODEC.fieldOf("All").forGetter(state -> state.all),
					                    LONG_SET_CODEC.fieldOf("Remaining").forGetter(state -> state.remaining)
			                    )
			                    .apply(instance, ChunkUpdateState::new)
	);

	/**
	 * Создаёт state type.
	 *
	 * @param id id
	 *
	 * @return PersistentStateType — результат операции
	 */
	public static PersistentStateType<ChunkUpdateState> createStateType(String id) {
		return new PersistentStateType<>(
				id,
				ChunkUpdateState::new,
				CODEC,
				DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES
		);
	}

	private ChunkUpdateState(LongSet all, LongSet remaining) {
		this.all = all;
		this.remaining = remaining;
	}

	public ChunkUpdateState() {
		this(new LongOpenHashSet(), new LongOpenHashSet());
	}

	/**
	 * Add.
	 *
	 * @param pos pos
	 */
	public void add(long pos) {
		this.all.add(pos);
		this.remaining.add(pos);
		this.markDirty();
	}

	/**
	 * Contains.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean contains(long pos) {
		return this.all.contains(pos);
	}

	public boolean isRemaining(long pos) {
		return this.remaining.contains(pos);
	}

	/**
	 * Mark resolved.
	 *
	 * @param pos pos
	 */
	public void markResolved(long pos) {
		if (this.remaining.remove(pos)) {
			this.markDirty();
		}
	}

	public LongSet getAll() {
		return this.all;
	}
}
