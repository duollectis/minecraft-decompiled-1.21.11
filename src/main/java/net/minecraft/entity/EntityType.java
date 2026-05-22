package net.minecraft.entity;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.*;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.entity.vehicle.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlag;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.featuretoggle.ToggleableFeature;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Тип сущности — центральный реестровый объект, описывающий все характеристики сущности:
 * размеры, группу спауна, дальность трекинга, фабрику создания и т.д.
 * Все конкретные типы объявлены как {@code public static final} константы в этом классе.
 */
public class EntityType<T extends Entity> implements ToggleableFeature, TypeFilter<Entity, T> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final RegistryEntry.Reference<EntityType<?>> registryEntry = Registries.ENTITY_TYPE.createEntry(this);
	public static final Codec<EntityType<?>> CODEC = Registries.ENTITY_TYPE.getCodec();
	public static final PacketCodec<RegistryByteBuf, EntityType<?>>
			PACKET_CODEC =
			PacketCodecs.registryValue(RegistryKeys.ENTITY_TYPE);
	private static final float HORSE_WIDTH = 1.3964844F;
	private static final int DEFAULT_TRACKING_RANGE = 10;
	public static final EntityType<BoatEntity> ACACIA_BOAT = register(
			"acacia_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.ACACIA_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> ACACIA_CHEST_BOAT = register(
			"acacia_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.ACACIA_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<AllayEntity> ALLAY = register(
			"allay",
			EntityType.Builder.create(AllayEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.35F, 0.6F)
			                  .eyeHeight(0.36F)
			                  .vehicleAttachment(0.04F)
			                  .maxTrackingRange(8)
			                  .trackingTickInterval(2)
	);
	public static final EntityType<AreaEffectCloudEntity> AREA_EFFECT_CLOUD = register(
			"area_effect_cloud",
			EntityType.Builder.<AreaEffectCloudEntity>create(AreaEffectCloudEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .makeFireImmune()
			                  .dimensions(6.0F, 0.5F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(Integer.MAX_VALUE)
	);
	public static final EntityType<ArmadilloEntity> ARMADILLO = register(
			"armadillo",
			EntityType.Builder
					.create(ArmadilloEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.7F, 0.65F)
					.eyeHeight(0.26F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ArmorStandEntity> ARMOR_STAND = register(
			"armor_stand",
			EntityType.Builder
					.<ArmorStandEntity>create(ArmorStandEntity::new, SpawnGroup.MISC)
					.dimensions(0.5F, 1.975F)
					.eyeHeight(1.7775F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ArrowEntity> ARROW = register(
			"arrow",
			EntityType.Builder.<ArrowEntity>create(ArrowEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.5F, 0.5F)
			                  .eyeHeight(0.13F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(20)
	);
	public static final EntityType<AxolotlEntity> AXOLOTL = register(
			"axolotl",
			EntityType.Builder
					.create(AxolotlEntity::new, SpawnGroup.AXOLOTLS)
					.dimensions(0.75F, 0.42F)
					.eyeHeight(0.2751F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestRaftEntity> BAMBOO_CHEST_RAFT = register(
			"bamboo_chest_raft",
			EntityType.Builder.create(getChestRaftFactory(() -> Items.BAMBOO_CHEST_RAFT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<RaftEntity> BAMBOO_RAFT = register(
			"bamboo_raft",
			EntityType.Builder.create(getRaftFactory(() -> Items.BAMBOO_RAFT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<BatEntity> BAT = register(
			"bat",
			EntityType.Builder
					.create(BatEntity::new, SpawnGroup.AMBIENT)
					.dimensions(0.5F, 0.9F)
					.eyeHeight(0.45F)
					.maxTrackingRange(5)
	);
	public static final EntityType<BeeEntity> BEE = register(
			"bee",
			EntityType.Builder
					.create(BeeEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.7F, 0.6F)
					.eyeHeight(0.3F)
					.maxTrackingRange(8)
	);
	public static final EntityType<BoatEntity> BIRCH_BOAT = register(
			"birch_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.BIRCH_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> BIRCH_CHEST_BOAT = register(
			"birch_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.BIRCH_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<BlazeEntity> BLAZE = register(
			"blaze",
			EntityType.Builder
					.create(BlazeEntity::new, SpawnGroup.MONSTER)
					.makeFireImmune()
					.dimensions(0.6F, 1.8F)
					.maxTrackingRange(8)
					.notAllowedInPeaceful()
	);
	public static final EntityType<DisplayEntity.BlockDisplayEntity> BLOCK_DISPLAY = register(
			"block_display",
			EntityType.Builder.create(DisplayEntity.BlockDisplayEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.0F, 0.0F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(1)
	);
	public static final EntityType<BoggedEntity> BOGGED = register(
			"bogged",
			EntityType.Builder.create(BoggedEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.99F)
			                  .eyeHeight(1.74F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<BreezeEntity> BREEZE = register(
			"breeze",
			EntityType.Builder
					.create(BreezeEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.6F, 1.77F)
					.eyeHeight(1.3452F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
					.notAllowedInPeaceful()
	);
	public static final EntityType<BreezeWindChargeEntity> BREEZE_WIND_CHARGE = register(
			"breeze_wind_charge",
			EntityType.Builder.<BreezeWindChargeEntity>create(BreezeWindChargeEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.3125F, 0.3125F)
			                  .eyeHeight(0.0F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<CamelEntity> CAMEL = register(
			"camel",
			EntityType.Builder
					.create(CamelEntity::new, SpawnGroup.CREATURE)
					.dimensions(1.7F, 2.375F)
					.eyeHeight(2.275F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<CamelHuskEntity> CAMEL_HUSK = register(
			"camel_husk",
			EntityType.Builder
					.create(CamelHuskEntity::new, SpawnGroup.MONSTER)
					.dimensions(1.7F, 2.375F)
					.eyeHeight(2.275F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<CatEntity> CAT = register(
			"cat",
			EntityType.Builder
					.create(CatEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.6F, 0.7F)
					.eyeHeight(0.35F)
					.passengerAttachments(0.5125F)
					.maxTrackingRange(8)
	);
	public static final EntityType<CaveSpiderEntity> CAVE_SPIDER = register(
			"cave_spider",
			EntityType.Builder
					.create(CaveSpiderEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.7F, 0.5F)
					.eyeHeight(0.45F)
					.maxTrackingRange(8)
					.notAllowedInPeaceful()
	);
	public static final EntityType<BoatEntity> CHERRY_BOAT = register(
			"cherry_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.CHERRY_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> CHERRY_CHEST_BOAT = register(
			"cherry_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.CHERRY_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestMinecartEntity> CHEST_MINECART = register(
			"chest_minecart",
			EntityType.Builder.create(ChestMinecartEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.98F, 0.7F)
			                  .passengerAttachments(0.1875F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<ChickenEntity> CHICKEN = register(
			"chicken",
			EntityType.Builder.create(ChickenEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.4F, 0.7F)
			                  .eyeHeight(0.644F)
			                  .passengerAttachments(new Vec3d(0.0, 0.7, -0.1))
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<CodEntity> COD = register(
			"cod",
			EntityType.Builder
					.create(CodEntity::new, SpawnGroup.WATER_AMBIENT)
					.dimensions(0.5F, 0.3F)
					.eyeHeight(0.195F)
					.maxTrackingRange(4)
	);
	public static final EntityType<CopperGolemEntity> COPPER_GOLEM = register(
			"copper_golem",
			EntityType.Builder
					.create(CopperGolemEntity::new, SpawnGroup.MISC)
					.dimensions(0.49F, 0.98F)
					.eyeHeight(0.8125F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<CommandBlockMinecartEntity> COMMAND_BLOCK_MINECART = register(
			"command_block_minecart",
			EntityType.Builder.create(CommandBlockMinecartEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.98F, 0.7F)
			                  .passengerAttachments(0.1875F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<CowEntity> COW = register(
			"cow",
			EntityType.Builder
					.create(CowEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.9F, 1.4F)
					.eyeHeight(1.3F)
					.passengerAttachments(1.36875F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<CreakingEntity> CREAKING = register(
			"creaking",
			EntityType.Builder
					.create(CreakingEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.9F, 2.7F)
					.eyeHeight(2.3F)
					.maxTrackingRange(8)
					.notAllowedInPeaceful()
	);
	public static final EntityType<CreeperEntity> CREEPER = register(
			"creeper",
			EntityType.Builder
					.create(CreeperEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.6F, 1.7F)
					.maxTrackingRange(8)
					.notAllowedInPeaceful()
	);
	public static final EntityType<BoatEntity> DARK_OAK_BOAT = register(
			"dark_oak_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.DARK_OAK_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> DARK_OAK_CHEST_BOAT = register(
			"dark_oak_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.DARK_OAK_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<DolphinEntity> DOLPHIN = register(
			"dolphin",
			EntityType.Builder
					.create(DolphinEntity::new, SpawnGroup.WATER_CREATURE)
					.dimensions(0.9F, 0.6F)
					.eyeHeight(0.3F)
	);
	public static final EntityType<DonkeyEntity> DONKEY = register(
			"donkey",
			EntityType.Builder.create(DonkeyEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(HORSE_WIDTH, 1.5F)
			                  .eyeHeight(1.425F)
			                  .passengerAttachments(1.1125F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<DragonFireballEntity> DRAGON_FIREBALL = register(
			"dragon_fireball",
			EntityType.Builder.<DragonFireballEntity>create(DragonFireballEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.0F, 1.0F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<DrownedEntity> DROWNED = register(
			"drowned",
			EntityType.Builder.create(DrownedEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .eyeHeight(1.74F)
			                  .passengerAttachments(2.0125F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<EggEntity> EGG = register(
			"egg",
			EntityType.Builder.<EggEntity>create(EggEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ElderGuardianEntity> ELDER_GUARDIAN = register(
			"elder_guardian",
			EntityType.Builder.create(ElderGuardianEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(1.9975F, 1.9975F)
			                  .eyeHeight(0.99875F)
			                  .passengerAttachments(2.350625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<EndermanEntity> ENDERMAN = register(
			"enderman",
			EntityType.Builder.create(EndermanEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 2.9F)
			                  .eyeHeight(2.55F)
			                  .passengerAttachments(2.80625F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<EndermiteEntity> ENDERMITE = register(
			"endermite",
			EntityType.Builder.create(EndermiteEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.4F, 0.3F)
			                  .eyeHeight(0.13F)
			                  .passengerAttachments(0.2375F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<EnderDragonEntity> ENDER_DRAGON = register(
			"ender_dragon",
			EntityType.Builder.create(EnderDragonEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .dimensions(16.0F, 8.0F)
			                  .passengerAttachments(3.0F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<EnderPearlEntity> ENDER_PEARL = register(
			"ender_pearl",
			EntityType.Builder.<EnderPearlEntity>create(EnderPearlEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<EndCrystalEntity> END_CRYSTAL = register(
			"end_crystal",
			EntityType.Builder.<EndCrystalEntity>create(EndCrystalEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .makeFireImmune()
			                  .dimensions(2.0F, 2.0F)
			                  .maxTrackingRange(16)
			                  .trackingTickInterval(Integer.MAX_VALUE)
	);
	public static final EntityType<EvokerEntity> EVOKER = register(
			"evoker",
			EntityType.Builder.create(EvokerEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .passengerAttachments(2.0F)
			                  .vehicleAttachment(-0.6F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<EvokerFangsEntity> EVOKER_FANGS = register(
			"evoker_fangs",
			EntityType.Builder.<EvokerFangsEntity>create(EvokerFangsEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.5F, 0.8F)
			                  .maxTrackingRange(6)
			                  .trackingTickInterval(2)
	);
	public static final EntityType<ExperienceBottleEntity> EXPERIENCE_BOTTLE = register(
			"experience_bottle",
			EntityType.Builder.<ExperienceBottleEntity>create(ExperienceBottleEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ExperienceOrbEntity> EXPERIENCE_ORB = register(
			"experience_orb",
			EntityType.Builder.<ExperienceOrbEntity>create(ExperienceOrbEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.5F, 0.5F)
			                  .maxTrackingRange(6)
			                  .trackingTickInterval(20)
	);
	public static final EntityType<EyeOfEnderEntity> EYE_OF_ENDER = register(
			"eye_of_ender",
			EntityType.Builder.<EyeOfEnderEntity>create(EyeOfEnderEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(4)
	);
	public static final EntityType<FallingBlockEntity> FALLING_BLOCK = register(
			"falling_block",
			EntityType.Builder.<FallingBlockEntity>create(FallingBlockEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.98F, 0.98F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(20)
	);
	public static final EntityType<FireballEntity> FIREBALL = register(
			"fireball",
			EntityType.Builder.<FireballEntity>create(FireballEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.0F, 1.0F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<FireworkRocketEntity> FIREWORK_ROCKET = register(
			"firework_rocket",
			EntityType.Builder.<FireworkRocketEntity>create(FireworkRocketEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<FoxEntity> FOX = register(
			"fox",
			EntityType.Builder.create(FoxEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.6F, 0.7F)
			                  .eyeHeight(0.4F)
			                  .passengerAttachments(new Vec3d(0.0, 0.6375, -0.25))
			                  .maxTrackingRange(8)
			                  .allowSpawningInside(Blocks.SWEET_BERRY_BUSH)
	);
	public static final EntityType<FrogEntity> FROG = register(
			"frog",
			EntityType.Builder.create(FrogEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.5F, 0.5F)
			                  .passengerAttachments(new Vec3d(0.0, 0.375, -0.25))
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<FurnaceMinecartEntity> FURNACE_MINECART = register(
			"furnace_minecart",
			EntityType.Builder.create(FurnaceMinecartEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.98F, 0.7F)
			                  .passengerAttachments(0.1875F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<GhastEntity> GHAST = register(
			"ghast",
			EntityType.Builder.create(GhastEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .dimensions(4.0F, 4.0F)
			                  .eyeHeight(2.6F)
			                  .passengerAttachments(4.0625F)
			                  .vehicleAttachment(0.5F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<HappyGhastEntity> HAPPY_GHAST = register(
			"happy_ghast",
			EntityType.Builder.create(HappyGhastEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(4.0F, 4.0F)
			                  .eyeHeight(2.6F)
			                  .passengerAttachments(
					                  new Vec3d(0.0, 4.0, 1.7),
					                  new Vec3d(-1.7, 4.0, 0.0),
					                  new Vec3d(0.0, 4.0, -1.7),
					                  new Vec3d(1.7, 4.0, 0.0)
			                  )
			                  .vehicleAttachment(0.5F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<GiantEntity> GIANT = register(
			"giant",
			EntityType.Builder.create(GiantEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(3.6F, 12.0F)
			                  .eyeHeight(10.44F)
			                  .vehicleAttachment(-3.75F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<GlowItemFrameEntity> GLOW_ITEM_FRAME = register(
			"glow_item_frame",
			EntityType.Builder.<GlowItemFrameEntity>create(GlowItemFrameEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.5F, 0.5F)
			                  .eyeHeight(0.0F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(Integer.MAX_VALUE)
	);
	public static final EntityType<GlowSquidEntity> GLOW_SQUID = register(
			"glow_squid",
			EntityType.Builder
					.create(GlowSquidEntity::new, SpawnGroup.UNDERGROUND_WATER_CREATURE)
					.dimensions(0.8F, 0.8F)
					.eyeHeight(0.4F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<GoatEntity> GOAT = register(
			"goat",
			EntityType.Builder
					.create(GoatEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.9F, 1.3F)
					.passengerAttachments(1.1125F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<GuardianEntity> GUARDIAN = register(
			"guardian",
			EntityType.Builder.create(GuardianEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.85F, 0.85F)
			                  .eyeHeight(0.425F)
			                  .passengerAttachments(0.975F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<HoglinEntity> HOGLIN = register(
			"hoglin",
			EntityType.Builder
					.create(HoglinEntity::new, SpawnGroup.MONSTER)
					.dimensions(HORSE_WIDTH, 1.4F)
					.passengerAttachments(1.49375F)
					.maxTrackingRange(8)
	);
	public static final EntityType<HopperMinecartEntity> HOPPER_MINECART = register(
			"hopper_minecart",
			EntityType.Builder.create(HopperMinecartEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.98F, 0.7F)
			                  .passengerAttachments(0.1875F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<HorseEntity> HORSE = register(
			"horse",
			EntityType.Builder.create(HorseEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(HORSE_WIDTH, 1.6F)
			                  .eyeHeight(1.52F)
			                  .passengerAttachments(1.44375F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<HuskEntity> HUSK = register(
			"husk",
			EntityType.Builder.create(HuskEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .eyeHeight(1.74F)
			                  .passengerAttachments(2.075F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<IllusionerEntity> ILLUSIONER = register(
			"illusioner",
			EntityType.Builder.create(IllusionerEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .passengerAttachments(2.0F)
			                  .vehicleAttachment(-0.6F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<InteractionEntity> INTERACTION = register(
			"interaction",
			EntityType.Builder
					.create(InteractionEntity::new, SpawnGroup.MISC)
					.dropsNothing()
					.dimensions(0.0F, 0.0F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<IronGolemEntity> IRON_GOLEM = register(
			"iron_golem",
			EntityType.Builder.create(IronGolemEntity::new, SpawnGroup.MISC).dimensions(1.4F, 2.7F).maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ItemEntity> ITEM = register(
			"item",
			EntityType.Builder.<ItemEntity>create(ItemEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .eyeHeight(ItemEntity.ITEM_EYE_HEIGHT)
			                  .maxTrackingRange(6)
			                  .trackingTickInterval(20)
	);
	public static final EntityType<DisplayEntity.ItemDisplayEntity> ITEM_DISPLAY = register(
			"item_display",
			EntityType.Builder.create(DisplayEntity.ItemDisplayEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.0F, 0.0F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(1)
	);
	public static final EntityType<ItemFrameEntity> ITEM_FRAME = register(
			"item_frame",
			EntityType.Builder.<ItemFrameEntity>create(ItemFrameEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.5F, 0.5F)
			                  .eyeHeight(0.0F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(Integer.MAX_VALUE)
	);
	public static final EntityType<BoatEntity> JUNGLE_BOAT = register(
			"jungle_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.JUNGLE_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> JUNGLE_CHEST_BOAT = register(
			"jungle_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.JUNGLE_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<LeashKnotEntity> LEASH_KNOT = register(
			"leash_knot",
			EntityType.Builder.<LeashKnotEntity>create(LeashKnotEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .disableSaving()
			                  .dimensions(0.375F, 0.5F)
			                  .eyeHeight(0.0625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(Integer.MAX_VALUE)
	);
	public static final EntityType<LightningEntity> LIGHTNING_BOLT = register(
			"lightning_bolt",
			EntityType.Builder.create(LightningEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .disableSaving()
			                  .dimensions(0.0F, 0.0F)
			                  .maxTrackingRange(16)
			                  .trackingTickInterval(Integer.MAX_VALUE)
	);
	public static final EntityType<LlamaEntity> LLAMA = register(
			"llama",
			EntityType.Builder.create(LlamaEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.9F, 1.87F)
			                  .eyeHeight(1.7765F)
			                  .passengerAttachments(new Vec3d(0.0, 1.37, -0.3))
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<LlamaSpitEntity> LLAMA_SPIT = register(
			"llama_spit",
			EntityType.Builder.<LlamaSpitEntity>create(LlamaSpitEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<MagmaCubeEntity> MAGMA_CUBE = register(
			"magma_cube",
			EntityType.Builder.create(MagmaCubeEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .dimensions(0.52F, 0.52F)
			                  .eyeHeight(0.325F)
			                  .spawnBoxScale(4.0F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<BoatEntity> MANGROVE_BOAT = register(
			"mangrove_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.MANGROVE_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> MANGROVE_CHEST_BOAT = register(
			"mangrove_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.MANGROVE_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<MannequinEntity> MANNEQUIN = register(
			"mannequin",
			EntityType.Builder.create(MannequinEntity::create, SpawnGroup.MISC)
			                  .dimensions(0.6F, 1.8F)
			                  .eyeHeight(1.62F)
			                  .vehicleAttachment(PlayerLikeEntity.VEHICLE_ATTACHMENT)
			                  .maxTrackingRange(32)
			                  .trackingTickInterval(2)
	);
	public static final EntityType<MarkerEntity> MARKER = register(
			"marker",
			EntityType.Builder
					.create(MarkerEntity::new, SpawnGroup.MISC)
					.dropsNothing()
					.dimensions(0.0F, 0.0F)
					.maxTrackingRange(0)
	);
	public static final EntityType<MinecartEntity> MINECART = register(
			"minecart",
			EntityType.Builder
					.create(MinecartEntity::new, SpawnGroup.MISC)
					.dropsNothing()
					.dimensions(0.98F, 0.7F)
					.passengerAttachments(0.1875F)
					.maxTrackingRange(8)
	);
	public static final EntityType<MooshroomEntity> MOOSHROOM = register(
			"mooshroom",
			EntityType.Builder.create(MooshroomEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.9F, 1.4F)
			                  .eyeHeight(1.3F)
			                  .passengerAttachments(1.36875F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<MuleEntity> MULE = register(
			"mule",
			EntityType.Builder.create(MuleEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(HORSE_WIDTH, 1.6F)
			                  .eyeHeight(1.52F)
			                  .passengerAttachments(1.2125F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<NautilusEntity> NAUTILUS = register(
			"nautilus",
			EntityType.Builder.create(NautilusEntity::new, SpawnGroup.WATER_CREATURE)
			                  .dimensions(0.875F, 0.95F)
			                  .passengerAttachments(1.1375F)
			                  .eyeHeight(0.2751F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<BoatEntity> OAK_BOAT = register(
			"oak_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.OAK_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> OAK_CHEST_BOAT = register(
			"oak_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.OAK_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<OcelotEntity> OCELOT = register(
			"ocelot",
			EntityType.Builder
					.create(OcelotEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.6F, 0.7F)
					.passengerAttachments(0.6375F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<OminousItemSpawnerEntity> OMINOUS_ITEM_SPAWNER = register(
			"ominous_item_spawner",
			EntityType.Builder
					.create(OminousItemSpawnerEntity::new, SpawnGroup.MISC)
					.dropsNothing()
					.dimensions(0.25F, 0.25F)
					.maxTrackingRange(8)
	);
	public static final EntityType<PaintingEntity> PAINTING = register(
			"painting",
			EntityType.Builder.<PaintingEntity>create(PaintingEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.5F, 0.5F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(Integer.MAX_VALUE)
	);
	public static final EntityType<BoatEntity> PALE_OAK_BOAT = register(
			"pale_oak_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.PALE_OAK_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> PALE_OAK_CHEST_BOAT = register(
			"pale_oak_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.PALE_OAK_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<PandaEntity> PANDA = register(
			"panda",
			EntityType.Builder
					.create(PandaEntity::new, SpawnGroup.CREATURE)
					.dimensions(1.3F, 1.25F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ParchedEntity> PARCHED = register(
			"parched",
			EntityType.Builder.create(ParchedEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.99F)
			                  .eyeHeight(1.74F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<ParrotEntity> PARROT = register(
			"parrot",
			EntityType.Builder.create(ParrotEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.5F, 0.9F)
			                  .eyeHeight(0.54F)
			                  .passengerAttachments(0.4625F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<PhantomEntity> PHANTOM = register(
			"phantom",
			EntityType.Builder.create(PhantomEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.9F, 0.5F)
			                  .eyeHeight(0.175F)
			                  .passengerAttachments(0.3375F)
			                  .vehicleAttachment(-0.125F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<PigEntity> PIG = register(
			"pig",
			EntityType.Builder
					.create(PigEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.9F, 0.9F)
					.passengerAttachments(0.86875F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<PiglinEntity> PIGLIN = register(
			"piglin",
			EntityType.Builder.create(PiglinEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .eyeHeight(1.79F)
			                  .passengerAttachments(2.0125F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<PiglinBruteEntity> PIGLIN_BRUTE = register(
			"piglin_brute",
			EntityType.Builder.create(PiglinBruteEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .eyeHeight(1.79F)
			                  .passengerAttachments(2.0125F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<PillagerEntity> PILLAGER = register(
			"pillager",
			EntityType.Builder.create(PillagerEntity::new, SpawnGroup.MONSTER)
			                  .spawnableFarFromPlayer()
			                  .dimensions(0.6F, 1.95F)
			                  .passengerAttachments(2.0F)
			                  .vehicleAttachment(-0.6F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<PolarBearEntity> POLAR_BEAR = register(
			"polar_bear",
			EntityType.Builder
					.create(PolarBearEntity::new, SpawnGroup.CREATURE)
					.allowSpawningInside(Blocks.POWDER_SNOW)
					.dimensions(1.4F, 1.4F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<SplashPotionEntity> SPLASH_POTION = register(
			"splash_potion",
			EntityType.Builder.<SplashPotionEntity>create(SplashPotionEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<LingeringPotionEntity> LINGERING_POTION = register(
			"lingering_potion",
			EntityType.Builder.<LingeringPotionEntity>create(LingeringPotionEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<PufferfishEntity> PUFFERFISH = register(
			"pufferfish",
			EntityType.Builder
					.create(PufferfishEntity::new, SpawnGroup.WATER_AMBIENT)
					.dimensions(0.7F, 0.7F)
					.eyeHeight(0.455F)
					.maxTrackingRange(4)
	);
	public static final EntityType<RabbitEntity> RABBIT = register(
			"rabbit",
			EntityType.Builder.create(RabbitEntity::new, SpawnGroup.CREATURE).dimensions(0.4F, 0.5F).maxTrackingRange(8)
	);
	public static final EntityType<RavagerEntity> RAVAGER = register(
			"ravager",
			EntityType.Builder.create(RavagerEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(1.95F, 2.2F)
			                  .passengerAttachments(new Vec3d(0.0, 2.2625, -0.0625))
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<SalmonEntity> SALMON = register(
			"salmon",
			EntityType.Builder
					.create(SalmonEntity::new, SpawnGroup.WATER_AMBIENT)
					.dimensions(0.7F, 0.4F)
					.eyeHeight(0.26F)
					.maxTrackingRange(4)
	);
	public static final EntityType<SheepEntity> SHEEP = register(
			"sheep",
			EntityType.Builder.create(SheepEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.9F, 1.3F)
			                  .eyeHeight(1.235F)
			                  .passengerAttachments(1.2375F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ShulkerEntity> SHULKER = register(
			"shulker",
			EntityType.Builder.create(ShulkerEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .spawnableFarFromPlayer()
			                  .dimensions(1.0F, 1.0F)
			                  .eyeHeight(0.5F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ShulkerBulletEntity> SHULKER_BULLET = register(
			"shulker_bullet",
			EntityType.Builder
					.<ShulkerBulletEntity>create(ShulkerBulletEntity::new, SpawnGroup.MISC)
					.dropsNothing()
					.dimensions(0.3125F, 0.3125F)
					.maxTrackingRange(8)
	);
	public static final EntityType<SilverfishEntity> SILVERFISH = register(
			"silverfish",
			EntityType.Builder.create(SilverfishEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.4F, 0.3F)
			                  .eyeHeight(0.13F)
			                  .passengerAttachments(0.2375F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<SkeletonEntity> SKELETON = register(
			"skeleton",
			EntityType.Builder.create(SkeletonEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.99F)
			                  .eyeHeight(1.74F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<SkeletonHorseEntity> SKELETON_HORSE = register(
			"skeleton_horse",
			EntityType.Builder.create(SkeletonHorseEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(HORSE_WIDTH, 1.6F)
			                  .eyeHeight(1.52F)
			                  .passengerAttachments(1.31875F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<SlimeEntity> SLIME = register(
			"slime",
			EntityType.Builder.create(SlimeEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.52F, 0.52F)
			                  .eyeHeight(0.325F)
			                  .spawnBoxScale(4.0F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<SmallFireballEntity> SMALL_FIREBALL = register(
			"small_fireball",
			EntityType.Builder.<SmallFireballEntity>create(SmallFireballEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.3125F, 0.3125F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<SnifferEntity> SNIFFER = register(
			"sniffer",
			EntityType.Builder.create(SnifferEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(1.9F, 1.75F)
			                  .eyeHeight(1.05F)
			                  .passengerAttachments(2.09375F)
			                  .nameTagAttachment(2.05F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<SnowballEntity> SNOWBALL = register(
			"snowball",
			EntityType.Builder.<SnowballEntity>create(SnowballEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<SnowGolemEntity> SNOW_GOLEM = register(
			"snow_golem",
			EntityType.Builder.create(SnowGolemEntity::new, SpawnGroup.MISC)
			                  .allowSpawningInside(Blocks.POWDER_SNOW)
			                  .dimensions(0.7F, 1.9F)
			                  .eyeHeight(1.7F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<SpawnerMinecartEntity> SPAWNER_MINECART = register(
			"spawner_minecart",
			EntityType.Builder.create(SpawnerMinecartEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.98F, 0.7F)
			                  .passengerAttachments(0.1875F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<SpectralArrowEntity> SPECTRAL_ARROW = register(
			"spectral_arrow",
			EntityType.Builder.<SpectralArrowEntity>create(SpectralArrowEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.5F, 0.5F)
			                  .eyeHeight(0.13F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(20)
	);
	public static final EntityType<SpiderEntity> SPIDER = register(
			"spider",
			EntityType.Builder.create(SpiderEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(1.4F, 0.9F)
			                  .eyeHeight(0.65F)
			                  .passengerAttachments(0.765F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<BoatEntity> SPRUCE_BOAT = register(
			"spruce_boat",
			EntityType.Builder.create(getBoatFactory(() -> Items.SPRUCE_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ChestBoatEntity> SPRUCE_CHEST_BOAT = register(
			"spruce_chest_boat",
			EntityType.Builder.create(getChestBoatFactory(() -> Items.SPRUCE_CHEST_BOAT), SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(1.375F, 0.5625F)
			                  .eyeHeight(0.5625F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<SquidEntity> SQUID = register(
			"squid",
			EntityType.Builder
					.create(SquidEntity::new, SpawnGroup.WATER_CREATURE)
					.dimensions(0.8F, 0.8F)
					.eyeHeight(0.4F)
					.maxTrackingRange(8)
	);
	public static final EntityType<StrayEntity> STRAY = register(
			"stray",
			EntityType.Builder.create(StrayEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.99F)
			                  .eyeHeight(1.74F)
			                  .vehicleAttachment(-0.7F)
			                  .allowSpawningInside(Blocks.POWDER_SNOW)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<StriderEntity> STRIDER = register(
			"strider",
			EntityType.Builder
					.create(StriderEntity::new, SpawnGroup.CREATURE)
					.makeFireImmune()
					.dimensions(0.9F, 1.7F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<TadpoleEntity> TADPOLE = register(
			"tadpole",
			EntityType.Builder
					.create(TadpoleEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.4F, 0.3F)
					.eyeHeight(0.19500001F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<DisplayEntity.TextDisplayEntity> TEXT_DISPLAY = register(
			"text_display",
			EntityType.Builder.create(DisplayEntity.TextDisplayEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.0F, 0.0F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(1)
	);
	public static final EntityType<TntEntity> TNT = register(
			"tnt",
			EntityType.Builder.<TntEntity>create(TntEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .makeFireImmune()
			                  .dimensions(0.98F, 0.98F)
			                  .eyeHeight(0.15F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<TntMinecartEntity> TNT_MINECART = register(
			"tnt_minecart",
			EntityType.Builder.create(TntMinecartEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.98F, 0.7F)
			                  .passengerAttachments(0.1875F)
			                  .maxTrackingRange(8)
	);
	public static final EntityType<TraderLlamaEntity> TRADER_LLAMA = register(
			"trader_llama",
			EntityType.Builder.create(TraderLlamaEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.9F, 1.87F)
			                  .eyeHeight(1.7765F)
			                  .passengerAttachments(new Vec3d(0.0, 1.37, -0.3))
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<TridentEntity> TRIDENT = register(
			"trident",
			EntityType.Builder.<TridentEntity>create(TridentEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.5F, 0.5F)
			                  .eyeHeight(0.13F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(20)
	);
	public static final EntityType<TropicalFishEntity> TROPICAL_FISH = register(
			"tropical_fish",
			EntityType.Builder
					.create(TropicalFishEntity::new, SpawnGroup.WATER_AMBIENT)
					.dimensions(0.5F, 0.4F)
					.eyeHeight(0.26F)
					.maxTrackingRange(4)
	);
	public static final EntityType<TurtleEntity> TURTLE = register(
			"turtle",
			EntityType.Builder.create(TurtleEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(1.2F, 0.4F)
			                  .passengerAttachments(new Vec3d(0.0, 0.55625, -0.25))
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<VexEntity> VEX = register(
			"vex",
			EntityType.Builder.create(VexEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .dimensions(0.4F, 0.8F)
			                  .eyeHeight(0.51875F)
			                  .passengerAttachments(0.7375F)
			                  .vehicleAttachment(0.04F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<VillagerEntity> VILLAGER = register(
			"villager",
			EntityType.Builder
					.<VillagerEntity>create(VillagerEntity::new, SpawnGroup.MISC)
					.dimensions(0.6F, 1.95F)
					.eyeHeight(1.62F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<VindicatorEntity> VINDICATOR = register(
			"vindicator",
			EntityType.Builder.create(VindicatorEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .passengerAttachments(2.0F)
			                  .vehicleAttachment(-0.6F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<WanderingTraderEntity> WANDERING_TRADER = register(
			"wandering_trader",
			EntityType.Builder
					.create(WanderingTraderEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.6F, 1.95F)
					.eyeHeight(1.62F)
					.maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<WardenEntity> WARDEN = register(
			"warden",
			EntityType.Builder.create(WardenEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.9F, 2.9F)
			                  .passengerAttachments(3.15F)
			                  .attachment(EntityAttachmentType.WARDEN_CHEST, 0.0F, 1.6F, 0.0F)
			                  .maxTrackingRange(16)
			                  .makeFireImmune()
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<WindChargeEntity> WIND_CHARGE = register(
			"wind_charge",
			EntityType.Builder.<WindChargeEntity>create(WindChargeEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.3125F, 0.3125F)
			                  .eyeHeight(0.0F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<WitchEntity> WITCH = register(
			"witch",
			EntityType.Builder.create(WitchEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .eyeHeight(1.62F)
			                  .passengerAttachments(2.2625F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<WitherEntity> WITHER = register(
			"wither",
			EntityType.Builder.create(WitherEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .allowSpawningInside(Blocks.WITHER_ROSE)
			                  .dimensions(0.9F, 3.5F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<WitherSkeletonEntity> WITHER_SKELETON = register(
			"wither_skeleton",
			EntityType.Builder.create(WitherSkeletonEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .allowSpawningInside(Blocks.WITHER_ROSE)
			                  .dimensions(0.7F, 2.4F)
			                  .eyeHeight(2.1F)
			                  .vehicleAttachment(-0.875F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<WitherSkullEntity> WITHER_SKULL = register(
			"wither_skull",
			EntityType.Builder.<WitherSkullEntity>create(WitherSkullEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .dimensions(0.3125F, 0.3125F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<WolfEntity> WOLF = register(
			"wolf",
			EntityType.Builder.create(WolfEntity::new, SpawnGroup.CREATURE)
			                  .dimensions(0.6F, 0.85F)
			                  .eyeHeight(0.68F)
			                  .passengerAttachments(new Vec3d(0.0, 0.81875, -0.0625))
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ZoglinEntity> ZOGLIN = register(
			"zoglin",
			EntityType.Builder.create(ZoglinEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .dimensions(HORSE_WIDTH, 1.4F)
			                  .passengerAttachments(1.49375F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<ZombieEntity> ZOMBIE = register(
			"zombie",
			EntityType.Builder.<ZombieEntity>create(ZombieEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .eyeHeight(1.74F)
			                  .passengerAttachments(2.0125F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<ZombieHorseEntity> ZOMBIE_HORSE = register(
			"zombie_horse",
			EntityType.Builder.create(ZombieHorseEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(HORSE_WIDTH, 1.6F)
			                  .eyeHeight(1.52F)
			                  .passengerAttachments(1.31875F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ZombieNautilusEntity> ZOMBIE_NAUTILUS = register(
			"zombie_nautilus",
			EntityType.Builder.create(ZombieNautilusEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.875F, 0.95F)
			                  .passengerAttachments(1.1375F)
			                  .eyeHeight(0.2751F)
			                  .maxTrackingRange(DEFAULT_TRACKING_RANGE)
	);
	public static final EntityType<ZombieVillagerEntity> ZOMBIE_VILLAGER = register(
			"zombie_villager",
			EntityType.Builder.create(ZombieVillagerEntity::new, SpawnGroup.MONSTER)
			                  .dimensions(0.6F, 1.95F)
			                  .passengerAttachments(2.125F)
			                  .vehicleAttachment(-0.7F)
			                  .eyeHeight(1.74F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<ZombifiedPiglinEntity> ZOMBIFIED_PIGLIN = register(
			"zombified_piglin",
			EntityType.Builder.create(ZombifiedPiglinEntity::new, SpawnGroup.MONSTER)
			                  .makeFireImmune()
			                  .dimensions(0.6F, 1.95F)
			                  .eyeHeight(1.79F)
			                  .passengerAttachments(2.0F)
			                  .vehicleAttachment(-0.7F)
			                  .maxTrackingRange(8)
			                  .notAllowedInPeaceful()
	);
	public static final EntityType<PlayerEntity> PLAYER = register(
			"player",
			EntityType.Builder.<PlayerEntity>create(SpawnGroup.MISC)
			                  .disableSaving()
			                  .disableSummon()
			                  .dimensions(0.6F, 1.8F)
			                  .eyeHeight(1.62F)
			                  .vehicleAttachment(PlayerLikeEntity.VEHICLE_ATTACHMENT)
			                  .maxTrackingRange(32)
			                  .trackingTickInterval(2)
	);
	public static final EntityType<FishingBobberEntity> FISHING_BOBBER = register(
			"fishing_bobber",
			EntityType.Builder.<FishingBobberEntity>create(FishingBobberEntity::new, SpawnGroup.MISC)
			                  .dropsNothing()
			                  .disableSaving()
			                  .disableSummon()
			                  .dimensions(0.25F, 0.25F)
			                  .maxTrackingRange(4)
			                  .trackingTickInterval(5)
	);
	private static final Set<EntityType<?>>
			POTENTIALLY_EXECUTES_COMMANDS =
			Set.of(FALLING_BLOCK, COMMAND_BLOCK_MINECART, SPAWNER_MINECART);
	private final EntityType.EntityFactory<T> factory;
	private final SpawnGroup spawnGroup;
	private final ImmutableSet<Block> canSpawnInside;
	private final boolean saveable;
	private final boolean summonable;
	private final boolean fireImmune;
	private final boolean spawnableFarFromPlayer;
	private final int maxTrackDistance;
	private final int trackTickInterval;
	private final String translationKey;
	private @Nullable Text name;
	private final Optional<RegistryKey<LootTable>> lootTableKey;
	private final EntityDimensions dimensions;
	private final float spawnBoxScale;
	private final FeatureSet requiredFeatures;
	private final boolean allowedInPeaceful;

	private static <T extends Entity> EntityType<T> register(
			RegistryKey<EntityType<?>> key,
			EntityType.Builder<T> type
	) {
		return Registry.register(Registries.ENTITY_TYPE, key, type.build(key));
	}

	private static RegistryKey<EntityType<?>> keyOf(String id) {
		return RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.ofVanilla(id));
	}

	private static <T extends Entity> EntityType<T> register(String id, EntityType.Builder<T> type) {
		return register(keyOf(id), type);
	}

	public static Identifier getId(EntityType<?> type) {
		return Registries.ENTITY_TYPE.getId(type);
	}

	public static Optional<EntityType<?>> get(String id) {
		return Registries.ENTITY_TYPE.getOptionalValue(Identifier.tryParse(id));
	}

	public EntityType(
		EntityFactory<T> factory,
		SpawnGroup spawnGroup,
		boolean saveable,
		boolean summonable,
		boolean fireImmune,
		boolean spawnableFarFromPlayer,
		ImmutableSet<Block> canSpawnInside,
		EntityDimensions dimensions,
		float spawnBoxScale,
		int maxTrackDistance,
		int trackTickInterval,
		String translationKey,
		Optional<RegistryKey<LootTable>> lootTable,
		FeatureSet requiredFeatures,
		boolean allowedInPeaceful
	) {
		this.factory = factory;
		this.spawnGroup = spawnGroup;
		this.spawnableFarFromPlayer = spawnableFarFromPlayer;
		this.saveable = saveable;
		this.summonable = summonable;
		this.fireImmune = fireImmune;
		this.canSpawnInside = canSpawnInside;
		this.dimensions = dimensions;
		this.spawnBoxScale = spawnBoxScale;
		this.maxTrackDistance = maxTrackDistance;
		this.trackTickInterval = trackTickInterval;
		this.translationKey = translationKey;
		this.lootTableKey = lootTable;
		this.requiredFeatures = requiredFeatures;
		this.allowedInPeaceful = allowedInPeaceful;
	}

	/**
	 * Спаунит сущность из предмета (например, яйца призыва), применяя данные из NBT стека.
	 * Если стек не null, копирует компоненты и NBT-данные на созданную сущность.
	 *
	 * @param alignPosition выровнять позицию по центру блока
	 * @param invertY       инвертировать смещение по Y (для спауна снизу вверх)
	 */
	public @Nullable T spawnFromItemStack(
		ServerWorld world,
		@Nullable ItemStack stack,
		@Nullable LivingEntity spawner,
		BlockPos pos,
		SpawnReason spawnReason,
		boolean alignPosition,
		boolean invertY
	) {
		Consumer<T> consumer = stack != null ? copier(world, stack, spawner) : entity -> {};
		return spawn(world, consumer, pos, spawnReason, alignPosition, invertY);
	}

	public static <T extends Entity> Consumer<T> copier(World world, ItemStack stack, @Nullable LivingEntity spawner) {
		return copier(entity -> {}, world, stack, spawner);
	}

	public static <T extends Entity> Consumer<T> copier(
			Consumer<T> chained,
			World world,
			ItemStack stack,
			@Nullable LivingEntity spawner
	) {
		return nbtCopier(componentsCopier(chained, stack), world, stack, spawner);
	}

	public static <T extends Entity> Consumer<T> componentsCopier(Consumer<T> chained, ItemStack stack) {
		return chained.andThen(entity -> entity.copyComponentsFrom(stack));
	}

	public static <T extends Entity> Consumer<T> nbtCopier(
			Consumer<T> chained,
			World world,
			ItemStack stack,
			@Nullable LivingEntity spawner
	) {
		TypedEntityData<EntityType<?>> typedEntityData = stack.get(DataComponentTypes.ENTITY_DATA);
		return typedEntityData != null ? chained.andThen(entity -> loadFromEntityNbt(
				world,
				spawner,
				entity,
				typedEntityData
		)) : chained;
	}

	public @Nullable T spawn(ServerWorld world, BlockPos pos, SpawnReason reason) {
		return spawn(world, null, pos, reason, false, false);
	}

	public @Nullable T spawn(
			ServerWorld world,
			@Nullable Consumer<T> afterConsumer,
			BlockPos pos,
			SpawnReason reason,
			boolean alignPosition,
			boolean invertY
	) {
		T entity = create(world, afterConsumer, pos, reason, alignPosition, invertY);

		if (entity != null) {
			world.spawnEntityAndPassengers(entity);

			if (entity instanceof MobEntity mobEntity) {
				mobEntity.playAmbientSound();
			}
		}

		return entity;
	}

	public @Nullable T create(
		ServerWorld world,
		@Nullable Consumer<T> afterConsumer,
		BlockPos pos,
		SpawnReason reason,
		boolean alignPosition,
		boolean invertY
	) {
		T entity = create(world, reason);
		if (entity == null) {
			return null;
		}

		double originYOffset = 0.0;
		if (alignPosition) {
			entity.setPosition(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
			originYOffset = getOriginY(world, pos, invertY, entity.getBoundingBox());
		}

		entity.refreshPositionAndAngles(
			pos.getX() + 0.5,
			pos.getY() + originYOffset,
			pos.getZ() + 0.5,
			MathHelper.wrapDegrees(world.random.nextFloat() * 360.0F),
			0.0F
		);

		if (entity instanceof MobEntity mobEntity) {
			mobEntity.headYaw = mobEntity.getYaw();
			mobEntity.bodyYaw = mobEntity.getYaw();
			mobEntity.initialize(world, world.getLocalDifficulty(mobEntity.getBlockPos()), reason, null);
		}

		if (afterConsumer != null) {
			afterConsumer.accept(entity);
		}

		return entity;
	}

	/**
	 * Вычисляет смещение по Y для корректного размещения сущности на поверхности блока.
	 * При {@code invertY=true} ищет поверхность снизу (для подвешенных сущностей).
	 */
	protected static double getOriginY(WorldView world, BlockPos pos, boolean invertY, Box boundingBox) {
		Box box = new Box(pos);
		if (invertY) {
			box = box.stretch(0.0, -1.0, 0.0);
		}

		Iterable<VoxelShape> collisions = world.getCollisions(null, box);
		return 1.0 + VoxelShapes.calculateMaxOffset(Direction.Axis.Y, boundingBox, collisions, invertY ? -2.0 : -1.0);
	}

	/**
	 * Применяет NBT-данные из компонента предмета к сущности с проверкой безопасности.
	 * На сервере запрещает применение данных для типов, способных выполнять команды,
	 * если спаунер не является оператором — защита от эксплойтов через яйца призыва.
	 */
	public static void loadFromEntityNbt(
		World world,
		@Nullable LivingEntity spawner,
		@Nullable Entity entity,
		TypedEntityData<EntityType<?>> data
	) {
		MinecraftServer server = world.getServer();
		if (server == null || entity == null) {
			return;
		}

		if (entity.getType() != data.getType()) {
			return;
		}

		boolean isClientOrSafe = world.isClient()
			|| !entity.getType().canPotentiallyExecuteCommands()
			|| (spawner instanceof PlayerEntity player && server.getPlayerManager().isOperator(player.getPlayerConfigEntry()));
		if (isClientOrSafe) {
			data.applyToEntity(entity);
		}
	}

	public boolean isSaveable() {
		return this.saveable;
	}

	public boolean isSummonable() {
		return this.summonable;
	}

	public boolean isFireImmune() {
		return this.fireImmune;
	}

	public boolean isSpawnableFarFromPlayer() {
		return this.spawnableFarFromPlayer;
	}

	public SpawnGroup getSpawnGroup() {
		return this.spawnGroup;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}

	public Text getName() {
		if (name == null) {
			name = Text.translatable(getTranslationKey());
		}

		return name;
	}

	@Override
	public String toString() {
		return this.getTranslationKey();
	}

	public String getUntranslatedName() {
		String key = getTranslationKey();
		int dotIndex = key.lastIndexOf('.');
		return dotIndex == -1 ? key : key.substring(dotIndex + 1);
	}

	public Optional<RegistryKey<LootTable>> getLootTableKey() {
		return this.lootTableKey;
	}

	public float getWidth() {
		return this.dimensions.width();
	}

	public float getHeight() {
		return this.dimensions.height();
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return this.requiredFeatures;
	}

	public @Nullable T create(World world, SpawnReason reason) {
		return isEnabled(world.getEnabledFeatures()) ? factory.create(this, world) : null;
	}

	public static Optional<Entity> getEntityFromData(ReadView view, World world, SpawnReason reason) {
		return Util.ifPresentOrElse(
				fromData(view).map(type -> type.create(world, reason)),
				entity -> entity.readData(view),
				() -> LOGGER.warn("Skipping Entity with id {}", view.getString("id", "[invalid]"))
		);
	}

	public static Optional<Entity> getEntityFromData(
			EntityType<?> type,
			ReadView readView,
			World world,
			SpawnReason spawnReason
	) {
		Optional<Entity> entity = Optional.ofNullable(type.create(world, spawnReason));
		entity.ifPresent(e -> e.readData(readView));
		return entity;
	}

	public Box getSpawnBox(double x, double y, double z) {
		float halfWidth = spawnBoxScale * getWidth() / 2.0F;
		float scaledHeight = spawnBoxScale * getHeight();
		return new Box(
			x - halfWidth, y, z - halfWidth,
			x + halfWidth, y + scaledHeight, z + halfWidth
		);
	}

	/**
	 * Проверяет, является ли блок недопустимым для спауна этой сущности.
	 * Учитывает список разрешённых блоков ({@code canSpawnInside}), иммунитет к огню
	 * и хардкодированный список опасных блоков (кактус, порошковый снег и т.д.).
	 */
	public boolean isInvalidSpawn(BlockState state) {
		if (canSpawnInside.contains(state.getBlock())) {
			return false;
		}

		if (!fireImmune && PathNodeMaker.isFireDamaging(state)) {
			return true;
		}

		return state.isOf(Blocks.WITHER_ROSE)
			|| state.isOf(Blocks.SWEET_BERRY_BUSH)
			|| state.isOf(Blocks.CACTUS)
			|| state.isOf(Blocks.POWDER_SNOW);
	}

	public EntityDimensions getDimensions() {
		return this.dimensions;
	}

	public static Optional<EntityType<?>> fromData(ReadView view) {
		return view.read("id", CODEC);
	}

	public static @Nullable Entity loadEntityWithPassengers(
		NbtCompound nbt,
		World world,
		SpawnReason reason,
		LoadedEntityProcessor processor
	) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
			return loadEntityWithPassengers(
				NbtReadView.create(logging, world.getRegistryManager(), nbt),
				world,
				reason,
				processor
			);
		}
	}

	public static @Nullable Entity loadEntityWithPassengers(
		EntityType<?> type, NbtCompound data, World world, SpawnReason reason, LoadedEntityProcessor processor
	) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(LOGGER)) {
			return loadEntityWithPassengers(
				type,
				NbtReadView.create(logging, world.getRegistryManager(), data),
				world,
				reason,
				processor
			);
		}
	}

	public static @Nullable Entity loadEntityWithPassengers(
		ReadView view,
		World world,
		SpawnReason reason,
		LoadedEntityProcessor processor
	) {
		return loadEntityFromData(view, world, reason)
			.map(processor::process)
			.map(entity -> loadEntityPassengers(entity, view, world, reason, processor))
			.orElse(null);
	}

	public static @Nullable Entity loadEntityWithPassengers(
		EntityType<?> type,
		ReadView view,
		World world,
		SpawnReason reason,
		LoadedEntityProcessor processor
	) {
		return loadEntityFromData(type, view, world, reason)
			.map(processor::process)
			.map(entity -> loadEntityPassengers(entity, view, world, reason, processor))
			.orElse(null);
	}

	private static Entity loadEntityPassengers(
		Entity entity,
		ReadView view,
		World world,
		SpawnReason reason,
		LoadedEntityProcessor processor
	) {
		for (ReadView passengerView : view.getListReadView("Passengers")) {
			Entity passenger = loadEntityWithPassengers(passengerView, world, reason, processor);
			if (passenger != null) {
				passenger.startRiding(entity, true, false);
			}
		}

		return entity;
	}

	public static Stream<Entity> streamFromData(ReadView.ListReadView view, World world, SpawnReason reason) {
		return view.stream().mapMulti((viewx, callback) -> loadEntityWithPassengers(
				viewx, world, reason, entity -> {
					callback.accept(entity);
					return entity;
				}
		));
	}

	private static Optional<Entity> loadEntityFromData(ReadView view, World world, SpawnReason reason) {
		try {
			return getEntityFromData(view, world, reason);
		}
		catch (RuntimeException exception) {
			LOGGER.warn("Exception loading entity: ", exception);
			return Optional.empty();
		}
	}

	private static Optional<Entity> loadEntityFromData(
		EntityType<?> type,
		ReadView view,
		World world,
		SpawnReason reason
	) {
		try {
			return getEntityFromData(type, view, world, reason);
		}
		catch (RuntimeException exception) {
			LOGGER.warn("Exception loading entity: ", exception);
			return Optional.empty();
		}
	}

	public int getMaxTrackDistance() {
		return this.maxTrackDistance;
	}

	public int getTrackTickInterval() {
		return this.trackTickInterval;
	}

	/**
	 * Определяет, нужно ли всегда отправлять пакет обновления скорости клиенту.
	 * Для ряда специфических типов (игрок, рамки, кристаллы и т.д.) обновление
	 * происходит только при изменении — они исключены из этого списка.
	 */
	public boolean alwaysUpdateVelocity() {
		return this != PLAYER
				&& this != LLAMA_SPIT
				&& this != WITHER
				&& this != BAT
				&& this != ITEM_FRAME
				&& this != GLOW_ITEM_FRAME
				&& this != LEASH_KNOT
				&& this != PAINTING
				&& this != END_CRYSTAL
				&& this != EVOKER_FANGS;
	}

	public boolean isIn(TagKey<EntityType<?>> tag) {
		return this.registryEntry.isIn(tag);
	}

	public boolean isIn(RegistryEntryList<EntityType<?>> entityTypeEntryList) {
		return entityTypeEntryList.contains(this.registryEntry);
	}

	@SuppressWarnings("unchecked")
	public @Nullable T downcast(Entity entity) {
		return entity.getType() == this ? (T) entity : null;
	}

	@Override
	public Class<? extends Entity> getBaseClass() {
		return Entity.class;
	}

	@Deprecated
	public RegistryEntry.Reference<EntityType<?>> getRegistryEntry() {
		return registryEntry;
	}

	public boolean isAllowedInPeaceful() {
		return this.allowedInPeaceful;
	}

	private static EntityFactory<BoatEntity> getBoatFactory(Supplier<Item> itemSupplier) {
		return (type, world) -> new BoatEntity(type, world, itemSupplier);
	}

	private static EntityFactory<ChestBoatEntity> getChestBoatFactory(Supplier<Item> itemSupplier) {
		return (type, world) -> new ChestBoatEntity(type, world, itemSupplier);
	}

	private static EntityFactory<RaftEntity> getRaftFactory(Supplier<Item> itemSupplier) {
		return (type, world) -> new RaftEntity(type, world, itemSupplier);
	}

	private static EntityFactory<ChestRaftEntity> getChestRaftFactory(Supplier<Item> itemSupplier) {
		return (type, world) -> new ChestRaftEntity(type, world, itemSupplier);
	}

	public boolean canPotentiallyExecuteCommands() {
		return POTENTIALLY_EXECUTES_COMMANDS.contains(this);
	}

	/**
	 * Строитель для конфигурирования и создания экземпляра {@link EntityType}.
	 * Используется при регистрации типов сущностей в статическом блоке инициализации.
	 */
	public static class Builder<T extends Entity> implements net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType.Builder<T> {

		private final EntityType.EntityFactory<T> factory;
		private final SpawnGroup spawnGroup;
		private ImmutableSet<Block> canSpawnInside = ImmutableSet.of();
		private boolean saveable = true;
		private boolean summonable = true;
		private boolean fireImmune;
		private boolean spawnableFarFromPlayer;
		private int maxTrackingRange = 5;
		private int trackingTickInterval = 3;
		private EntityDimensions dimensions = EntityDimensions.changing(0.6F, 1.8F);
		private float spawnBoxScale = 1.0F;
		private EntityAttachments.Builder attachments = EntityAttachments.builder();
		private FeatureSet requiredFeatures = FeatureFlags.VANILLA_FEATURES;
		private RegistryKeyedValue<EntityType<?>, Optional<RegistryKey<LootTable>>>
				lootTable =
				registryKey -> Optional.of(
						RegistryKey.of(RegistryKeys.LOOT_TABLE, registryKey.getValue().withPrefixedPath("entities/"))
				);
		private final RegistryKeyedValue<EntityType<?>, String>
				translationKey =
				registryKey -> Util.createTranslationKey("entity", registryKey.getValue());
		private boolean allowedInPeaceful = true;

		private Builder(EntityFactory<T> factory, SpawnGroup spawnGroup) {
			this.factory = factory;
			this.spawnGroup = spawnGroup;
			spawnableFarFromPlayer = spawnGroup == SpawnGroup.CREATURE || spawnGroup == SpawnGroup.MISC;
		}

		public static <T extends Entity> Builder<T> create(EntityFactory<T> factory, SpawnGroup spawnGroup) {
			return new Builder<>(factory, spawnGroup);
		}

		public static <T extends Entity> Builder<T> create(SpawnGroup spawnGroup) {
			return new Builder<>((type, world) -> null, spawnGroup);
		}

		public Builder<T> dimensions(float width, float height) {
			dimensions = EntityDimensions.changing(width, height);
			return this;
		}

		public Builder<T> spawnBoxScale(float spawnBoxScale) {
			this.spawnBoxScale = spawnBoxScale;
			return this;
		}

		public Builder<T> eyeHeight(float eyeHeight) {
			dimensions = dimensions.withEyeHeight(eyeHeight);
			return this;
		}

		public Builder<T> passengerAttachments(float... offsetYs) {
			for (float offsetY : offsetYs) {
				attachments = attachments.add(EntityAttachmentType.PASSENGER, 0.0F, offsetY, 0.0F);
			}

			return this;
		}

		public Builder<T> passengerAttachments(Vec3d... passengerAttachments) {
			for (Vec3d attachment : passengerAttachments) {
				attachments = attachments.add(EntityAttachmentType.PASSENGER, attachment);
			}

			return this;
		}

		public Builder<T> vehicleAttachment(Vec3d vehicleAttachment) {
			return attachment(EntityAttachmentType.VEHICLE, vehicleAttachment);
		}

		public Builder<T> vehicleAttachment(float offsetY) {
			return attachment(EntityAttachmentType.VEHICLE, 0.0F, -offsetY, 0.0F);
		}

		public Builder<T> nameTagAttachment(float offsetY) {
			return attachment(EntityAttachmentType.NAME_TAG, 0.0F, offsetY, 0.0F);
		}

		public Builder<T> attachment(EntityAttachmentType type, float offsetX, float offsetY, float offsetZ) {
			attachments = attachments.add(type, offsetX, offsetY, offsetZ);
			return this;
		}

		public Builder<T> attachment(EntityAttachmentType type, Vec3d offset) {
			attachments = attachments.add(type, offset);
			return this;
		}

		public Builder<T> disableSummon() {
			summonable = false;
			return this;
		}

		public Builder<T> disableSaving() {
			saveable = false;
			return this;
		}

		public Builder<T> makeFireImmune() {
			fireImmune = true;
			return this;
		}

		public Builder<T> allowSpawningInside(Block... blocks) {
			canSpawnInside = ImmutableSet.copyOf(blocks);
			return this;
		}

		public Builder<T> spawnableFarFromPlayer() {
			spawnableFarFromPlayer = true;
			return this;
		}

		public Builder<T> maxTrackingRange(int maxTrackingRange) {
			this.maxTrackingRange = maxTrackingRange;
			return this;
		}

		public Builder<T> trackingTickInterval(int trackingTickInterval) {
			this.trackingTickInterval = trackingTickInterval;
			return this;
		}

		public Builder<T> requires(FeatureFlag... features) {
			requiredFeatures = FeatureFlags.FEATURE_MANAGER.featureSetOf(features);
			return this;
		}

		public Builder<T> dropsNothing() {
			lootTable = RegistryKeyedValue.fixed(Optional.empty());
			return this;
		}

		public Builder<T> notAllowedInPeaceful() {
			allowedInPeaceful = false;
			return this;
		}

		public EntityType<T> build(RegistryKey<EntityType<?>> registryKey) {
			if (saveable) {
				Util.getChoiceType(TypeReferences.ENTITY_TREE, registryKey.getValue().toString());
			}

			return new EntityType<>(
				factory,
				spawnGroup,
				saveable,
				summonable,
				fireImmune,
				spawnableFarFromPlayer,
				canSpawnInside,
				dimensions.withAttachments(attachments),
				spawnBoxScale,
				maxTrackingRange,
				trackingTickInterval,
				translationKey.get(registryKey),
				lootTable.get(registryKey),
				requiredFeatures,
				allowedInPeaceful
			);
		}
	}

	/**
	 * Фабрика для создания экземпляра сущности заданного типа.
	 */
	@FunctionalInterface
	public interface EntityFactory<T extends Entity> {

		@Nullable T create(EntityType<T> type, World world);
	}
}
