package net.minecraft.component;

import com.mojang.serialization.Codec;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Sherds;
import net.minecraft.component.type.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.mob.ZombieNautilusVariant;
import net.minecraft.entity.passive.*;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.LazyRegistryEntryReference;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.Unit;
import net.minecraft.util.dynamic.CodecCache;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.village.VillagerType;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * {@code DataComponentTypes}.
 */
public class DataComponentTypes {

	static final CodecCache CACHE = new CodecCache(512);
	public static final ComponentType<NbtComponent>
			CUSTOM_DATA =
			register("custom_data", builder -> builder.codec(NbtComponent.CODEC));
	public static final ComponentType<Integer> MAX_STACK_SIZE = register(
			"max_stack_size", builder -> builder.codec(Codecs.rangedInt(1, 99)).packetCodec(PacketCodecs.VAR_INT)
	);
	public static final ComponentType<Integer> MAX_DAMAGE = register(
			"max_damage", builder -> builder.codec(Codecs.POSITIVE_INT).packetCodec(PacketCodecs.VAR_INT)
	);
	public static final ComponentType<Integer> DAMAGE = register(
			"damage",
			builder -> builder.codec(Codecs.NON_NEGATIVE_INT).skipsHandAnimation().packetCodec(PacketCodecs.VAR_INT)
	);
	public static final ComponentType<Unit>
			UNBREAKABLE =
			register("unbreakable", builder -> builder.codec(Unit.CODEC).packetCodec(Unit.PACKET_CODEC));
	public static final ComponentType<UseEffectsComponent> USE_EFFECTS = register(
			"use_effects",
			builder -> builder.codec(UseEffectsComponent.CODEC).packetCodec(UseEffectsComponent.PACKET_CODEC)
	);
	public static final ComponentType<Text> CUSTOM_NAME = register(
			"custom_name",
			builder -> builder.codec(TextCodecs.CODEC).packetCodec(TextCodecs.REGISTRY_PACKET_CODEC).cache()
	);
	public static final ComponentType<Float> MINIMUM_ATTACK_CHARGE = register(
			"minimum_attack_charge",
			builder -> builder.codec(Codecs.rangedInclusiveFloat(0.0F, 1.0F)).packetCodec(PacketCodecs.FLOAT)
	);
	public static final ComponentType<LazyRegistryEntryReference<DamageType>> DAMAGE_TYPE = register(
			"damage_type",
			builder -> builder
					.codec(LazyRegistryEntryReference.createCodec(RegistryKeys.DAMAGE_TYPE, DamageType.ENTRY_CODEC))
					.packetCodec(LazyRegistryEntryReference.createPacketCodec(
							RegistryKeys.DAMAGE_TYPE,
							DamageType.ENTRY_PACKET_CODEC
					))
	);
	public static final ComponentType<Text> ITEM_NAME = register(
			"item_name",
			builder -> builder.codec(TextCodecs.CODEC).packetCodec(TextCodecs.REGISTRY_PACKET_CODEC).cache()
	);
	public static final ComponentType<Identifier> ITEM_MODEL = register(
			"item_model", builder -> builder.codec(Identifier.CODEC).packetCodec(Identifier.PACKET_CODEC).cache()
	);
	public static final ComponentType<LoreComponent> LORE = register(
			"lore", builder -> builder.codec(LoreComponent.CODEC).packetCodec(LoreComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<Rarity>
			RARITY =
			register("rarity", builder -> builder.codec(Rarity.CODEC).packetCodec(Rarity.PACKET_CODEC));
	public static final ComponentType<ItemEnchantmentsComponent> ENCHANTMENTS = register(
			"enchantments",
			builder -> builder
					.codec(ItemEnchantmentsComponent.CODEC)
					.packetCodec(ItemEnchantmentsComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<BlockPredicatesComponent> CAN_PLACE_ON = register(
			"can_place_on",
			builder -> builder
					.codec(BlockPredicatesComponent.CODEC)
					.packetCodec(BlockPredicatesComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<BlockPredicatesComponent> CAN_BREAK = register(
			"can_break",
			builder -> builder
					.codec(BlockPredicatesComponent.CODEC)
					.packetCodec(BlockPredicatesComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<AttributeModifiersComponent> ATTRIBUTE_MODIFIERS = register(
			"attribute_modifiers",
			builder -> builder
					.codec(AttributeModifiersComponent.CODEC)
					.packetCodec(AttributeModifiersComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<CustomModelDataComponent> CUSTOM_MODEL_DATA = register(
			"custom_model_data",
			builder -> builder.codec(CustomModelDataComponent.CODEC).packetCodec(CustomModelDataComponent.PACKET_CODEC)
	);
	public static final ComponentType<TooltipDisplayComponent> TOOLTIP_DISPLAY = register(
			"tooltip_display",
			builder -> builder
					.codec(TooltipDisplayComponent.CODEC)
					.packetCodec(TooltipDisplayComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<Integer> REPAIR_COST = register(
			"repair_cost", builder -> builder.codec(Codecs.NON_NEGATIVE_INT).packetCodec(PacketCodecs.VAR_INT)
	);
	public static final ComponentType<Unit>
			CREATIVE_SLOT_LOCK =
			register("creative_slot_lock", builder -> builder.packetCodec(Unit.PACKET_CODEC));
	public static final ComponentType<Boolean> ENCHANTMENT_GLINT_OVERRIDE = register(
			"enchantment_glint_override", builder -> builder.codec(Codec.BOOL).packetCodec(PacketCodecs.BOOLEAN)
	);
	public static final ComponentType<Unit>
			INTANGIBLE_PROJECTILE =
			register("intangible_projectile", builder -> builder.codec(Unit.CODEC));
	public static final ComponentType<FoodComponent> FOOD = register(
			"food", builder -> builder.codec(FoodComponent.CODEC).packetCodec(FoodComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<ConsumableComponent> CONSUMABLE = register(
			"consumable",
			builder -> builder.codec(ConsumableComponent.CODEC).packetCodec(ConsumableComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<UseRemainderComponent> USE_REMAINDER = register(
			"use_remainder",
			builder -> builder
					.codec(UseRemainderComponent.CODEC)
					.packetCodec(UseRemainderComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<UseCooldownComponent> USE_COOLDOWN = register(
			"use_cooldown",
			builder -> builder.codec(UseCooldownComponent.CODEC).packetCodec(UseCooldownComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<DamageResistantComponent> DAMAGE_RESISTANT = register(
			"damage_resistant",
			builder -> builder
					.codec(DamageResistantComponent.CODEC)
					.packetCodec(DamageResistantComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<ToolComponent> TOOL = register(
			"tool", builder -> builder.codec(ToolComponent.CODEC).packetCodec(ToolComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<WeaponComponent> WEAPON = register(
			"weapon", builder -> builder.codec(WeaponComponent.CODEC).packetCodec(WeaponComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<AttackRangeComponent> ATTACK_RANGE = register(
			"attack_range",
			builder -> builder.codec(AttackRangeComponent.CODEC).packetCodec(AttackRangeComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<EnchantableComponent> ENCHANTABLE = register(
			"enchantable",
			builder -> builder.codec(EnchantableComponent.CODEC).packetCodec(EnchantableComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<EquippableComponent> EQUIPPABLE = register(
			"equippable",
			builder -> builder.codec(EquippableComponent.CODEC).packetCodec(EquippableComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<RepairableComponent> REPAIRABLE = register(
			"repairable",
			builder -> builder.codec(RepairableComponent.CODEC).packetCodec(RepairableComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<Unit>
			GLIDER =
			register("glider", builder -> builder.codec(Unit.CODEC).packetCodec(Unit.PACKET_CODEC));
	public static final ComponentType<Identifier> TOOLTIP_STYLE = register(
			"tooltip_style", builder -> builder.codec(Identifier.CODEC).packetCodec(Identifier.PACKET_CODEC).cache()
	);
	public static final ComponentType<DeathProtectionComponent> DEATH_PROTECTION = register(
			"death_protection",
			builder -> builder
					.codec(DeathProtectionComponent.CODEC)
					.packetCodec(DeathProtectionComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<BlocksAttacksComponent> BLOCKS_ATTACKS = register(
			"blocks_attacks",
			builder -> builder
					.codec(BlocksAttacksComponent.CODEC)
					.packetCodec(BlocksAttacksComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<PiercingWeaponComponent> PIERCING_WEAPON = register(
			"piercing_weapon",
			builder -> builder
					.codec(PiercingWeaponComponent.CODEC)
					.packetCodec(PiercingWeaponComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<KineticWeaponComponent> KINETIC_WEAPON = register(
			"kinetic_weapon",
			builder -> builder
					.codec(KineticWeaponComponent.CODEC)
					.packetCodec(KineticWeaponComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<SwingAnimationComponent> SWING_ANIMATION = register(
			"swing_animation",
			builder -> builder.codec(SwingAnimationComponent.CODEC).packetCodec(SwingAnimationComponent.PACKET_CODEC)
	);
	public static final ComponentType<ItemEnchantmentsComponent> STORED_ENCHANTMENTS = register(
			"stored_enchantments",
			builder -> builder
					.codec(ItemEnchantmentsComponent.CODEC)
					.packetCodec(ItemEnchantmentsComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<DyedColorComponent> DYED_COLOR = register(
			"dyed_color",
			builder -> builder.codec(DyedColorComponent.CODEC).packetCodec(DyedColorComponent.PACKET_CODEC)
	);
	public static final ComponentType<MapColorComponent> MAP_COLOR = register(
			"map_color", builder -> builder.codec(MapColorComponent.CODEC).packetCodec(MapColorComponent.PACKET_CODEC)
	);
	public static final ComponentType<MapIdComponent> MAP_ID = register(
			"map_id", builder -> builder.codec(MapIdComponent.CODEC).packetCodec(MapIdComponent.PACKET_CODEC)
	);
	public static final ComponentType<MapDecorationsComponent> MAP_DECORATIONS = register(
			"map_decorations", builder -> builder.codec(MapDecorationsComponent.CODEC).cache()
	);
	public static final ComponentType<MapPostProcessingComponent> MAP_POST_PROCESSING = register(
			"map_post_processing", builder -> builder.packetCodec(MapPostProcessingComponent.PACKET_CODEC)
	);
	public static final ComponentType<ChargedProjectilesComponent> CHARGED_PROJECTILES = register(
			"charged_projectiles",
			builder -> builder
					.codec(ChargedProjectilesComponent.CODEC)
					.packetCodec(ChargedProjectilesComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<BundleContentsComponent> BUNDLE_CONTENTS = register(
			"bundle_contents",
			builder -> builder
					.codec(BundleContentsComponent.CODEC)
					.packetCodec(BundleContentsComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<PotionContentsComponent> POTION_CONTENTS = register(
			"potion_contents",
			builder -> builder
					.codec(PotionContentsComponent.CODEC)
					.packetCodec(PotionContentsComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<Float> POTION_DURATION_SCALE = register(
			"potion_duration_scale",
			builder -> builder.codec(Codecs.NON_NEGATIVE_FLOAT).packetCodec(PacketCodecs.FLOAT).cache()
	);
	public static final ComponentType<SuspiciousStewEffectsComponent> SUSPICIOUS_STEW_EFFECTS = register(
			"suspicious_stew_effects",
			builder -> builder
					.codec(SuspiciousStewEffectsComponent.CODEC)
					.packetCodec(SuspiciousStewEffectsComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<WritableBookContentComponent> WRITABLE_BOOK_CONTENT = register(
			"writable_book_content",
			builder -> builder
					.codec(WritableBookContentComponent.CODEC)
					.packetCodec(WritableBookContentComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<WrittenBookContentComponent> WRITTEN_BOOK_CONTENT = register(
			"written_book_content",
			builder -> builder
					.codec(WrittenBookContentComponent.CODEC)
					.packetCodec(WrittenBookContentComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<ArmorTrim>
			TRIM =
			register("trim", builder -> builder.codec(ArmorTrim.CODEC).packetCodec(ArmorTrim.PACKET_CODEC).cache());
	public static final ComponentType<DebugStickStateComponent> DEBUG_STICK_STATE = register(
			"debug_stick_state", builder -> builder.codec(DebugStickStateComponent.CODEC).cache()
	);
	public static final ComponentType<TypedEntityData<EntityType<?>>> ENTITY_DATA = register(
			"entity_data",
			builder -> builder
					.codec(TypedEntityData.createCodec(EntityType.CODEC))
					.packetCodec(TypedEntityData.createPacketCodec(EntityType.PACKET_CODEC))
	);
	public static final ComponentType<NbtComponent> BUCKET_ENTITY_DATA = register(
			"bucket_entity_data", builder -> builder.codec(NbtComponent.CODEC).packetCodec(NbtComponent.PACKET_CODEC)
	);
	public static final ComponentType<TypedEntityData<BlockEntityType<?>>> BLOCK_ENTITY_DATA = register(
			"block_entity_data",
			builder -> builder.codec(TypedEntityData.createCodec(Registries.BLOCK_ENTITY_TYPE.getCodec()))
			                  .packetCodec(TypedEntityData.createPacketCodec(PacketCodecs.registryValue(RegistryKeys.BLOCK_ENTITY_TYPE)))
	);
	public static final ComponentType<InstrumentComponent> INSTRUMENT = register(
			"instrument",
			builder -> builder.codec(InstrumentComponent.CODEC).packetCodec(InstrumentComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<ProvidesTrimMaterialComponent> PROVIDES_TRIM_MATERIAL = register(
			"provides_trim_material",
			builder -> builder
					.codec(ProvidesTrimMaterialComponent.CODEC)
					.packetCodec(ProvidesTrimMaterialComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<OminousBottleAmplifierComponent> OMINOUS_BOTTLE_AMPLIFIER = register(
			"ominous_bottle_amplifier",
			builder -> builder
					.codec(OminousBottleAmplifierComponent.CODEC)
					.packetCodec(OminousBottleAmplifierComponent.PACKET_CODEC)
	);
	public static final ComponentType<JukeboxPlayableComponent> JUKEBOX_PLAYABLE = register(
			"jukebox_playable",
			builder -> builder.codec(JukeboxPlayableComponent.CODEC).packetCodec(JukeboxPlayableComponent.PACKET_CODEC)
	);
	public static final ComponentType<TagKey<BannerPattern>> PROVIDES_BANNER_PATTERNS = register(
			"provides_banner_patterns",
			builder -> builder
					.codec(TagKey.codec(RegistryKeys.BANNER_PATTERN))
					.packetCodec(TagKey.packetCodec(RegistryKeys.BANNER_PATTERN))
					.cache()
	);
	public static final ComponentType<List<RegistryKey<Recipe<?>>>>
			RECIPES =
			register("recipes", builder -> builder.codec(Recipe.KEY_CODEC.listOf()).cache());
	public static final ComponentType<LodestoneTrackerComponent> LODESTONE_TRACKER = register(
			"lodestone_tracker",
			builder -> builder
					.codec(LodestoneTrackerComponent.CODEC)
					.packetCodec(LodestoneTrackerComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<FireworkExplosionComponent> FIREWORK_EXPLOSION = register(
			"firework_explosion",
			builder -> builder
					.codec(FireworkExplosionComponent.CODEC)
					.packetCodec(FireworkExplosionComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<FireworksComponent> FIREWORKS = register(
			"fireworks",
			builder -> builder.codec(FireworksComponent.CODEC).packetCodec(FireworksComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<ProfileComponent> PROFILE = register(
			"profile",
			builder -> builder.codec(ProfileComponent.CODEC).packetCodec(ProfileComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<Identifier> NOTE_BLOCK_SOUND = register(
			"note_block_sound", builder -> builder.codec(Identifier.CODEC).packetCodec(Identifier.PACKET_CODEC)
	);
	public static final ComponentType<BannerPatternsComponent> BANNER_PATTERNS = register(
			"banner_patterns",
			builder -> builder
					.codec(BannerPatternsComponent.CODEC)
					.packetCodec(BannerPatternsComponent.PACKET_CODEC)
					.cache()
	);
	public static final ComponentType<DyeColor>
			BASE_COLOR =
			register("base_color", builder -> builder.codec(DyeColor.CODEC).packetCodec(DyeColor.PACKET_CODEC));
	public static final ComponentType<Sherds> POT_DECORATIONS = register(
			"pot_decorations", builder -> builder.codec(Sherds.CODEC).packetCodec(Sherds.PACKET_CODEC).cache()
	);
	public static final ComponentType<ContainerComponent> CONTAINER = register(
			"container",
			builder -> builder.codec(ContainerComponent.CODEC).packetCodec(ContainerComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<BlockStateComponent> BLOCK_STATE = register(
			"block_state",
			builder -> builder.codec(BlockStateComponent.CODEC).packetCodec(BlockStateComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<BeesComponent> BEES = register(
			"bees", builder -> builder.codec(BeesComponent.CODEC).packetCodec(BeesComponent.PACKET_CODEC).cache()
	);
	public static final ComponentType<ContainerLock>
			LOCK =
			register("lock", builder -> builder.codec(ContainerLock.CODEC));
	public static final ComponentType<ContainerLootComponent>
			CONTAINER_LOOT =
			register("container_loot", builder -> builder.codec(ContainerLootComponent.CODEC));
	public static final ComponentType<RegistryEntry<SoundEvent>> BREAK_SOUND = register(
			"break_sound",
			builder -> builder.codec(SoundEvent.ENTRY_CODEC).packetCodec(SoundEvent.ENTRY_PACKET_CODEC).cache()
	);
	public static final ComponentType<RegistryEntry<VillagerType>> VILLAGER_VARIANT = register(
			"villager/variant", builder -> builder.codec(VillagerType.CODEC).packetCodec(VillagerType.PACKET_CODEC)
	);
	public static final ComponentType<RegistryEntry<WolfVariant>> WOLF_VARIANT = register(
			"wolf/variant",
			builder -> builder.codec(WolfVariant.ENTRY_CODEC).packetCodec(WolfVariant.ENTRY_PACKET_CODEC)
	);
	public static final ComponentType<RegistryEntry<WolfSoundVariant>> WOLF_SOUND_VARIANT = register(
			"wolf/sound_variant",
			builder -> builder.codec(WolfSoundVariant.ENTRY_CODEC).packetCodec(WolfSoundVariant.PACKET_CODEC)
	);
	public static final ComponentType<DyeColor> WOLF_COLLAR = register(
			"wolf/collar", builder -> builder.codec(DyeColor.CODEC).packetCodec(DyeColor.PACKET_CODEC)
	);
	public static final ComponentType<FoxEntity.Variant> FOX_VARIANT = register(
			"fox/variant", builder -> builder.codec(FoxEntity.Variant.CODEC).packetCodec(FoxEntity.Variant.PACKET_CODEC)
	);
	public static final ComponentType<SalmonEntity.Variant> SALMON_SIZE = register(
			"salmon/size",
			builder -> builder.codec(SalmonEntity.Variant.CODEC).packetCodec(SalmonEntity.Variant.PACKET_CODEC)
	);
	public static final ComponentType<ParrotEntity.Variant> PARROT_VARIANT = register(
			"parrot/variant",
			builder -> builder.codec(ParrotEntity.Variant.CODEC).packetCodec(ParrotEntity.Variant.PACKET_CODEC)
	);
	public static final ComponentType<TropicalFishEntity.Pattern> TROPICAL_FISH_PATTERN = register(
			"tropical_fish/pattern",
			builder -> builder
					.codec(TropicalFishEntity.Pattern.CODEC)
					.packetCodec(TropicalFishEntity.Pattern.PACKET_CODEC)
	);
	public static final ComponentType<DyeColor> TROPICAL_FISH_BASE_COLOR = register(
			"tropical_fish/base_color", builder -> builder.codec(DyeColor.CODEC).packetCodec(DyeColor.PACKET_CODEC)
	);
	public static final ComponentType<DyeColor> TROPICAL_FISH_PATTERN_COLOR = register(
			"tropical_fish/pattern_color", builder -> builder.codec(DyeColor.CODEC).packetCodec(DyeColor.PACKET_CODEC)
	);
	public static final ComponentType<MooshroomEntity.Variant> MOOSHROOM_VARIANT = register(
			"mooshroom/variant",
			builder -> builder.codec(MooshroomEntity.Variant.CODEC).packetCodec(MooshroomEntity.Variant.PACKET_CODEC)
	);
	public static final ComponentType<RabbitEntity.Variant> RABBIT_VARIANT = register(
			"rabbit/variant",
			builder -> builder.codec(RabbitEntity.Variant.CODEC).packetCodec(RabbitEntity.Variant.PACKET_CODEC)
	);
	public static final ComponentType<RegistryEntry<PigVariant>> PIG_VARIANT = register(
			"pig/variant", builder -> builder.codec(PigVariant.ENTRY_CODEC).packetCodec(PigVariant.ENTRY_PACKET_CODEC)
	);
	public static final ComponentType<RegistryEntry<CowVariant>> COW_VARIANT = register(
			"cow/variant", builder -> builder.codec(CowVariant.ENTRY_CODEC).packetCodec(CowVariant.ENTRY_PACKET_CODEC)
	);
	public static final ComponentType<LazyRegistryEntryReference<ChickenVariant>> CHICKEN_VARIANT = register(
			"chicken/variant",
			builder -> builder
					.codec(LazyRegistryEntryReference.createCodec(
							RegistryKeys.CHICKEN_VARIANT,
							ChickenVariant.ENTRY_CODEC
					))
					.packetCodec(LazyRegistryEntryReference.createPacketCodec(
							RegistryKeys.CHICKEN_VARIANT,
							ChickenVariant.ENTRY_PACKET_CODEC
					))
	);
	public static final ComponentType<LazyRegistryEntryReference<ZombieNautilusVariant>>
			ZOMBIE_NAUTILUS_VARIANT =
			register(
					"zombie_nautilus/variant",
					builder -> builder
							.codec(LazyRegistryEntryReference.createCodec(
									RegistryKeys.ZOMBIE_NAUTILUS_VARIANT,
									ZombieNautilusVariant.ENTRY_CODEC
							))
							.packetCodec(LazyRegistryEntryReference.createPacketCodec(
									RegistryKeys.ZOMBIE_NAUTILUS_VARIANT,
									ZombieNautilusVariant.ENTRY_PACKET_CODEC
							))
			);
	public static final ComponentType<RegistryEntry<FrogVariant>> FROG_VARIANT = register(
			"frog/variant", builder -> builder.codec(FrogVariant.ENTRY_CODEC).packetCodec(FrogVariant.PACKET_CODEC)
	);
	public static final ComponentType<HorseColor> HORSE_VARIANT = register(
			"horse/variant", builder -> builder.codec(HorseColor.CODEC).packetCodec(HorseColor.PACKET_CODEC)
	);
	public static final ComponentType<RegistryEntry<PaintingVariant>> PAINTING_VARIANT = register(
			"painting/variant",
			builder -> builder.codec(PaintingVariant.ENTRY_CODEC).packetCodec(PaintingVariant.ENTRY_PACKET_CODEC)
	);
	public static final ComponentType<LlamaEntity.Variant> LLAMA_VARIANT = register(
			"llama/variant",
			builder -> builder.codec(LlamaEntity.Variant.CODEC).packetCodec(LlamaEntity.Variant.PACKET_CODEC)
	);
	public static final ComponentType<AxolotlEntity.Variant> AXOLOTL_VARIANT = register(
			"axolotl/variant",
			builder -> builder.codec(AxolotlEntity.Variant.CODEC).packetCodec(AxolotlEntity.Variant.PACKET_CODEC)
	);
	public static final ComponentType<RegistryEntry<CatVariant>> CAT_VARIANT = register(
			"cat/variant", builder -> builder.codec(CatVariant.ENTRY_CODEC).packetCodec(CatVariant.PACKET_CODEC)
	);
	public static final ComponentType<DyeColor>
			CAT_COLLAR =
			register("cat/collar", builder -> builder.codec(DyeColor.CODEC).packetCodec(DyeColor.PACKET_CODEC));
	public static final ComponentType<DyeColor> SHEEP_COLOR = register(
			"sheep/color", builder -> builder.codec(DyeColor.CODEC).packetCodec(DyeColor.PACKET_CODEC)
	);
	public static final ComponentType<DyeColor> SHULKER_COLOR = register(
			"shulker/color", builder -> builder.codec(DyeColor.CODEC).packetCodec(DyeColor.PACKET_CODEC)
	);
	public static final ComponentMap DEFAULT_ITEM_COMPONENTS = ComponentMap.builder()
	                                                                       .add(MAX_STACK_SIZE, 64)
	                                                                       .add(LORE, LoreComponent.DEFAULT)
	                                                                       .add(
			                                                                       ENCHANTMENTS,
			                                                                       ItemEnchantmentsComponent.DEFAULT
	                                                                       )
	                                                                       .add(REPAIR_COST, 0)
	                                                                       .add(
			                                                                       USE_EFFECTS,
			                                                                       UseEffectsComponent.DEFAULT
	                                                                       )
	                                                                       .add(
			                                                                       ATTRIBUTE_MODIFIERS,
			                                                                       AttributeModifiersComponent.DEFAULT
	                                                                       )
	                                                                       .add(RARITY, Rarity.COMMON)
	                                                                       .add(
			                                                                       BREAK_SOUND,
			                                                                       SoundEvents.ENTITY_ITEM_BREAK
	                                                                       )
	                                                                       .add(
			                                                                       TOOLTIP_DISPLAY,
			                                                                       TooltipDisplayComponent.DEFAULT
	                                                                       )
	                                                                       .add(
			                                                                       SWING_ANIMATION,
			                                                                       SwingAnimationComponent.DEFAULT
	                                                                       )
	                                                                       .build();

	public static ComponentType<?> getDefault(Registry<ComponentType<?>> registry) {
		return CUSTOM_DATA;
	}

	private static <T> ComponentType<T> register(String id, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
		return Registry.register(
				Registries.DATA_COMPONENT_TYPE,
				id,
				builderOperator.apply(ComponentType.builder()).build()
		);
	}
}
