package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.datafixer.DataFixTypes;

/**
 * {@code IdCountsState}.
 */
public class IdCountsState extends PersistentState {

	private static final int UNSET_ID = -1;
	public static final Codec<IdCountsState> CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(Codec.INT.optionalFieldOf("map", -1).forGetter(state -> state.map))
					.apply(instance, IdCountsState::new)
	);
	public static final PersistentStateType<IdCountsState> STATE_TYPE = new PersistentStateType<>(
			"idcounts", IdCountsState::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX
	);
	private int map;

	public IdCountsState() {
		this(-1);
	}

	public IdCountsState(int map) {
		this.map = map;
	}

	public MapIdComponent createNextMapId() {
		MapIdComponent mapIdComponent = new MapIdComponent(++this.map);
		this.markDirty();
		return mapIdComponent;
	}
}
