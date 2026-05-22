package net.minecraft.loot.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Функция лута, увеличивающая количество предметов в зависимости от уровня зачарования инструмента.
 * Поддерживает три формулы: биномиальная, рудная и равномерная.
 */
public class ApplyBonusLootFunction extends ConditionalLootFunction {

	private static final Map<Identifier, ApplyBonusLootFunction.Type> FACTORIES = Stream.of(
		ApplyBonusLootFunction.BinomialWithBonusCount.TYPE,
		ApplyBonusLootFunction.OreDrops.TYPE,
		ApplyBonusLootFunction.UniformBonusCount.TYPE
	).collect(Collectors.toMap(ApplyBonusLootFunction.Type::id, Function.identity()));

	private static final Codec<ApplyBonusLootFunction.Type> TYPE_CODEC = Identifier.CODEC.comapFlatMap(
		id -> {
			ApplyBonusLootFunction.Type type = FACTORIES.get(id);
			return type != null
				? DataResult.success(type)
				: DataResult.error(() -> "No formula type with id: '" + id + "'");
		},
		ApplyBonusLootFunction.Type::id
	);

	private static final MapCodec<ApplyBonusLootFunction.Formula> FORMULA_CODEC = Codecs.parameters(
		"formula",
		"parameters",
		TYPE_CODEC,
		ApplyBonusLootFunction.Formula::getType,
		ApplyBonusLootFunction.Type::codec
	);

	public static final MapCodec<ApplyBonusLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(instance.group(
				Enchantment.ENTRY_CODEC.fieldOf("enchantment").forGetter(function -> function.enchantment),
				FORMULA_CODEC.forGetter(function -> function.formula)
			))
			.apply(instance, ApplyBonusLootFunction::new)
	);

	private final RegistryEntry<Enchantment> enchantment;
	private final ApplyBonusLootFunction.Formula formula;

	private ApplyBonusLootFunction(
		List<LootCondition> conditions,
		RegistryEntry<Enchantment> enchantment,
		ApplyBonusLootFunction.Formula formula
	) {
		super(conditions);
		this.enchantment = enchantment;
		this.formula = formula;
	}

	@Override
	public LootFunctionType<ApplyBonusLootFunction> getType() {
		return LootFunctionTypes.APPLY_BONUS;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.TOOL);
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		ItemStack tool = context.get(LootContextParameters.TOOL);

		if (tool == null) {
			return stack;
		}

		int enchantLevel = EnchantmentHelper.getLevel(enchantment, tool);
		int newCount = formula.getValue(context.getRandom(), stack.getCount(), enchantLevel);
		stack.setCount(newCount);
		return stack;
	}

	public static ConditionalLootFunction.Builder<?> binomialWithBonusCount(
		RegistryEntry<Enchantment> enchantment,
		float probability,
		int extra
	) {
		return builder(conditions -> new ApplyBonusLootFunction(
			conditions,
			enchantment,
			new ApplyBonusLootFunction.BinomialWithBonusCount(extra, probability)
		));
	}

	public static ConditionalLootFunction.Builder<?> oreDrops(RegistryEntry<Enchantment> enchantment) {
		return builder(conditions -> new ApplyBonusLootFunction(
			conditions,
			enchantment,
			ApplyBonusLootFunction.OreDrops.INSTANCE
		));
	}

	public static ConditionalLootFunction.Builder<?> uniformBonusCount(RegistryEntry<Enchantment> enchantment) {
		return builder(conditions -> new ApplyBonusLootFunction(
			conditions,
			enchantment,
			new ApplyBonusLootFunction.UniformBonusCount(1)
		));
	}

	public static ConditionalLootFunction.Builder<?> uniformBonusCount(
		RegistryEntry<Enchantment> enchantment,
		int bonusMultiplier
	) {
		return builder(conditions -> new ApplyBonusLootFunction(
			conditions,
			enchantment,
			new ApplyBonusLootFunction.UniformBonusCount(bonusMultiplier)
		));
	}

	/** Биномиальная формула: каждый из (уровень + extra) испытаний даёт +1 с вероятностью probability. */
	record BinomialWithBonusCount(int extra, float probability) implements ApplyBonusLootFunction.Formula {

		private static final Codec<ApplyBonusLootFunction.BinomialWithBonusCount> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.INT.fieldOf("extra").forGetter(ApplyBonusLootFunction.BinomialWithBonusCount::extra),
				Codec.FLOAT.fieldOf("probability").forGetter(ApplyBonusLootFunction.BinomialWithBonusCount::probability)
			).apply(instance, ApplyBonusLootFunction.BinomialWithBonusCount::new)
		);

		public static final ApplyBonusLootFunction.Type TYPE =
			new ApplyBonusLootFunction.Type(Identifier.ofVanilla("binomial_with_bonus_count"), CODEC);

		@Override
		public int getValue(Random random, int initialCount, int enchantmentLevel) {
			for (int trial = 0; trial < enchantmentLevel + extra; trial++) {
				if (random.nextFloat() < probability) {
					initialCount++;
				}
			}

			return initialCount;
		}

		@Override
		public ApplyBonusLootFunction.Type getType() {
			return TYPE;
		}
	}

	/** Интерфейс формулы бонуса к количеству предметов. */
	interface Formula {

		int getValue(Random random, int initialCount, int enchantmentLevel);

		ApplyBonusLootFunction.Type getType();
	}

	/** Рудная формула: умножает количество на случайный бонус от уровня зачарования. */
	record OreDrops() implements ApplyBonusLootFunction.Formula {

		public static final ApplyBonusLootFunction.OreDrops INSTANCE = new ApplyBonusLootFunction.OreDrops();
		public static final Codec<ApplyBonusLootFunction.OreDrops> CODEC = MapCodec.unitCodec(INSTANCE);
		public static final ApplyBonusLootFunction.Type TYPE =
			new ApplyBonusLootFunction.Type(Identifier.ofVanilla("ore_drops"), CODEC);

		@Override
		public int getValue(Random random, int initialCount, int enchantmentLevel) {
			if (enchantmentLevel == 0) {
				return initialCount;
			}

			int bonus = random.nextInt(enchantmentLevel + 2) - 1;
			if (bonus < 0) {
				bonus = 0;
			}

			return initialCount * (bonus + 1);
		}

		@Override
		public ApplyBonusLootFunction.Type getType() {
			return TYPE;
		}
	}

	/** Тип формулы бонуса с идентификатором и кодеком. */
	record Type(Identifier id, Codec<? extends ApplyBonusLootFunction.Formula> codec) {
	}

	/** Равномерная формула: добавляет случайное количество в диапазоне [0, bonusMultiplier * уровень]. */
	record UniformBonusCount(int bonusMultiplier) implements ApplyBonusLootFunction.Formula {

		public static final Codec<ApplyBonusLootFunction.UniformBonusCount> CODEC = RecordCodecBuilder.create(
			instance -> instance
				.group(Codec.INT.fieldOf("bonusMultiplier").forGetter(ApplyBonusLootFunction.UniformBonusCount::bonusMultiplier))
				.apply(instance, ApplyBonusLootFunction.UniformBonusCount::new)
		);

		public static final ApplyBonusLootFunction.Type TYPE =
			new ApplyBonusLootFunction.Type(Identifier.ofVanilla("uniform_bonus_count"), CODEC);

		@Override
		public int getValue(Random random, int initialCount, int enchantmentLevel) {
			return initialCount + random.nextInt(bonusMultiplier * enchantmentLevel + 1);
		}

		@Override
		public ApplyBonusLootFunction.Type getType() {
			return TYPE;
		}
	}
}
