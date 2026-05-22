package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.datafixer.DataFixTypes;

/**
 * Персистентное состояние, хранящее счётчики идентификаторов для карт.
 * Обеспечивает уникальность ID карт в пределах мира.
 */
public class IdCountsState extends PersistentState {

	private static final int UNSET_ID = -1;

	public static final Codec<IdCountsState> CODEC = RecordCodecBuilder.create(
		instance -> instance
			.group(Codec.INT.optionalFieldOf("map", UNSET_ID).forGetter(state -> state.map))
			.apply(instance, IdCountsState::new)
	);

	public static final PersistentStateType<IdCountsState> STATE_TYPE = new PersistentStateType<>(
		"idcounts",
		IdCountsState::new,
		CODEC,
		DataFixTypes.SAVED_DATA_MAP_INDEX
	);

	private int map;

	public IdCountsState() {
		this(UNSET_ID);
	}

	public IdCountsState(int map) {
		this.map = map;
	}

	/**
	 * Создаёт следующий уникальный идентификатор карты и помечает состояние изменённым.
	 *
	 * @return новый компонент идентификатора карты
	 */
	public MapIdComponent createNextMapId() {
		MapIdComponent id = new MapIdComponent(++map);
		markDirty();
		return id;
	}
}
