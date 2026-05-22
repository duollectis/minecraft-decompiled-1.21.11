package net.minecraft.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.loot.v3.FabricLootTableBuilder;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionConsumingBuilder;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class LootTable {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final Codec<RegistryKey<LootTable>> TABLE_KEY = RegistryKey.createCodec(RegistryKeys.LOOT_TABLE);
	public static final ContextType GENERIC = LootContextTypes.GENERIC;
	public static final long DEFAULT_SEED = 0L;

	public static final Codec<LootTable> CODEC = Codec.lazyInitialized(
			() -> RecordCodecBuilder.create(
					instance -> instance.group(
							LootContextTypes.CODEC
									.lenientOptionalFieldOf("type", GENERIC)
									.forGetter(table -> table.type),
							Identifier.CODEC
									.optionalFieldOf("random_sequence")
									.forGetter(table -> table.randomSequenceId),
							LootPool.CODEC
									.listOf()
									.optionalFieldOf("pools", List.of())
									.forGetter(table -> table.pools),
							LootFunctionTypes.CODEC
									.listOf()
									.optionalFieldOf("functions", List.of())
									.forGetter(table -> table.functions)
					).apply(instance, LootTable::new)
			)
	);

	public static final Codec<RegistryEntry<LootTable>> ENTRY_CODEC =
			RegistryElementCodec.of(RegistryKeys.LOOT_TABLE, CODEC);

	public static final LootTable EMPTY =
			new LootTable(LootContextTypes.EMPTY, Optional.empty(), List.of(), List.of());

	private final ContextType type;
	private final Optional<Identifier> randomSequenceId;
	private final List<LootPool> pools;
	private final List<LootFunction> functions;
	private final BiFunction<ItemStack, LootContext, ItemStack> combinedFunction;

	LootTable(
			ContextType type,
			Optional<Identifier> randomSequenceId,
			List<LootPool> pools,
			List<LootFunction> functions
	) {
		this.type = type;
		this.randomSequenceId = randomSequenceId;
		this.pools = pools;
		this.functions = functions;
		combinedFunction = LootFunctionTypes.join(functions);
	}

	/**
	 * Оборачивает consumer так, чтобы стаки автоматически разбивались на части,
	 * если их количество превышает максимальный размер стака предмета.
	 * Также фильтрует предметы, отключённые флагами фич.
	 *
	 * @param world мир, из которого берутся активные флаги фич
	 * @param consumer целевой consumer для обработанных стаков
	 * @return обёрнутый consumer
	 */
	public static Consumer<ItemStack> processStacks(ServerWorld world, Consumer<ItemStack> consumer) {
		return stack -> {
			if (!stack.isItemEnabled(world.getEnabledFeatures())) {
				return;
			}

			if (stack.getCount() < stack.getMaxCount()) {
				consumer.accept(stack);
				return;
			}

			int remaining = stack.getCount();

			while (remaining > 0) {
				ItemStack chunk = stack.copyWithCount(Math.min(stack.getMaxCount(), remaining));
				remaining -= chunk.getCount();
				consumer.accept(chunk);
			}
		};
	}

	public void generateUnprocessedLoot(LootWorldContext parameters, Consumer<ItemStack> lootConsumer) {
		generateUnprocessedLoot(new LootContext.Builder(parameters).build(randomSequenceId), lootConsumer);
	}

	public void generateUnprocessedLoot(LootContext context, Consumer<ItemStack> lootConsumer) {
		LootContext.Entry<?> entry = LootContext.table(this);

		if (!context.markActive(entry)) {
			LOGGER.warn("Detected infinite loop in loot tables");
			return;
		}

		Consumer<ItemStack> consumer = LootFunction.apply(combinedFunction, lootConsumer, context);

		for (LootPool pool : pools) {
			pool.addGeneratedLoot(consumer, context);
		}

		context.markInactive(entry);
	}

	public void generateLoot(LootWorldContext parameters, long seed, Consumer<ItemStack> lootConsumer) {
		generateUnprocessedLoot(
				new LootContext.Builder(parameters).random(seed).build(randomSequenceId),
				processStacks(parameters.getWorld(), lootConsumer)
		);
	}

	public void generateLoot(LootWorldContext parameters, Consumer<ItemStack> lootConsumer) {
		generateUnprocessedLoot(parameters, processStacks(parameters.getWorld(), lootConsumer));
	}

	public void generateLoot(LootContext context, Consumer<ItemStack> lootConsumer) {
		generateUnprocessedLoot(context, processStacks(context.getWorld(), lootConsumer));
	}

	public ObjectArrayList<ItemStack> generateLoot(LootWorldContext parameters, Random random) {
		return generateLoot(new LootContext.Builder(parameters).random(random).build(randomSequenceId));
	}

	public ObjectArrayList<ItemStack> generateLoot(LootWorldContext parameters, long seed) {
		return generateLoot(new LootContext.Builder(parameters).random(seed).build(randomSequenceId));
	}

	public ObjectArrayList<ItemStack> generateLoot(LootWorldContext parameters) {
		return generateLoot(new LootContext.Builder(parameters).build(randomSequenceId));
	}

	private ObjectArrayList<ItemStack> generateLoot(LootContext context) {
		ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
		generateLoot(context, result::add);

		return result;
	}

	public ContextType getType() {
		return type;
	}

	public void validate(LootTableReporter reporter) {
		for (int index = 0; index < pools.size(); index++) {
			pools.get(index).validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("pools", index)));
		}

		for (int index = 0; index < functions.size(); index++) {
			functions.get(index)
					.validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("functions", index)));
		}
	}

	/**
	 * Заполняет инвентарь предметами из лут-таблицы, равномерно распределяя их по свободным слотам.
	 * Стаки с количеством больше 1 могут быть случайно разбиты на части для более реалистичного распределения.
	 *
	 * @param inventory инвентарь для заполнения
	 * @param parameters параметры мирового контекста лута
	 * @param seed сид для воспроизводимой генерации
	 */
	public void supplyInventory(Inventory inventory, LootWorldContext parameters, long seed) {
		LootContext lootContext = new LootContext.Builder(parameters).random(seed).build(randomSequenceId);
		ObjectArrayList<ItemStack> generatedStacks = generateLoot(lootContext);
		Random random = lootContext.getRandom();
		List<Integer> freeSlots = getFreeSlots(inventory, random);

		spreadStacks(generatedStacks, freeSlots.size(), random);

		for (ItemStack stack : generatedStacks) {
			if (freeSlots.isEmpty()) {
				LOGGER.warn("Tried to over-fill a container");
				return;
			}

			int targetSlot = freeSlots.remove(freeSlots.size() - 1);
			inventory.setStack(targetSlot, stack.isEmpty() ? ItemStack.EMPTY : stack);
		}
	}

	/**
	 * Случайно разбивает стаки с количеством > 1 на части, чтобы заполнить больше слотов.
	 * Алгоритм продолжает разбивать, пока есть свободные слоты и стаки для разбивки.
	 */
	private void spreadStacks(ObjectArrayList<ItemStack> stacks, int freeSlots, Random random) {
		List<ItemStack> splittable = Lists.newArrayList();
		Iterator<ItemStack> iterator = stacks.iterator();

		while (iterator.hasNext()) {
			ItemStack stack = iterator.next();

			if (stack.isEmpty()) {
				iterator.remove();
			} else if (stack.getCount() > 1) {
				splittable.add(stack);
				iterator.remove();
			}
		}

		while (freeSlots - stacks.size() - splittable.size() > 0 && !splittable.isEmpty()) {
			ItemStack source = splittable.remove(MathHelper.nextInt(random, 0, splittable.size() - 1));
			int splitCount = MathHelper.nextInt(random, 1, source.getCount() / 2);
			ItemStack split = source.split(splitCount);

			if (source.getCount() > 1 && random.nextBoolean()) {
				splittable.add(source);
			} else {
				stacks.add(source);
			}

			if (split.getCount() > 1 && random.nextBoolean()) {
				splittable.add(split);
			} else {
				stacks.add(split);
			}
		}

		stacks.addAll(splittable);
		Util.shuffle(stacks, random);
	}

	private List<Integer> getFreeSlots(Inventory inventory, Random random) {
		ObjectArrayList<Integer> freeSlots = new ObjectArrayList<>();

		for (int slot = 0; slot < inventory.size(); slot++) {
			if (inventory.getStack(slot).isEmpty()) {
				freeSlots.add(slot);
			}
		}

		Util.shuffle(freeSlots, random);

		return freeSlots;
	}

	public static LootTable.Builder builder() {
		return new LootTable.Builder();
	}

	public static class Builder implements LootFunctionConsumingBuilder<LootTable.Builder>, FabricLootTableBuilder {

		private final ImmutableList.Builder<LootPool> pools = ImmutableList.builder();
		private final ImmutableList.Builder<LootFunction> functions = ImmutableList.builder();
		private ContextType type = LootTable.GENERIC;
		private Optional<Identifier> randomSequenceId = Optional.empty();

		public LootTable.Builder pool(LootPool.Builder poolBuilder) {
			pools.add(poolBuilder.build());
			return this;
		}

		public LootTable.Builder type(ContextType type) {
			this.type = type;
			return this;
		}

		public LootTable.Builder randomSequenceId(Identifier randomSequenceId) {
			this.randomSequenceId = Optional.of(randomSequenceId);
			return this;
		}

		public LootTable.Builder apply(LootFunction.Builder builder) {
			functions.add(builder.build());
			return this;
		}

		public LootTable.Builder getThisFunctionConsumingBuilder() {
			return this;
		}

		public LootTable build() {
			return new LootTable(type, randomSequenceId, pools.build(), functions.build());
		}
	}
}
