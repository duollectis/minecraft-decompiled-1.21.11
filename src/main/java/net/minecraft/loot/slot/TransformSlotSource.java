package net.minecraft.loot.slot;

import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;

/**
 * Базовый класс для источников слотов, применяющих трансформацию к потоку предметов
 * дочернего источника. Подклассы реализуют {@link #transform(ItemStream)} для определения
 * конкретной операции (фильтрация, ограничение, маппинг содержимого контейнера).
 */
public abstract class TransformSlotSource implements SlotSource {

	protected final SlotSource slotSource;

	protected TransformSlotSource(SlotSource slotSource) {
		this.slotSource = slotSource;
	}

	@Override
	public abstract MapCodec<? extends TransformSlotSource> getCodec();

	protected static <T extends TransformSlotSource> P1<Mu<T>, SlotSource> addSlotSourceField(Instance<T> instance) {
		return instance.group(SlotSources.CODEC.fieldOf("slot_source").forGetter(source -> source.slotSource));
	}

	protected abstract ItemStream transform(ItemStream stream);

	@Override
	public final ItemStream stream(LootContext context) {
		return transform(slotSource.stream(context));
	}

	@Override
	public void validate(LootTableReporter reporter) {
		SlotSource.super.validate(reporter);
		slotSource.validate(reporter.makeChild(new ErrorReporter.MapElementContext("slot_source")));
	}
}
