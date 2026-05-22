package net.minecraft.data.loottable;

import net.fabricmc.fabric.api.datagen.v1.loot.FabricBlockLootTableGenerator;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.*;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.*;
import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import net.minecraft.loot.provider.number.BinomialLootNumberProvider;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.predicate.component.ComponentPredicateTypes;
import net.minecraft.predicate.component.ComponentsPredicate;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.EnchantmentsPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Базовый абстрактный генератор таблиц лута для блоков.
 * Предоставляет богатый набор фабричных методов для создания типовых таблиц лута:
 * выпадение с шёлковым касанием, ножницами, взрывоустойчивость, руды, листья, посевы и т.д.
 * Подклассы реализуют {@link #generate()} и регистрируют таблицы через {@link #addDrop}.
 */
public abstract class BlockLootTableGenerator implements LootTableGenerator, FabricBlockLootTableGenerator {

	protected final RegistryWrapper.WrapperLookup registries;
	protected final Set<Item> explosionImmuneItems;
	protected final FeatureSet requiredFeatures;
	protected final Map<RegistryKey<LootTable>, LootTable.Builder> lootTables;
	protected static final float[] SAPLING_DROP_CHANCE = new float[]{0.05F, 0.0625F, 0.083333336F, 0.1F};
	private static final float[]
			LEAVES_STICK_DROP_CHANCE =
			new float[]{0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

	public LootCondition.Builder createSilkTouchCondition() {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return MatchToolLootCondition.builder(
			ItemPredicate.Builder.create()
				.components(
					ComponentsPredicate.Builder.create()
						.partial(
							ComponentPredicateTypes.ENCHANTMENTS,
							EnchantmentsPredicate.enchantments(
								List.of(new EnchantmentPredicate(
									enchantmentLookup.getOrThrow(Enchantments.SILK_TOUCH),
									NumberRange.IntRange.atLeast(1)
								))
							)
						)
						.build()
				)
		);
	}

	public LootCondition.Builder createWithoutSilkTouchCondition() {
		return createSilkTouchCondition().invert();
	}

	public LootCondition.Builder createWithShearsCondition() {
		return MatchToolLootCondition.builder(
			ItemPredicate.Builder.create().items(registries.getOrThrow(RegistryKeys.ITEM), Items.SHEARS)
		);
	}

	public final LootCondition.Builder createWithSilkTouchOrShearsCondition() {
		return createWithShearsCondition().or(createSilkTouchCondition());
	}

	public final LootCondition.Builder createWithoutShearsOrSilkTouchCondition() {
		return createWithSilkTouchOrShearsCondition().invert();
	}

	protected BlockLootTableGenerator(
			Set<Item> explosionImmuneItems,
			FeatureSet requiredFeatures,
			RegistryWrapper.WrapperLookup registries
	) {
		this(explosionImmuneItems, requiredFeatures, new HashMap<>(), registries);
	}

	protected BlockLootTableGenerator(
			Set<Item> explosionImmuneItems,
			FeatureSet requiredFeatures,
			Map<RegistryKey<LootTable>, LootTable.Builder> lootTables,
			RegistryWrapper.WrapperLookup registries
	) {
		this.explosionImmuneItems = explosionImmuneItems;
		this.requiredFeatures = requiredFeatures;
		this.lootTables = lootTables;
		this.registries = registries;
	}

	public <T extends LootFunctionConsumingBuilder<T>> T applyExplosionDecay(
		ItemConvertible drop,
		LootFunctionConsumingBuilder<T> builder
	) {
		return explosionImmuneItems.contains(drop.asItem())
			? builder.getThisFunctionConsumingBuilder()
			: builder.apply(ExplosionDecayLootFunction.builder());
	}

	public <T extends LootConditionConsumingBuilder<T>> T addSurvivesExplosionCondition(
		ItemConvertible drop,
		LootConditionConsumingBuilder<T> builder
	) {
		return explosionImmuneItems.contains(drop.asItem())
			? builder.getThisConditionConsumingBuilder()
			: builder.conditionally(SurvivesExplosionLootCondition.builder());
	}

	public LootTable.Builder drops(ItemConvertible drop) {
		return LootTable.builder()
		                .pool(this.addSurvivesExplosionCondition(
				                drop,
				                LootPool
						                .builder()
						                .rolls(ConstantLootNumberProvider.create(1.0F))
						                .with(ItemEntry.builder(drop))
		                ));
	}

	public static LootTable.Builder drops(
			Block drop,
			LootCondition.Builder conditionBuilder,
			LootPoolEntry.Builder<?> child
	) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .with(ItemEntry
						                        .builder(drop)
						                        .conditionally(conditionBuilder)
						                        .alternatively(child))
		                );
	}

	public LootTable.Builder dropsWithSilkTouch(Block block, LootPoolEntry.Builder<?> loot) {
		return drops(block, this.createSilkTouchCondition(), loot);
	}

	public LootTable.Builder dropsWithShears(Block block, LootPoolEntry.Builder<?> loot) {
		return drops(block, this.createWithShearsCondition(), loot);
	}

	public LootTable.Builder dropsWithSilkTouchOrShears(Block block, LootPoolEntry.Builder<?> loot) {
		return drops(block, this.createWithSilkTouchOrShearsCondition(), loot);
	}

	public LootTable.Builder drops(Block withSilkTouch, ItemConvertible withoutSilkTouch) {
		return dropsWithSilkTouch(
			withSilkTouch,
			addSurvivesExplosionCondition(withSilkTouch, ItemEntry.builder(withoutSilkTouch))
		);
	}

	public LootTable.Builder drops(ItemConvertible drop, LootNumberProvider count) {
		return LootTable.builder()
			.pool(
				LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1.0F))
					.with(applyExplosionDecay(
						drop,
						ItemEntry.builder(drop).apply(SetCountLootFunction.builder(count))
					))
			);
	}

	public LootTable.Builder drops(Block block, ItemConvertible drop, LootNumberProvider count) {
		return dropsWithSilkTouch(
			block,
			applyExplosionDecay(block, ItemEntry.builder(drop).apply(SetCountLootFunction.builder(count)))
		);
	}

	public final LootTable.Builder dropsWithSilkTouch(ItemConvertible drop) {
		return LootTable.builder()
		                .pool(LootPool
				                .builder()
				                .conditionally(this.createSilkTouchCondition())
				                .rolls(ConstantLootNumberProvider.create(1.0F))
				                .with(ItemEntry.builder(drop)));
	}

	public final LootTable.Builder pottedPlantDrops(ItemConvertible drop) {
		return LootTable.builder()
		                .pool(
				                this.addSurvivesExplosionCondition(
						                Blocks.FLOWER_POT,
						                LootPool
								                .builder()
								                .rolls(ConstantLootNumberProvider.create(1.0F))
								                .with(ItemEntry.builder(Blocks.FLOWER_POT))
				                )
		                )
		                .pool(this.addSurvivesExplosionCondition(
				                drop,
				                LootPool
						                .builder()
						                .rolls(ConstantLootNumberProvider.create(1.0F))
						                .with(ItemEntry.builder(drop))
		                ));
	}

	public LootTable.Builder slabDrops(Block drop) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .with(
						                        (LootPoolEntry.Builder<?>) this.applyExplosionDecay(
								                        drop,
								                        ItemEntry.builder(drop)
								                                 .apply(
										                                 SetCountLootFunction
												                                 .builder(ConstantLootNumberProvider.create(
														                                 2.0F))
												                                 .conditionally(
														                                 BlockStatePropertyLootCondition
																                                 .builder(drop)
																                                 .properties(
																		                                 StatePredicate.Builder
																				                                 .create()
																				                                 .exactMatch(
																						                                 SlabBlock.TYPE,
																						                                 SlabType.DOUBLE
																				                                 ))
												                                 )
								                                 )
						                        )
				                        )
		                );
	}

	public <T extends Comparable<T> & StringIdentifiable> LootTable.Builder dropsWithProperty(
			Block drop,
			Property<T> property,
			T value
	) {
		return LootTable.builder()
		                .pool(
				                this.addSurvivesExplosionCondition(
						                drop,
						                LootPool.builder()
						                        .rolls(ConstantLootNumberProvider.create(1.0F))
						                        .with(
								                        ItemEntry.builder(drop)
								                                 .conditionally(BlockStatePropertyLootCondition
										                                 .builder(drop)
										                                 .properties(StatePredicate.Builder
												                                 .create()
												                                 .exactMatch(property, value)))
						                        )
				                )
		                );
	}

	public LootTable.Builder nameableContainerDrops(Block drop) {
		return LootTable.builder()
		                .pool(
				                this.addSurvivesExplosionCondition(
						                drop,
						                LootPool.builder()
						                        .rolls(ConstantLootNumberProvider.create(1.0F))
						                        .with(
								                        ItemEntry.builder(drop)
								                                 .apply(CopyComponentsLootFunction
										                                 .blockEntity(LootContextParameters.BLOCK_ENTITY)
										                                 .include(DataComponentTypes.CUSTOM_NAME))
						                        )
				                )
		                );
	}

	public LootTable.Builder shulkerBoxDrops(Block drop) {
		return LootTable.builder()
		                .pool(
				                this.addSurvivesExplosionCondition(
						                drop,
						                LootPool.builder()
						                        .rolls(ConstantLootNumberProvider.create(1.0F))
						                        .with(
								                        ItemEntry.builder(drop)
								                                 .apply(
										                                 CopyComponentsLootFunction
												                                 .blockEntity(LootContextParameters.BLOCK_ENTITY)
												                                 .include(DataComponentTypes.CUSTOM_NAME)
												                                 .include(DataComponentTypes.CONTAINER)
												                                 .include(DataComponentTypes.LOCK)
												                                 .include(DataComponentTypes.CONTAINER_LOOT)
								                                 )
						                        )
				                )
		                );
	}

	public LootTable.Builder copperOreDrops(Block drop) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return dropsWithSilkTouch(
			drop,
			applyExplosionDecay(
				drop,
				ItemEntry.builder(Items.RAW_COPPER)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(2.0F, 5.0F)))
					.apply(ApplyBonusLootFunction.oreDrops(enchantmentLookup.getOrThrow(Enchantments.FORTUNE)))
			)
		);
	}

	public LootTable.Builder lapisOreDrops(Block drop) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return dropsWithSilkTouch(
			drop,
			applyExplosionDecay(
				drop,
				ItemEntry.builder(Items.LAPIS_LAZULI)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(4.0F, 9.0F)))
					.apply(ApplyBonusLootFunction.oreDrops(enchantmentLookup.getOrThrow(Enchantments.FORTUNE)))
			)
		);
	}

	public LootTable.Builder redstoneOreDrops(Block drop) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return dropsWithSilkTouch(
			drop,
			applyExplosionDecay(
				drop,
				ItemEntry.builder(Items.REDSTONE)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(4.0F, 5.0F)))
					.apply(ApplyBonusLootFunction.uniformBonusCount(enchantmentLookup.getOrThrow(Enchantments.FORTUNE)))
			)
		);
	}

	public LootTable.Builder bannerDrops(Block drop) {
		return LootTable.builder()
		                .pool(
				                this.addSurvivesExplosionCondition(
						                drop,
						                LootPool.builder()
						                        .rolls(ConstantLootNumberProvider.create(1.0F))
						                        .with(
								                        ItemEntry.builder(drop)
								                                 .apply(
										                                 CopyComponentsLootFunction
												                                 .blockEntity(LootContextParameters.BLOCK_ENTITY)
												                                 .include(DataComponentTypes.CUSTOM_NAME)
												                                 .include(DataComponentTypes.ITEM_NAME)
												                                 .include(DataComponentTypes.TOOLTIP_DISPLAY)
												                                 .include(DataComponentTypes.BANNER_PATTERNS)
												                                 .include(DataComponentTypes.RARITY)
								                                 )
						                        )
				                )
		                );
	}

	public LootTable.Builder beeNestDrops(Block drop) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .conditionally(this.createSilkTouchCondition())
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .with(
						                        ItemEntry.builder(drop)
						                                 .apply(CopyComponentsLootFunction
								                                 .blockEntity(LootContextParameters.BLOCK_ENTITY)
								                                 .include(DataComponentTypes.BEES))
						                                 .apply(CopyStateLootFunction
								                                 .builder(drop)
								                                 .addProperty(BeehiveBlock.HONEY_LEVEL))
				                        )
		                );
	}

	public LootTable.Builder beehiveDrops(Block drop) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .with(
						                        ItemEntry.builder(drop)
						                                 .conditionally(this.createSilkTouchCondition())
						                                 .apply(CopyComponentsLootFunction
								                                 .blockEntity(LootContextParameters.BLOCK_ENTITY)
								                                 .include(DataComponentTypes.BEES))
						                                 .apply(CopyStateLootFunction
								                                 .builder(drop)
								                                 .addProperty(BeehiveBlock.HONEY_LEVEL))
						                                 .alternatively(ItemEntry.builder(drop))
				                        )
		                );
	}

	public LootTable.Builder glowBerryDrops(Block drop) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .with(ItemEntry.builder(Items.GLOW_BERRIES))
				                        .conditionally(BlockStatePropertyLootCondition
						                        .builder(drop)
						                        .properties(StatePredicate.Builder
								                        .create()
								                        .exactMatch(CaveVines.BERRIES, true)))
		                );
	}

	public LootTable.Builder copperGolemStatueDrops(Block drop) {
		return LootTable.builder()
		                .pool(
				                this.addSurvivesExplosionCondition(
						                drop,
						                LootPool.builder()
						                        .rolls(ConstantLootNumberProvider.create(1.0F))
						                        .with(
								                        ItemEntry.builder(drop)
								                                 .apply(CopyComponentsLootFunction
										                                 .blockEntity(LootContextParameters.BLOCK_ENTITY)
										                                 .include(DataComponentTypes.CUSTOM_NAME))
								                                 .apply(CopyStateLootFunction
										                                 .builder(drop)
										                                 .addProperty(CopperGolemStatueBlock.POSE))
						                        )
				                )
		                );
	}

	public LootTable.Builder oreDrops(Block withSilkTouch, Item withoutSilkTouch) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return dropsWithSilkTouch(
			withSilkTouch,
			applyExplosionDecay(
				withSilkTouch,
				ItemEntry.builder(withoutSilkTouch)
					.apply(ApplyBonusLootFunction.oreDrops(enchantmentLookup.getOrThrow(Enchantments.FORTUNE)))
			)
		);
	}

	public LootTable.Builder mushroomBlockDrops(Block withSilkTouch, ItemConvertible withoutSilkTouch) {
		return dropsWithSilkTouch(
			withSilkTouch,
			applyExplosionDecay(
				withSilkTouch,
				ItemEntry.builder(withoutSilkTouch)
					.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(-6.0F, 2.0F)))
					.apply(LimitCountLootFunction.builder(BoundedIntUnaryOperator.createMin(0)))
			)
		);
	}

	public LootTable.Builder shortPlantDrops(Block withShears) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return dropsWithShears(
			withShears,
			applyExplosionDecay(
				withShears,
				ItemEntry.builder(Items.WHEAT_SEEDS)
					.conditionally(RandomChanceLootCondition.builder(0.125F))
					.apply(ApplyBonusLootFunction.uniformBonusCount(enchantmentLookup.getOrThrow(Enchantments.FORTUNE), 2))
			)
		);
	}

	public LootTable.Builder cropStemDrops(Block stem, Item drop) {
		return LootTable.builder()
		                .pool(
				                this.applyExplosionDecay(
						                stem,
						                LootPool.builder()
						                        .rolls(ConstantLootNumberProvider.create(1.0F))
						                        .with(
								                        ItemEntry.builder(drop)
								                                 .apply(
										                                 StemBlock.AGE.getValues(),
										                                 age -> SetCountLootFunction
												                                 .builder(BinomialLootNumberProvider.create(
														                                 3,
														                                 (age + 1) / 15.0F
												                                 ))
												                                 .conditionally(
														                                 BlockStatePropertyLootCondition
																                                 .builder(stem)
																                                 .properties(
																		                                 StatePredicate.Builder
																				                                 .create()
																				                                 .exactMatch(
																						                                 StemBlock.AGE,
																						                                 age.intValue()
																				                                 ))
												                                 )
								                                 )
						                        )
				                )
		                );
	}

	public LootTable.Builder attachedCropStemDrops(Block stem, Item drop) {
		return LootTable.builder()
		                .pool(
				                this.applyExplosionDecay(
						                stem,
						                LootPool.builder()
						                        .rolls(ConstantLootNumberProvider.create(1.0F))
						                        .with(ItemEntry
								                        .builder(drop)
								                        .apply(SetCountLootFunction.builder(BinomialLootNumberProvider.create(
										                        3,
										                        0.53333336F
								                        ))))
				                )
		                );
	}

	public LootTable.Builder dropsWithShears(ItemConvertible item) {
		return LootTable.builder()
		                .pool(LootPool
				                .builder()
				                .rolls(ConstantLootNumberProvider.create(1.0F))
				                .conditionally(this.createWithShearsCondition())
				                .with(ItemEntry.builder(item)));
	}

	public LootTable.Builder dropsWithSilkTouchOrShears(ItemConvertible item) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .conditionally(this.createWithSilkTouchOrShearsCondition())
				                        .with(ItemEntry.builder(item))
		                );
	}

	public LootTable.Builder multifaceGrowthDrops(Block drop, LootCondition.Builder condition) {
		return LootTable.builder()
			.pool(
				LootPool.builder()
					.with(
						applyExplosionDecay(
							drop,
							ItemEntry.builder(drop)
								.conditionally(condition)
								.apply(
									Direction.values(),
									direction -> SetCountLootFunction
										.builder(ConstantLootNumberProvider.create(1.0F), true)
										.conditionally(
											BlockStatePropertyLootCondition.builder(drop)
												.properties(StatePredicate.Builder.create()
													.exactMatch(MultifaceBlock.getProperty(direction), true))
										)
								)
								.apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(-1.0F), true))
						)
					)
			);
	}

	public LootTable.Builder multifaceGrowthDrops(Block drop) {
		return LootTable.builder()
			.pool(
				LootPool.builder()
					.with(
						applyExplosionDecay(
							drop,
							ItemEntry.builder(drop)
								.apply(
									Direction.values(),
									direction -> SetCountLootFunction
										.builder(ConstantLootNumberProvider.create(1.0F), true)
										.conditionally(
											BlockStatePropertyLootCondition.builder(drop)
												.properties(StatePredicate.Builder.create()
													.exactMatch(MultifaceBlock.getProperty(direction), true))
										)
								)
								.apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(-1.0F), true))
						)
					)
			);
	}

	public LootTable.Builder paleMossCarpetDrops(Block block) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .with(
						                        (LootPoolEntry.Builder<?>) this.applyExplosionDecay(
								                        block,
								                        ItemEntry.builder(block)
								                                 .conditionally(
										                                 BlockStatePropertyLootCondition.builder(block)
										                                                                .properties(
												                                                                StatePredicate.Builder
														                                                                .create()
														                                                                .exactMatch(
																                                                                PaleMossCarpetBlock.BOTTOM,
																                                                                true
														                                                                ))
								                                 )
						                        )
				                        )
		                );
	}

	public LootTable.Builder leavesDrops(Block leaves, Block sapling, float... saplingChance) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return dropsWithSilkTouchOrShears(
				leaves,
				addSurvivesExplosionCondition(leaves, ItemEntry.builder(sapling))
						.conditionally(TableBonusLootCondition.builder(
								enchantmentLookup.getOrThrow(Enchantments.FORTUNE),
								saplingChance
						))
		)
		.pool(
				LootPool.builder()
				        .rolls(ConstantLootNumberProvider.create(1.0F))
				        .conditionally(createWithoutShearsOrSilkTouchCondition())
				        .with(
						        applyExplosionDecay(
								        leaves,
								        ItemEntry.builder(Items.STICK)
								                 .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										                 1.0F,
										                 2.0F
								                 )))
						        )
						        .conditionally(TableBonusLootCondition.builder(
								        enchantmentLookup.getOrThrow(Enchantments.FORTUNE),
								        LEAVES_STICK_DROP_CHANCE
						        ))
				        )
		);
	}

	public LootTable.Builder oakLeavesDrops(Block leaves, Block sapling, float... saplingChance) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return leavesDrops(leaves, sapling, saplingChance)
				.pool(
						LootPool.builder()
						        .rolls(ConstantLootNumberProvider.create(1.0F))
						        .conditionally(createWithoutShearsOrSilkTouchCondition())
						        .with(
								        addSurvivesExplosionCondition(leaves, ItemEntry.builder(Items.APPLE))
										        .conditionally(
												        TableBonusLootCondition.builder(
														        enchantmentLookup.getOrThrow(Enchantments.FORTUNE),
														        0.005F,
														        0.0055555557F,
														        0.00625F,
														        0.008333334F,
														        0.025F
												        )
										        )
						        )
				);
	}

	public LootTable.Builder mangroveLeavesDrops(Block leaves) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return dropsWithSilkTouchOrShears(
				leaves,
				applyExplosionDecay(
						Blocks.MANGROVE_LEAVES,
						ItemEntry.builder(Items.STICK)
						         .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0F, 2.0F)))
				)
				.conditionally(TableBonusLootCondition.builder(
						enchantmentLookup.getOrThrow(Enchantments.FORTUNE),
						LEAVES_STICK_DROP_CHANCE
				))
		);
	}

	public LootTable.Builder cropDrops(Block crop, Item product, Item seeds, LootCondition.Builder condition) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return applyExplosionDecay(
				crop,
				LootTable.builder()
				         .pool(
						         LootPool.builder()
						                 .with(
								                 ItemEntry.builder(product)
								                          .conditionally(condition)
								                          .alternatively(ItemEntry.builder(seeds))
						                 )
				         )
				         .pool(
						         LootPool.builder()
						                 .conditionally(condition)
						                 .with(
								                 ItemEntry.builder(seeds)
								                          .apply(ApplyBonusLootFunction.binomialWithBonusCount(
										                          enchantmentLookup.getOrThrow(Enchantments.FORTUNE),
										                          0.5714286F,
										                          3
								                          ))
						                 )
				         )
		);
	}

	public LootTable.Builder seagrassDrops(Block seagrass) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .conditionally(this.createWithShearsCondition())
				                        .with(ItemEntry
						                        .builder(seagrass)
						                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(
								                        2.0F))))
		                );
	}

	public LootTable.Builder tallPlantDrops(Block tallPlant, Block shortPlant) {
		RegistryWrapper.Impl<Block> blockLookup = registries.getOrThrow(RegistryKeys.BLOCK);
		LootPoolEntry.Builder<?> entryBuilder = ItemEntry.builder(shortPlant)
		                                                 .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2.0F)))
		                                                 .conditionally(createWithShearsCondition())
		                                                 .alternatively(
				                                                 addSurvivesExplosionCondition(
						                                                 tallPlant,
						                                                 ItemEntry.builder(Items.WHEAT_SEEDS)
				                                                 )
				                                                 .conditionally(RandomChanceLootCondition.builder(0.125F))
		                                                 );
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .with(entryBuilder)
				                        .conditionally(
						                        BlockStatePropertyLootCondition.builder(tallPlant)
						                                                       .properties(
								                                                       StatePredicate.Builder.create()
								                                                                            .exactMatch(
										                                                                            TallPlantBlock.HALF,
										                                                                            DoubleBlockHalf.LOWER
								                                                                            )
						                                                       )
				                        )
				                        .conditionally(
						                        LocationCheckLootCondition.builder(
								                        LocationPredicate.Builder.create()
								                                                 .block(
										                                                 BlockPredicate.Builder.create()
										                                                                       .blocks(blockLookup, tallPlant)
										                                                                       .state(
												                                                                       StatePredicate.Builder.create()
												                                                                                            .exactMatch(
														                                                                                            TallPlantBlock.HALF,
														                                                                                            DoubleBlockHalf.UPPER
												                                                                                            )
										                                                                       )
								                                                 ),
								                        new BlockPos(0, 1, 0)
						                        )
				                        )
		                )
		                .pool(
				                LootPool.builder()
				                        .with(entryBuilder)
				                        .conditionally(
						                        BlockStatePropertyLootCondition.builder(tallPlant)
						                                                       .properties(
								                                                       StatePredicate.Builder.create()
								                                                                            .exactMatch(
										                                                                            TallPlantBlock.HALF,
										                                                                            DoubleBlockHalf.UPPER
								                                                                            )
						                                                       )
				                        )
				                        .conditionally(
						                        LocationCheckLootCondition.builder(
								                        LocationPredicate.Builder.create()
								                                                 .block(
										                                                 BlockPredicate.Builder.create()
										                                                                       .blocks(blockLookup, tallPlant)
										                                                                       .state(
												                                                                       StatePredicate.Builder.create()
												                                                                                            .exactMatch(
														                                                                                            TallPlantBlock.HALF,
														                                                                                            DoubleBlockHalf.LOWER
												                                                                                            )
										                                                                       )
								                                                 ),
								                        new BlockPos(0, -1, 0)
						                        )
				                        )
		                );
	}

	public LootTable.Builder candleDrops(Block candle) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .with(
						                        applyExplosionDecay(
								                        candle,
								                        ItemEntry.builder(candle)
								                                 .apply(
										                                 List.of(2, 3, 4),
										                                 candles -> SetCountLootFunction
												                                 .builder(ConstantLootNumberProvider.create(candles.intValue()))
												                                 .conditionally(
														                                 BlockStatePropertyLootCondition.builder(candle)
																                                 .properties(
																		                                 StatePredicate.Builder.create()
																				                                 .exactMatch(
																						                                 CandleBlock.CANDLES,
																						                                 candles.intValue()
																				                                 )
																                                 )
												                                 )
								                                 )
						                        )
				                        )
		                );
	}

	public LootTable.Builder segmentedDrops(Block segmented) {
		return segmented instanceof Segmented segmented2
				? LootTable.builder()
				           .pool(
						           LootPool.builder()
						                   .rolls(ConstantLootNumberProvider.create(1.0F))
						                   .with(
								                   applyExplosionDecay(
										                   segmented,
										                   ItemEntry.builder(segmented)
										                            .apply(
												                            IntStream.rangeClosed(1, 4).boxed().toList(),
												                            count -> SetCountLootFunction
												                                     .builder(ConstantLootNumberProvider.create(count.intValue()))
												                                     .conditionally(
														                                     BlockStatePropertyLootCondition.builder(segmented)
																                                     .properties(
																		                                     StatePredicate.Builder.create()
																				                                     .exactMatch(
																						                                     segmented2.getAmountProperty(),
																						                                     count.intValue()
																				                                     )
																                                     )
												                                     )
										                            )
								                   )
						                   )
				           )
				: dropsNothing();
	}

	public static LootTable.Builder candleCakeDrops(Block candleCake) {
		return LootTable
				.builder()
				.pool(LootPool
						.builder()
						.rolls(ConstantLootNumberProvider.create(1.0F))
						.with(ItemEntry.builder(candleCake)));
	}

	public static LootTable.Builder dropsNothing() {
		return LootTable.builder();
	}

	public abstract void generate();

	@Override
	public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> lootTableBiConsumer) {
		generate();
		Set<RegistryKey<LootTable>> registeredKeys = new HashSet<>();

		for (Block block : Registries.BLOCK) {
			if (block.isEnabled(requiredFeatures)) {
				block.getLootTableKey()
				     .ifPresent(lootTableKey -> {
					     if (registeredKeys.add(lootTableKey)) {
						     LootTable.Builder tableBuilder = lootTables.remove(lootTableKey);
						     if (tableBuilder == null) {
							     throw new IllegalStateException(
									     String.format(
											     Locale.ROOT,
											     "Missing loottable '%s' for '%s'",
											     lootTableKey.getValue(),
											     Registries.BLOCK.getId(block)
									     )
							     );
						     }

						     lootTableBiConsumer.accept(lootTableKey, tableBuilder);
					     }
				     });
			}
		}

		if (lootTables.isEmpty()) {
			return;
		}

		throw new IllegalStateException("Created block loot tables for non-blocks: " + lootTables.keySet());
	}

	public void addVinePlantDrop(Block vine, Block vinePlant) {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		LootTable.Builder tableBuilder = dropsWithSilkTouchOrShears(
				vine,
				ItemEntry.builder(vine)
				         .conditionally(TableBonusLootCondition.builder(
						         enchantmentLookup.getOrThrow(Enchantments.FORTUNE),
						         0.33F,
						         0.55F,
						         0.77F,
						         1.0F
				         ))
		);
		addDrop(vine, tableBuilder);
		addDrop(vinePlant, tableBuilder);
	}

	public LootTable.Builder doorDrops(Block block) {
		return this.dropsWithProperty(block, DoorBlock.HALF, DoubleBlockHalf.LOWER);
	}

	public void addPottedPlantDrops(Block block) {
		this.addDrop(block, flowerPot -> this.pottedPlantDrops(((FlowerPotBlock) flowerPot).getContent()));
	}

	public void addDropWithSilkTouch(Block block, Block drop) {
		this.addDrop(block, this.dropsWithSilkTouch(drop));
	}

	public void addDrop(Block block, ItemConvertible drop) {
		this.addDrop(block, this.drops(drop));
	}

	public void addDropWithSilkTouch(Block block) {
		this.addDropWithSilkTouch(block, block);
	}

	public void addDrop(Block block) {
		this.addDrop(block, block);
	}

	public void addDrop(Block block, Function<Block, LootTable.Builder> lootTableFunction) {
		this.addDrop(block, lootTableFunction.apply(block));
	}

	public void addDrop(Block block, LootTable.Builder lootTable) {
		this.lootTables.put(
				block
						.getLootTableKey()
						.orElseThrow(() -> new IllegalStateException("Block " + block + " does not have loot table")),
				lootTable
		);
	}
}
