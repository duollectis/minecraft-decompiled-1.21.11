package net.minecraft.data.loottable;

import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.datagen.v1.loot.FabricEntityLootTableGenerator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.FrogVariant;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.AnyOfLootCondition;
import net.minecraft.loot.condition.DamageSourcePropertiesLootCondition;
import net.minecraft.loot.condition.EntityPropertiesLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.AlternativeEntry;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.component.ComponentMapPredicate;
import net.minecraft.predicate.component.ComponentPredicateTypes;
import net.minecraft.predicate.component.ComponentsPredicate;
import net.minecraft.predicate.entity.*;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.EnchantmentsPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.DyeColor;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Базовый генератор таблиц лута для сущностей.
 * Обходит все зарегистрированные типы сущностей и проверяет наличие
 * соответствующих таблиц лута, выбрасывая исключение при их отсутствии.
 */
public abstract class EntityLootTableGenerator implements LootTableGenerator, FabricEntityLootTableGenerator {

	protected final RegistryWrapper.WrapperLookup registries;
	private final FeatureSet requiredFeatures;
	private final FeatureSet featureSet;
	private final Map<EntityType<?>, Map<RegistryKey<LootTable>, LootTable.Builder>> lootTables = Maps.newHashMap();

	protected final AnyOfLootCondition.Builder createSmeltLootCondition() {
		RegistryWrapper.Impl<Enchantment> enchantmentLookup = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return AnyOfLootCondition.builder(
				EntityPropertiesLootCondition.builder(
						LootContext.EntityReference.THIS,
						EntityPredicate.Builder.create().flags(EntityFlagsPredicate.Builder.create().onFire(true))
				),
				EntityPropertiesLootCondition.builder(
						LootContext.EntityReference.DIRECT_ATTACKER,
						EntityPredicate.Builder.create()
						                       .equipment(
								                       EntityEquipmentPredicate.Builder.create()
								                                                       .mainhand(
										                                                       ItemPredicate.Builder.create()
										                                                                           .components(
												                                                                           ComponentsPredicate.Builder.create()
												                                                                                                      .partial(
														                                                                                                       ComponentPredicateTypes.ENCHANTMENTS,
														                                                                                                       EnchantmentsPredicate.enchantments(
																                                                                                                       List.of(new EnchantmentPredicate(
																				                                                                                                       enchantmentLookup.getOrThrow(EnchantmentTags.SMELTS_LOOT),
																				                                                                                                       NumberRange.IntRange.ANY
																                                                                                                               ))
														                                                                                                       )
												                                                                                                      )
												                                                                                                      .build()
										                                                                           )
								                                                       )
						                       )
				)
		);
	}

	protected EntityLootTableGenerator(FeatureSet requiredFeatures, RegistryWrapper.WrapperLookup registries) {
		this(requiredFeatures, requiredFeatures, registries);
	}

	protected EntityLootTableGenerator(
			FeatureSet requiredFeatures,
			FeatureSet featureSet,
			RegistryWrapper.WrapperLookup registries
	) {
		this.requiredFeatures = requiredFeatures;
		this.featureSet = featureSet;
		this.registries = registries;
	}

	public static LootPool.Builder createForSheep(Map<DyeColor, RegistryKey<LootTable>> colorLootTables) {
		AlternativeEntry.Builder builder = AlternativeEntry.builder();

		for (Entry<DyeColor, RegistryKey<LootTable>> entry : colorLootTables.entrySet()) {
			builder = builder.alternatively(
					LootTableEntry.builder(entry.getValue())
					              .conditionally(
							              EntityPropertiesLootCondition.builder(
									              LootContext.EntityReference.THIS,
									              EntityPredicate.Builder.create()
									                                     .components(
											                                     ComponentsPredicate.Builder
													                                     .create()
													                                     .exact(ComponentMapPredicate.of(
															                                     DataComponentTypes.SHEEP_COLOR,
															                                     entry.getKey()
													                                     ))
													                                     .build()
									                                     )
									                                     .typeSpecific(SheepPredicate.unsheared())
							              )
					              )
			);
		}

		return LootPool.builder().with(builder);
	}

