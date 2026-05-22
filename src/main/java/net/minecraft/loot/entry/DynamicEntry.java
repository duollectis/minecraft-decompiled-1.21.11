package net.minecraft.loot.entry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

/** Запись пула лута, делегирующая генерацию предметов динамическому дропу по идентификатору. */
public class DynamicEntry extends LeafEntry {

	public static final MapCodec<DynamicEntry> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(Identifier.CODEC.fieldOf("name").forGetter(entry -> entry.name))
			.and(addLeafFields(instance))
			.apply(instance, DynamicEntry::new)
	);

	private final Identifier name;

	private DynamicEntry(
		Identifier name,
		int weight,
		int quality,
		List<LootCondition> conditions,
		List<LootFunction> functions
	) {
		super(weight, quality, conditions, functions);
		this.name = name;
	}

	@Override
	public LootPoolEntryType getType() {
		return LootPoolEntryTypes.DYNAMIC;
	}

	@Override
	public void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
		context.drop(name, lootConsumer);
	}

	public static LeafEntry.Builder<?> builder(Identifier name) {
		return builder((weight, quality, conditions, functions) -> new DynamicEntry(
			name,
			weight,
			quality,
			conditions,
			functions
		));
	}
}
