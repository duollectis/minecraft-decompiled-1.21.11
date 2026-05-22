package net.minecraft.loot.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.List;

/**
 * Источник слотов, объединяющий несколько дочерних источников в единый поток предметов.
 * Поддерживает инлайн-кодек для сокращённой JSON-записи в виде массива.
 */
public class GroupSlotSource extends CombinedSlotSource {

	public static final MapCodec<GroupSlotSource> CODEC = createCodec(GroupSlotSource::new);
	public static final Codec<GroupSlotSource> INLINE_CODEC = createInlineCodec(GroupSlotSource::new);

	private GroupSlotSource(List<SlotSource> sources) {
		super(sources);
	}

	@Override
	public MapCodec<GroupSlotSource> getCodec() {
		return CODEC;
	}
}
