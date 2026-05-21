package net.minecraft.loot.entry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.slot.SlotSource;
import net.minecraft.loot.slot.SlotSources;
import net.minecraft.util.ErrorReporter;

import java.util.List;
import java.util.function.Consumer;

/**
 * {@code SlotsEntry}.
 */
public class SlotsEntry extends LeafEntry {

	public static final MapCodec<SlotsEntry> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(SlotSources.CODEC.fieldOf("slot_source").forGetter(entry -> entry.slotSource))
			                    .and(addLeafFields(instance))
			                    .apply(instance, SlotsEntry::new)
	);
	private final SlotSource slotSource;

	private SlotsEntry(
			SlotSource slotSource,
			int weight,
			int quality,
			List<LootCondition> conditions,
			List<LootFunction> functions
	) {
		super(weight, quality, conditions, functions);
		this.slotSource = slotSource;
	}

	@Override
	public LootPoolEntryType getType() {
		return LootPoolEntryTypes.SLOTS;
	}

	@Override
	public void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
		this.slotSource.stream(context).itemCopies().filter(stack -> !stack.isEmpty()).forEach(lootConsumer);
	}

	@Override
	public void validate(LootTableReporter reporter) {
		super.validate(reporter);
		this.slotSource.validate(reporter.makeChild(new ErrorReporter.MapElementContext("slot_source")));
	}
}
