package net.minecraft.loot.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

/**
 * {@code LimitSlotsSlotSource}.
 */
public class LimitSlotsSlotSource extends TransformSlotSource {

	public static final MapCodec<LimitSlotsSlotSource> CODEC = RecordCodecBuilder.mapCodec(
			instance -> addSlotSourceField(instance)
					.and(Codecs.POSITIVE_INT.fieldOf("limit").forGetter(source -> source.limit))
					.apply(instance, LimitSlotsSlotSource::new)
	);
	private final int limit;

	private LimitSlotsSlotSource(SlotSource slotSource, int limit) {
		super(slotSource);
		this.limit = limit;
	}

	@Override
	public MapCodec<LimitSlotsSlotSource> getCodec() {
		return CODEC;
	}

	@Override
	protected ItemStream transform(ItemStream stream) {
		return stream.limit(this.limit);
	}
}
