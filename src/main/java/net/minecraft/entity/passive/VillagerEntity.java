package net.minecraft.entity.passive;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.SharedConstants;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.sensor.GolemLastSeenSensor;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.VillagerTaskListProvider;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.village.*;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Житель деревни — торговец с профессиями и репутационной системой.
 * Торгует предметами, размножается, спит ночью и убегает от зомби.
 */
public class VillagerEntity extends MerchantEntity implements InteractionObserver, VillagerDataContainer {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final TrackedData<VillagerData>
			VILLAGER_DATA =
			DataTracker.registerData(VillagerEntity.class, TrackedDataHandlerRegistry.VILLAGER_DATA);
	public static final int MAX_FOOD_LEVEL = 12;
	public static final Map<Item, Integer>
			ITEM_FOOD_VALUES =
			ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
	private static final int MAX_RESTOCKS_PER_DAY = 2;
	private static final int RESTOCK_INTERVAL_MINUTES = 10;
	private static final int RESTOCK_COOLDOWN_TICKS = 1200;
	private static final int TICKS_PER_DAY = 24000;
	private static final int GOSSIP_UPDATE_INTERVAL = 10;
	private static final int GOSSIP_SHARE_COUNT = 5;
	private static final long GOSSIP_DECAY_PERIOD_TICKS = 24000L;
	@VisibleForTesting
	public static final float BREEDING_FOOD_THRESHOLD = 0.5F;
	private static final int DEFAULT_FOOD_LEVEL = 0;
	private static final byte DEFAULT_GOSSIP_BYTE = 0;
	private static final int DEFAULT_EXPERIENCE = 0;
	private static final int DEFAULT_RESTOCK_COUNT = 0;
	private static final int DEFAULT_LEVEL_UP_TIMER = 0;
	private static final boolean DEFAULT_NATURAL = false;
	private int levelUpTimer;
	private boolean levelingUp;
	private @Nullable PlayerEntity lastCustomer;
	private boolean needsOffersUpdate;
	private int foodLevel = 0;
	private final VillagerGossips gossip = new VillagerGossips();
	private long gossipStartTime;
	private long lastGossipDecayTime = 0L;
	private int experience = 0;
	private long lastRestockTime = 0L;
	private int restocksToday = 0;
	private long lastRestockCheckTime;
	private boolean natural = false;
	private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
			MemoryModuleType.HOME,
			MemoryModuleType.JOB_SITE,
			MemoryModuleType.POTENTIAL_JOB_SITE,
			MemoryModuleType.MEETING_POINT,
			MemoryModuleType.MOBS,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.VISIBLE_VILLAGER_BABIES,
			MemoryModuleType.NEAREST_PLAYERS,
			MemoryModuleType.NEAREST_VISIBLE_PLAYER,
			MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
			MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
			MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
			new MemoryModuleType[]{
					MemoryModuleType.WALK_TARGET,
					MemoryModuleType.LOOK_TARGET,
					MemoryModuleType.INTERACTION_TARGET,
					MemoryModuleType.BREED_TARGET,
					MemoryModuleType.PATH,
					MemoryModuleType.DOORS_TO_CLOSE,
					MemoryModuleType.NEAREST_BED,
					MemoryModuleType.HURT_BY,
					MemoryModuleType.HURT_BY_ENTITY,
					MemoryModuleType.NEAREST_HOSTILE,
					MemoryModuleType.SECONDARY_JOB_SITE,
					MemoryModuleType.HIDING_PLACE,
					MemoryModuleType.HEARD_BELL_TIME,
					MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
					MemoryModuleType.LAST_SLEPT,
					MemoryModuleType.LAST_WOKEN,
					MemoryModuleType.LAST_WORKED_AT_POI,
					MemoryModuleType.GOLEM_DETECTED_RECENTLY
			}
	);
	private static final ImmutableList<SensorType<? extends Sensor<? super VillagerEntity>>> SENSORS = ImmutableList.of(
			SensorType.NEAREST_LIVING_ENTITIES,
			SensorType.NEAREST_PLAYERS,
			SensorType.NEAREST_ITEMS,
			SensorType.NEAREST_BED,
			SensorType.HURT_BY,
			SensorType.VILLAGER_HOSTILES,
			SensorType.VILLAGER_BABIES,
			SensorType.SECONDARY_POIS,
			SensorType.GOLEM_DETECTED
	);
	public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>>>
			POINTS_OF_INTEREST =
			ImmutableMap.of(
					MemoryModuleType.HOME,
					(BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>>) (villager, poi) -> poi.matchesKey(
							PointOfInterestTypes.HOME),
					MemoryModuleType.JOB_SITE,
					(BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>>) (villager, poi) -> villager
							.getVillagerData()
							.profession()
							.value()
							.heldWorkstation()
							.test(poi),
					MemoryModuleType.POTENTIAL_JOB_SITE,
					(BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>>) (villager, poi) -> VillagerProfession.IS_ACQUIRABLE_JOB_SITE.test(
							poi),
					MemoryModuleType.MEETING_POINT,
					(BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>>) (villager, poi) -> poi.matchesKey(
							PointOfInterestTypes.MEETING)
			);

	public VillagerEntity(EntityType<? extends VillagerEntity> entityType, World world) {
		this(entityType, world, VillagerType.PLAINS);
	}

	public VillagerEntity(
			EntityType<? extends VillagerEntity> entityType,
			World world,
			RegistryKey<VillagerType> type
	) {
		this(entityType, world, world.getRegistryManager().getEntryOrThrow(type));
	}

	public VillagerEntity(
			EntityType<? extends VillagerEntity> entityType,
			World world,
			RegistryEntry<VillagerType> type
	) {
		super(entityType, world);
		this.getNavigation().setCanOpenDoors(true);
		this.getNavigation().setCanSwim(true);
		this.getNavigation().setMaxFollowRange(48.0F);
		this.setCanPickUpLoot(true);
		this.setVillagerData(this
				.getVillagerData()
				.withType(type)
				.withProfession(world.getRegistryManager(), VillagerProfession.NONE));
	}

	@Override
	public Brain<VillagerEntity> getBrain() {
		return (Brain<VillagerEntity>) super.getBrain();
	}

	@Override
	protected Brain.Profile<VillagerEntity> createBrainProfile() {
		return Brain.createProfile(MEMORY_MODULES, SENSORS);
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		Brain<VillagerEntity> brain = this.createBrainProfile().deserialize(dynamic);
		this.initBrain(brain);
		return brain;
	}

	/**
	 * Reinitialize brain.
	 *
	 * @param world world
	 */
	public void reinitializeBrain(ServerWorld world) {
		Brain<VillagerEntity> brain = this.getBrain();
		brain.stopAllTasks(world, this);
		this.brain = brain.copy();
		this.initBrain(this.getBrain());
	}

	private void initBrain(Brain<VillagerEntity> brain) {
		RegistryEntry<VillagerProfession> registryEntry = this.getVillagerData().profession();
		if (this.isBaby()) {
			brain.setSchedule(EnvironmentAttributes.BABY_VILLAGER_ACTIVITY_GAMEPLAY);
			brain.setTaskList(Activity.PLAY, VillagerTaskListProvider.createPlayTasks(0.5F));
		}
		else {
			brain.setSchedule(EnvironmentAttributes.VILLAGER_ACTIVITY_GAMEPLAY);
			brain.setTaskList(
					Activity.WORK,
					VillagerTaskListProvider.createWorkTasks(registryEntry, 0.5F),
					ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryModuleState.VALUE_PRESENT))
			);
		}

		brain.setTaskList(Activity.CORE, VillagerTaskListProvider.createCoreTasks(registryEntry, 0.5F));
		brain.setTaskList(
				Activity.MEET,
				VillagerTaskListProvider.createMeetTasks(registryEntry, 0.5F),
				ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryModuleState.VALUE_PRESENT))
		);
		brain.setTaskList(Activity.REST, VillagerTaskListProvider.createRestTasks(registryEntry, 0.5F));
		brain.setTaskList(Activity.IDLE, VillagerTaskListProvider.createIdleTasks(registryEntry, 0.5F));
		brain.setTaskList(Activity.PANIC, VillagerTaskListProvider.createPanicTasks(registryEntry, 0.5F));
		brain.setTaskList(Activity.PRE_RAID, VillagerTaskListProvider.createPreRaidTasks(registryEntry, 0.5F));
		brain.setTaskList(Activity.RAID, VillagerTaskListProvider.createRaidTasks(registryEntry, 0.5F));
		brain.setTaskList(Activity.HIDE, VillagerTaskListProvider.createHideTasks(registryEntry, 0.5F));
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.doExclusively(Activity.IDLE);
		brain.refreshActivities(
				this.getEntityWorld().getEnvironmentAttributes(),
				this.getEntityWorld().getTime(),
				this.getEntityPos()
		);
	}

	@Override
	protected void onGrowUp() {
		super.onGrowUp();
		if (this.getEntityWorld() instanceof ServerWorld) {
			this.reinitializeBrain((ServerWorld) this.getEntityWorld());
		}
	}

	public static DefaultAttributeContainer.Builder createVillagerAttributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.MOVEMENT_SPEED, 0.5);
	}

	public boolean isNatural() {
		return this.natural;
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("villagerBrain");
		this.getBrain().tick(world, this);
		profiler.pop();
		if (this.natural) {
			this.natural = false;
		}

		if (!this.hasCustomer() && this.levelUpTimer > 0) {
			this.levelUpTimer--;
			if (this.levelUpTimer <= 0) {
				if (this.levelingUp) {
					this.levelUp(world);
					this.levelingUp = false;
				}

				this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0));
			}
		}

		if (this.lastCustomer != null) {
			world.handleInteraction(EntityInteraction.TRADE, this.lastCustomer, this);
			world.sendEntityStatus(this, (byte) 14);
			this.lastCustomer = null;
		}

		if (!this.isAiDisabled() && this.random.nextInt(100) == 0) {
			Raid raid = world.getRaidAt(this.getBlockPos());
			if (raid != null && raid.isActive() && !raid.isFinished()) {
				world.sendEntityStatus(this, (byte) 42);
			}
		}

		if (this.getVillagerData().profession().matchesKey(VillagerProfession.NONE) && this.hasCustomer()) {
			this.resetCustomer();
		}

		super.mobTick(world);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getHeadRollingTimeLeft() > 0) {
			this.setHeadRollingTimeLeft(this.getHeadRollingTimeLeft() - 1);
		}

		this.decayGossip();
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.isOf(Items.VILLAGER_SPAWN_EGG) || !this.isAlive() || this.hasCustomer() || this.isSleeping()) {
			return super.interactMob(player, hand);
		}
		else if (this.isBaby()) {
			this.sayNo();
			return ActionResult.SUCCESS;
		}
		else {
			if (!this.getEntityWorld().isClient()) {
				boolean bl = this.getOffers().isEmpty();
				if (hand == Hand.MAIN_HAND) {
					if (bl) {
						this.sayNo();
					}

					player.incrementStat(Stats.TALKED_TO_VILLAGER);
				}

				if (bl) {
					return ActionResult.CONSUME;
				}

				this.beginTradeWith(player);
			}

			return ActionResult.SUCCESS;
		}
	}

	private void sayNo() {
		this.setHeadRollingTimeLeft(40);
		if (!this.getEntityWorld().isClient()) {
			this.playSound(SoundEvents.ENTITY_VILLAGER_NO);
		}
	}

	private void beginTradeWith(PlayerEntity customer) {
		this.prepareOffersFor(customer);
		this.setCustomer(customer);
		this.sendOffers(customer, this.getDisplayName(), this.getVillagerData().level());
	}

	@Override
	public void setCustomer(@Nullable PlayerEntity customer) {
		boolean bl = this.getCustomer() != null && customer == null;
		super.setCustomer(customer);
		if (bl) {
			this.resetCustomer();
		}
	}

	@Override
	protected void resetCustomer() {
		super.resetCustomer();
		this.clearSpecialPrices();
	}

	private void clearSpecialPrices() {
		if (!this.getEntityWorld().isClient()) {
			for (TradeOffer tradeOffer : this.getOffers()) {
				tradeOffer.clearSpecialPrice();
			}
		}
	}

	@Override
	public boolean canRefreshTrades() {
		return true;
	}

	/**
	 * Restock.
	 */
	public void restock() {
		this.updateDemandBonus();

		for (TradeOffer tradeOffer : this.getOffers()) {
			tradeOffer.resetUses();
		}

		this.sendOffersToCustomer();
		this.lastRestockTime = this.getEntityWorld().getTime();
		this.restocksToday++;
	}

	private void sendOffersToCustomer() {
		TradeOfferList tradeOfferList = this.getOffers();
		PlayerEntity playerEntity = this.getCustomer();
		if (playerEntity != null && !tradeOfferList.isEmpty()) {
			playerEntity.sendTradeOffers(
					playerEntity.currentScreenHandler.syncId,
					tradeOfferList,
					this.getVillagerData().level(),
					this.getExperience(),
					this.isLeveledMerchant(),
					this.canRefreshTrades()
			);
		}
	}

	private boolean needsRestock() {
		for (TradeOffer tradeOffer : this.getOffers()) {
			if (tradeOffer.hasBeenUsed()) {
				return true;
			}
		}

		return false;
	}

	private boolean canRestock() {
		return this.restocksToday == 0
				|| this.restocksToday < 2 && this.getEntityWorld().getTime() > this.lastRestockTime + 2400L;
	}

	/**
	 * Определяет, следует ли restock.
	 *
	 * @param world world
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldRestock(ServerWorld world) {
		long l = this.lastRestockTime + 12000L;
		long m = this.getEntityWorld().getTime();
		boolean bl = m > l;
		long n = world.getDay();
		bl |= this.lastRestockCheckTime > 0L && n > this.lastRestockCheckTime;
		this.lastRestockCheckTime = n;
		if (bl) {
			this.lastRestockTime = m;
			this.clearDailyRestockCount();
		}

		return this.canRestock() && this.needsRestock();
	}

	private void restockAndUpdateDemandBonus() {
		int i = 2 - this.restocksToday;
		if (i > 0) {
			for (TradeOffer tradeOffer : this.getOffers()) {
				tradeOffer.resetUses();
			}
		}

		for (int j = 0; j < i; j++) {
			this.updateDemandBonus();
		}

		this.sendOffersToCustomer();
	}

	private void updateDemandBonus() {
		for (TradeOffer tradeOffer : this.getOffers()) {
			tradeOffer.updateDemandBonus();
		}
	}

	private void prepareOffersFor(PlayerEntity player) {
		int i = this.getReputation(player);
		if (i != 0) {
			for (TradeOffer tradeOffer : this.getOffers()) {
				tradeOffer.increaseSpecialPrice(-MathHelper.floor(i * tradeOffer.getPriceMultiplier()));
			}
		}

		if (player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
			StatusEffectInstance statusEffectInstance = player.getStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
			int j = statusEffectInstance.getAmplifier();

			for (TradeOffer tradeOffer2 : this.getOffers()) {
				double d = 0.3 + 0.0625 * j;
				int k = (int) Math.floor(d * tradeOffer2.getOriginalFirstBuyItem().getCount());
				tradeOffer2.increaseSpecialPrice(-Math.max(k, 1));
			}
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VILLAGER_DATA, createVillagerData());
	}

	/**
	 * Создаёт villager data.
	 *
	 * @return VillagerData — результат операции
	 */
	public static VillagerData createVillagerData() {
		return new VillagerData(
				Registries.VILLAGER_TYPE.getOrThrow(VillagerType.PLAINS),
				Registries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE),
				1
		);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("VillagerData", VillagerData.CODEC, this.getVillagerData());
		view.putByte("FoodLevel", (byte) this.foodLevel);
		view.put("Gossips", VillagerGossips.CODEC, this.gossip);
		view.putInt("Xp", this.experience);
		view.putLong("LastRestock", this.lastRestockTime);
		view.putLong("LastGossipDecay", this.lastGossipDecayTime);
		view.putInt("RestocksToday", this.restocksToday);
		if (this.natural) {
			view.putBoolean("AssignProfessionWhenSpawned", true);
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.dataTracker.set(
				VILLAGER_DATA,
				view
						.<VillagerData>read("VillagerData", VillagerData.CODEC)
						.orElseGet(VillagerEntity::createVillagerData)
		);
		this.foodLevel = view.getByte("FoodLevel", (byte) 0);
		this.gossip.clear();
		view.<VillagerGossips>read("Gossips", VillagerGossips.CODEC).ifPresent(this.gossip::add);
		this.experience = view.getInt("Xp", 0);
		this.lastRestockTime = view.getLong("LastRestock", 0L);
		this.lastGossipDecayTime = view.getLong("LastGossipDecay", 0L);
		if (this.getEntityWorld() instanceof ServerWorld) {
			this.reinitializeBrain((ServerWorld) this.getEntityWorld());
		}

		this.restocksToday = view.getInt("RestocksToday", 0);
		this.natural = view.getBoolean("AssignProfessionWhenSpawned", false);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		if (this.isSleeping()) {
			return null;
		}
		else {
			return this.hasCustomer() ? SoundEvents.ENTITY_VILLAGER_TRADE : SoundEvents.ENTITY_VILLAGER_AMBIENT;
		}
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_VILLAGER_DEATH;
	}

	/**
	 * Play work sound.
	 */
	public void playWorkSound() {
		this.playSound(this.getVillagerData().profession().value().workSound());
	}

	@Override
	public void setVillagerData(VillagerData villagerData) {
		VillagerData villagerData2 = this.getVillagerData();
		if (!villagerData2.profession().equals(villagerData.profession())) {
			this.offers = null;
		}

		this.dataTracker.set(VILLAGER_DATA, villagerData);
	}

	@Override
	public VillagerData getVillagerData() {
		return this.dataTracker.get(VILLAGER_DATA);
	}

	@Override
	protected void afterUsing(TradeOffer offer) {
		int i = 3 + this.random.nextInt(4);
		this.experience = this.experience + offer.getMerchantExperience();
		this.lastCustomer = this.getCustomer();
		if (this.canLevelUp()) {
			this.levelUpTimer = 40;
			this.levelingUp = true;
			i += 5;
		}

		if (offer.shouldRewardPlayerExperience()) {
			this
					.getEntityWorld()
					.spawnEntity(new ExperienceOrbEntity(
							this.getEntityWorld(),
							this.getX(),
							this.getY() + 0.5,
							this.getZ(),
							i
					));
		}
	}

	@Override
	public void setAttacker(@Nullable LivingEntity attacker) {
		if (attacker != null && this.getEntityWorld() instanceof ServerWorld) {
			((ServerWorld) this.getEntityWorld()).handleInteraction(EntityInteraction.VILLAGER_HURT, attacker, this);
			if (this.isAlive() && attacker instanceof PlayerEntity) {
				this.getEntityWorld().sendEntityStatus(this, (byte) 13);
			}
		}

		super.setAttacker(attacker);
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		LOGGER.info("Villager {} died, message: '{}'", this, damageSource.getDeathMessage(this).getString());
		Entity entity = damageSource.getAttacker();
		if (entity != null) {
			this.notifyDeath(entity);
		}

		this.releaseAllTickets();
		super.onDeath(damageSource);
	}

	private void releaseAllTickets() {
		this.releaseTicketFor(MemoryModuleType.HOME);
		this.releaseTicketFor(MemoryModuleType.JOB_SITE);
		this.releaseTicketFor(MemoryModuleType.POTENTIAL_JOB_SITE);
		this.releaseTicketFor(MemoryModuleType.MEETING_POINT);
	}

	private void notifyDeath(Entity killer) {
		if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
			Optional<LivingTargetCache>
					optional =
					this.brain.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS);
			if (!optional.isEmpty()) {
				optional.get()
				        .iterate(InteractionObserver.class::isInstance)
				        .forEach(observer -> serverWorld.handleInteraction(
						        EntityInteraction.VILLAGER_KILLED,
						        killer,
						        (InteractionObserver) observer
				        ));
			}
		}
	}

	/**
	 * Release ticket for.
	 *
	 * @param pos pos
	 */
	public void releaseTicketFor(MemoryModuleType<GlobalPos> pos) {
		if (this.getEntityWorld() instanceof ServerWorld) {
			MinecraftServer minecraftServer = ((ServerWorld) this.getEntityWorld()).getServer();
			this.brain.getOptionalRegisteredMemory(pos).ifPresent(posx -> {
				ServerWorld serverWorld = minecraftServer.getWorld(posx.dimension());
				if (serverWorld != null) {
					PointOfInterestStorage pointOfInterestStorage = serverWorld.getPointOfInterestStorage();
					Optional<RegistryEntry<PointOfInterestType>> optional = pointOfInterestStorage.getType(posx.pos());
					BiPredicate<VillagerEntity, RegistryEntry<PointOfInterestType>>
							biPredicate =
							POINTS_OF_INTEREST.get(pos);
					if (optional.isPresent() && biPredicate.test(this, optional.get())) {
						pointOfInterestStorage.releaseTicket(posx.pos());
						serverWorld.getSubscriptionTracker().onPoiUpdated(posx.pos());
					}
				}
			});
		}
	}

	@Override
	public boolean isReadyToBreed() {
		return this.foodLevel + this.getAvailableFood() >= MAX_FOOD_LEVEL && !this.isSleeping() && this.getBreedingAge() == 0;
	}

	private boolean canEatFood() {
		return this.foodLevel < MAX_FOOD_LEVEL;
	}

	private void consumeAvailableFood() {
		if (this.canEatFood() && this.getAvailableFood() != 0) {
			for (int i = 0; i < this.getInventory().size(); i++) {
				ItemStack itemStack = this.getInventory().getStack(i);
				if (!itemStack.isEmpty()) {
					Integer integer = ITEM_FOOD_VALUES.get(itemStack.getItem());
					if (integer != null) {
						int j = itemStack.getCount();

						for (int k = j; k > 0; k--) {
							this.foodLevel = this.foodLevel + integer;
							this.getInventory().removeStack(i, 1);
							if (!this.canEatFood()) {
								return;
							}
						}
					}
				}
			}
		}
	}

	public int getReputation(PlayerEntity player) {
		return this.gossip.getReputationFor(player.getUuid(), gossipType -> true);
	}

	private void depleteFood(int amount) {
		this.foodLevel -= amount;
	}

	/**
	 * Eat for breeding.
	 */
	public void eatForBreeding() {
		this.consumeAvailableFood();
		this.depleteFood(MAX_FOOD_LEVEL);
	}

	public void setOffers(TradeOfferList offers) {
		this.offers = offers;
	}

	private boolean canLevelUp() {
		int i = this.getVillagerData().level();
		return VillagerData.canLevelUp(i) && this.experience >= VillagerData.getUpperLevelExperience(i);
	}

	private void levelUp(ServerWorld world) {
		this.setVillagerData(this.getVillagerData().withLevel(this.getVillagerData().level() + 1));
		this.fillRecipes(world);
	}

	@Override
	protected Text getDefaultName() {
		return this.getVillagerData().profession().value().id();
	}

	@Override
	public void handleStatus(byte status) {
		if (status == MAX_FOOD_LEVEL) {
			this.produceParticles(ParticleTypes.HEART);
		}
		else if (status == 13) {
			this.produceParticles(ParticleTypes.ANGRY_VILLAGER);
		}
		else if (status == 14) {
			this.produceParticles(ParticleTypes.HAPPY_VILLAGER);
		}
		else if (status == 42) {
			this.produceParticles(ParticleTypes.SPLASH);
		}
		else {
			super.handleStatus(status);
		}
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (spawnReason == SpawnReason.BREEDING) {
			this.setVillagerData(this
					.getVillagerData()
					.withProfession(world.getRegistryManager(), VillagerProfession.NONE));
		}

		if (spawnReason == SpawnReason.COMMAND
				|| spawnReason == SpawnReason.SPAWN_ITEM_USE
				|| SpawnReason.isAnySpawner(spawnReason)
				|| spawnReason == SpawnReason.DISPENSER) {
			this.setVillagerData(this
					.getVillagerData()
					.withType(world.getRegistryManager(), VillagerType.forBiome(world.getBiome(this.getBlockPos()))));
		}

		if (spawnReason == SpawnReason.STRUCTURE) {
			this.natural = true;
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	/**
	 * Создаёт child.
	 *
	 * @param serverWorld server world
	 * @param passiveEntity passive entity
	 *
	 * @return @Nullable VillagerEntity — результат операции
	 */
	public @Nullable VillagerEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		double d = this.random.nextDouble();
		RegistryEntry<VillagerType> registryEntry;
		if (d < 0.5) {
			registryEntry =
					serverWorld
							.getRegistryManager()
							.getEntryOrThrow(VillagerType.forBiome(serverWorld.getBiome(this.getBlockPos())));
		}
		else if (d < 0.75) {
			registryEntry = this.getVillagerData().type();
		}
		else {
			registryEntry = ((VillagerEntity) passiveEntity).getVillagerData().type();
		}

		VillagerEntity villagerEntity = new VillagerEntity(EntityType.VILLAGER, serverWorld, registryEntry);
		villagerEntity.initialize(
				serverWorld,
				serverWorld.getLocalDifficulty(villagerEntity.getBlockPos()),
				SpawnReason.BREEDING,
				null
		);
		return villagerEntity;
	}

	@Override
	public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
		if (world.getDifficulty() != Difficulty.PEACEFUL) {
			LOGGER.info("Villager {} was struck by lightning {}.", this, lightning);
			WitchEntity witchEntity = this.convertTo(
					EntityType.WITCH, EntityConversionContext.create(this, false, false), witch -> {
						witch.initialize(
								world,
								world.getLocalDifficulty(witch.getBlockPos()),
								SpawnReason.CONVERSION,
								null
						);
						witch.setPersistent();
						this.releaseAllTickets();
					}
			);
			if (witchEntity == null) {
				super.onStruckByLightning(world, lightning);
			}
		}
		else {
			super.onStruckByLightning(world, lightning);
		}
	}

	@Override
	protected void loot(ServerWorld world, ItemEntity itemEntity) {
		InventoryOwner.pickUpItem(world, this, this, itemEntity);
	}

	@Override
	public boolean canGather(ServerWorld world, ItemStack stack) {
		Item item = stack.getItem();
		return (stack.isIn(ItemTags.VILLAGER_PICKS_UP) || this
				.getVillagerData()
				.profession()
				.value()
				.gatherableItems()
				.contains(item)
		)
				&& this.getInventory().canInsert(stack);
	}

	/**
	 * Проверяет возможность share food for breeding.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canShareFoodForBreeding() {
		return this.getAvailableFood() >= 24;
	}

	/**
	 * Needs food for breeding.
	 *
	 * @return boolean — результат операции
	 */
	public boolean needsFoodForBreeding() {
		return this.getAvailableFood() < MAX_FOOD_LEVEL;
	}

	private int getAvailableFood() {
		SimpleInventory simpleInventory = this.getInventory();
		return ITEM_FOOD_VALUES
				.entrySet()
				.stream()
				.mapToInt(item -> simpleInventory.count(item.getKey()) * item.getValue())
				.sum();
	}

	public boolean hasSeedToPlant() {
		return this.getInventory().containsAny(stack -> stack.isIn(ItemTags.VILLAGER_PLANTABLE_SEEDS));
	}

	@Override
	protected void fillRecipes(ServerWorld world) {
		VillagerData villagerData = this.getVillagerData();
		RegistryKey<VillagerProfession> registryKey = villagerData.profession().getKey().orElse(null);
		if (registryKey != null) {
			Int2ObjectMap<TradeOffers.Factory[]> int2ObjectMap2;
			if (this.getEntityWorld().getEnabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
				Int2ObjectMap<TradeOffers.Factory[]>
						int2ObjectMap =
						TradeOffers.REBALANCED_PROFESSION_TO_LEVELED_TRADE.get(registryKey);
				int2ObjectMap2 =
						int2ObjectMap != null ? int2ObjectMap
						                      : TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(registryKey);
			}
			else {
				int2ObjectMap2 = TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(registryKey);
			}

			if (int2ObjectMap2 != null && !int2ObjectMap2.isEmpty()) {
				TradeOffers.Factory[] factorys = (TradeOffers.Factory[]) int2ObjectMap2.get(villagerData.level());
				if (factorys != null) {
					TradeOfferList tradeOfferList = this.getOffers();
					this.fillRecipesFromPool(world, tradeOfferList, factorys, 2);
					if (SharedConstants.UNLOCK_ALL_TRADES && villagerData.level() < int2ObjectMap2.size()) {
						this.levelUp(world);
					}
				}
			}
		}
	}

	/**
	 * Talk with villager.
	 *
	 * @param world world
	 * @param villager villager
	 * @param time time
	 */
	public void talkWithVillager(ServerWorld world, VillagerEntity villager, long time) {
		if ((time < this.gossipStartTime || time >= this.gossipStartTime + RESTOCK_COOLDOWN_TICKS)
				&& (time < villager.gossipStartTime || time >= villager.gossipStartTime + RESTOCK_COOLDOWN_TICKS)) {
			this.gossip.shareGossipFrom(villager.gossip, this.random, 10);
			this.gossipStartTime = time;
			villager.gossipStartTime = time;
			this.summonGolem(world, time, 5);
		}
	}

	private void decayGossip() {
		long l = this.getEntityWorld().getTime();
		if (this.lastGossipDecayTime == 0L) {
			this.lastGossipDecayTime = l;
		}
		else if (l >= this.lastGossipDecayTime + GOSSIP_DECAY_PERIOD_TICKS) {
			this.gossip.decay();
			this.lastGossipDecayTime = l;
		}
	}

	/**
	 * Summon golem.
	 *
	 * @param world world
	 * @param time time
	 * @param requiredCount required count
	 */
	public void summonGolem(ServerWorld world, long time, int requiredCount) {
		if (this.canSummonGolem(time)) {
			Box box = this.getBoundingBox().expand(10.0, 10.0, 10.0);
			List<VillagerEntity> list = world.getNonSpectatingEntities(VillagerEntity.class, box);
			List<VillagerEntity>
					list2 =
					list.stream().filter(villager -> villager.canSummonGolem(time)).limit(5L).toList();
			if (list2.size() >= requiredCount) {
				if (!LargeEntitySpawnHelper.trySpawnAt(
						                           EntityType.IRON_GOLEM,
						                           SpawnReason.MOB_SUMMONED,
						                           world,
						                           this.getBlockPos(),
						                           10,
						                           8,
						                           6,
						                           LargeEntitySpawnHelper.Requirements.IRON_GOLEM,
						                           false
				                           )
				                           .isEmpty()) {
					list.forEach(GolemLastSeenSensor::rememberIronGolem);
				}
			}
		}
	}

	/**
	 * Проверяет возможность summon golem.
	 *
	 * @param time time
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canSummonGolem(long time) {
		return !this.hasRecentlySlept(this.getEntityWorld().getTime()) ? false
		                                                               : !this.brain.hasMemoryModule(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
	}

	@Override
	public void onInteractionWith(EntityInteraction interaction, Entity entity) {
		if (interaction == EntityInteraction.ZOMBIE_VILLAGER_CURED) {
			this.gossip.startGossip(entity.getUuid(), VillagerGossipType.MAJOR_POSITIVE, 20);
			this.gossip.startGossip(entity.getUuid(), VillagerGossipType.MINOR_POSITIVE, 25);
		}
		else if (interaction == EntityInteraction.TRADE) {
			this.gossip.startGossip(entity.getUuid(), VillagerGossipType.TRADING, 2);
		}
		else if (interaction == EntityInteraction.VILLAGER_HURT) {
			this.gossip.startGossip(entity.getUuid(), VillagerGossipType.MINOR_NEGATIVE, 25);
		}
		else if (interaction == EntityInteraction.VILLAGER_KILLED) {
			this.gossip.startGossip(entity.getUuid(), VillagerGossipType.MAJOR_NEGATIVE, 25);
		}
	}

	@Override
	public int getExperience() {
		return this.experience;
	}

	public void setExperience(int experience) {
		this.experience = experience;
	}

	private void clearDailyRestockCount() {
		this.restockAndUpdateDemandBonus();
		this.restocksToday = 0;
	}

	public VillagerGossips getGossip() {
		return this.gossip;
	}

	/**
	 * Читает gossip data.
	 *
	 * @param gossips gossips
	 */
	public void readGossipData(VillagerGossips gossips) {
		this.gossip.add(gossips);
	}

	@Override
	public void sleep(BlockPos pos) {
		super.sleep(pos);
		this.brain.remember(MemoryModuleType.LAST_SLEPT, this.getEntityWorld().getTime());
		this.brain.forget(MemoryModuleType.WALK_TARGET);
		this.brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
	}

	@Override
	public void wakeUp() {
		super.wakeUp();
		this.brain.remember(MemoryModuleType.LAST_WOKEN, this.getEntityWorld().getTime());
	}

	private boolean hasRecentlySlept(long worldTime) {
		Optional<Long> optional = this.brain.getOptionalRegisteredMemory(MemoryModuleType.LAST_SLEPT);
		return optional.filter(lastSlept -> worldTime - lastSlept < GOSSIP_DECAY_PERIOD_TICKS).isPresent();
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.VILLAGER_VARIANT ? castComponentValue(
				(ComponentType<T>) type,
				this.getVillagerData().type()
		) : super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.VILLAGER_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.VILLAGER_VARIANT) {
			RegistryEntry<VillagerType> registryEntry = castComponentValue(DataComponentTypes.VILLAGER_VARIANT, value);
			this.setVillagerData(this.getVillagerData().withType(registryEntry));
			return true;
		}
		else {
			return super.setApplicableComponent(type, value);
		}
	}
}
