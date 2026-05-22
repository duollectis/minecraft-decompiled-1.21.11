package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.datafixer.DataFixTypes;

/**
 * Персистентное состояние, отслеживающее чанки, требующие обновления.
 * Хранит два множества: все затронутые чанки и ещё не обработанные.
 */
public class ChunkUpdateState extends PersistentState {

	private static final Codec<LongSet> LONG_SET_CODEC =
		Codec.LONG_STREAM.xmap(LongOpenHashSet::toSet, LongCollection::longStream);

	public static final Codec<ChunkUpdateState> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			LONG_SET_CODEC.fieldOf("All").forGetter(state -> state.all),
			LONG_SET_CODEC.fieldOf("Remaining").forGetter(state -> state.remaining)
		).apply(instance, ChunkUpdateState::new)
	);

	private final LongSet all;
	private final LongSet remaining;

	/**
	 * Создаёт тип персистентного состояния для регистрации в менеджере состояний.
	 *
	 * @param id идентификатор состояния на диске
	 * @return зарегистрированный тип состояния
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

	/** Добавляет позицию чанка в оба множества и помечает состояние изменённым. */
	public void add(long pos) {
		all.add(pos);
		remaining.add(pos);
		markDirty();
	}

	/** Проверяет, был ли чанк добавлен в это состояние. */
	public boolean contains(long pos) {
		return all.contains(pos);
	}

	/** Проверяет, ожидает ли чанк обработки. */
	public boolean isRemaining(long pos) {
		return remaining.contains(pos);
	}

	/** Помечает чанк как обработанный, удаляя его из множества ожидающих. */
	public void markResolved(long pos) {
		if (remaining.remove(pos)) {
			markDirty();
		}
	}

	public LongSet getAll() {
		return all;
	}
}