	public abstract void generate();

	@Override
	public void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> lootTableBiConsumer) {
		generate();
		Set<RegistryKey<LootTable>> registeredKeys = new HashSet<>();

		Registries.ENTITY_TYPE.streamEntries().forEach(entityTypeEntry -> {
			EntityType<?> type = entityTypeEntry.value();

			if (type.isEnabled(requiredFeatures) == false) {
				return;
			}

			Optional<RegistryKey<LootTable>> lootTableKey = type.getLootTableKey();

			if (lootTableKey.isPresent()) {
				Map<RegistryKey<LootTable>, LootTable.Builder> tableMaps = lootTables.remove(type);

				if (type.isEnabled(featureSet) && (tableMaps == null || tableMaps.containsKey(lootTableKey.get()) == false)) {
					throw new IllegalStateException(
							String.format(
									Locale.ROOT,
									"Missing loottable '%s' for '%s'",
									lootTableKey.get(),
									entityTypeEntry.registryKey().getValue()
							)
					);
				}

				if (tableMaps == null) {
					return;
				}

				tableMaps.forEach((tableKey, tableBuilder) -> {
					if (registeredKeys.add(tableKey) == false) {
						throw new IllegalStateException(
								String.format(
										Locale.ROOT,
										"Duplicate loottable '%s' for '%s'",
										tableKey,
										entityTypeEntry.registryKey().getValue()
								)
						);
					}

					lootTableBiConsumer.accept(tableKey, tableBuilder);
				});
			} else {
				Map<RegistryKey<LootTable>, LootTable.Builder> orphanedTables = lootTables.remove(type);

				if (orphanedTables == null) {
					return;
				}

				throw new IllegalStateException(
						String.format(
								Locale.ROOT,
								"Weird loottables '%s' for '%s', not a LivingEntity so should not have loot",
								orphanedTables.keySet()
								             .stream()
								             .map(key -> key.getValue().toString())
								             .collect(Collectors.joining(",")),
								entityTypeEntry.registryKey().getValue()
						)
				);
			}
		});

		if (lootTables.isEmpty()) {
			return;
		}

		throw new IllegalStateException(
				"Created loot tables for entities not supported by datapack: " + lootTables.keySet()
		);
	}

	protected LootCondition.Builder killedByFrog(RegistryEntryLookup<EntityType<?>> entityTypeLookup) {
		return DamageSourcePropertiesLootCondition.builder(
				DamageSourcePredicate.Builder
						.create()
						.sourceEntity(EntityPredicate.Builder.create().type(entityTypeLookup, EntityType.FROG))
		);
	}

	protected LootCondition.Builder killedByFrog(
			RegistryEntryLookup<EntityType<?>> entityTypeLookup,
			RegistryEntryLookup<FrogVariant> frogVariantLookup,
			RegistryKey<FrogVariant> frogVariant
	) {
		return DamageSourcePropertiesLootCondition.builder(
				DamageSourcePredicate.Builder.create()
				                             .sourceEntity(
						                             EntityPredicate.Builder.create()
						                                                    .type(entityTypeLookup, EntityType.FROG)
						                                                    .components(
								                                                    ComponentsPredicate.Builder.create()
								                                                                               .exact(ComponentMapPredicate.of(
										                                                                               DataComponentTypes.FROG_VARIANT,
										                                                                               frogVariantLookup.getOrThrow(
												                                                                               frogVariant)
								                                                                               ))
								                                                                               .build()
						                                                    )
				                             )
		);
	}

	public void register(EntityType<?> entityType, LootTable.Builder lootTable) {
		register(
				entityType,
				entityType.getLootTableKey()
				          .orElseThrow(() -> new IllegalStateException("Entity " + entityType + " has no loot table")),
				lootTable
		);
	}

	public void register(EntityType<?> entityType, RegistryKey<LootTable> tableKey, LootTable.Builder lootTable) {
		lootTables.computeIfAbsent(entityType, ignored -> new HashMap<>()).put(tableKey, lootTable);
	}
}
