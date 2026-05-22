package net.minecraft.loot.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.inventory.SlotRange;
import net.minecraft.inventory.SlotRanges;
import net.minecraft.inventory.StackReferenceGetter;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootEntityValueSource;
import net.minecraft.util.context.ContextParameter;

import java.util.Set;

/**
 * Источник слотов, извлекающий предметы из диапазона слотов сущности или блок-сущности
 * через {@link StackReferenceGetter}. Если объект контекста не реализует этот интерфейс,
 * возвращает пустой поток.
 */
public class SlotRangeSlotSource implements SlotSource {

	public static final MapCodec<SlotRangeSlotSource> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					LootEntityValueSource.ENTITY_OR_BLOCK_ENTITY_CODEC
							.fieldOf("source")
							.forGetter(source -> source.entitySource),
					SlotRanges.CODEC.fieldOf("slots").forGetter(source -> source.slotRange)
			).apply(instance, SlotRangeSlotSource::new)
	);
	private final LootEntityValueSource<Object> entitySource;
	private final SlotRange slotRange;

	private SlotRangeSlotSource(LootEntityValueSource<Object> entitySource, SlotRange slotRange) {
		this.entitySource = entitySource;
		this.slotRange = slotRange;
	}

	@Override
	public MapCodec<SlotRangeSlotSource> getCodec() {
		return CODEC;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(entitySource.contextParam());
	}

	@Override
	public final ItemStream stream(LootContext context) {
		return entitySource.get(context) instanceof StackReferenceGetter stackReferenceGetter
				? stackReferenceGetter.getStackReferences(slotRange.getSlotIds())
				: ItemStream.EMPTY;
	}
}
