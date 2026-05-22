package net.minecraft.entity.passive;

import com.google.common.collect.Lists;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradeOffers;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

/**
 * Базовый класс для торговцев (жители, странствующий торговец).
 * Управляет списком торговых предложений, инвентарём и анимацией «кивания головой».
 */
public abstract class MerchantEntity extends PassiveEntity implements InventoryOwner, Npc, Merchant {

	private static final TrackedData<Integer> HEAD_ROLLING_TIME_LEFT = DataTracker.registerData(
		MerchantEntity.class,
		TrackedDataHandlerRegistry.INTEGER
	);

	public static final int HEAD_ROLLING_DURATION = 300;
	private static final int INVENTORY_SIZE = 8;
	/** Смещение слотов инвентаря торговца в глобальной адресации слотов. */
	private static final int INVENTORY_SLOT_OFFSET = 300;
	private static final int PRODUCE_PARTICLE_COUNT = 5;
	private static final int INTERACT_DISTANCE = 4;

	private @Nullable PlayerEntity customer;
	protected @Nullable TradeOfferList offers;
	private final SimpleInventory inventory = new SimpleInventory(INVENTORY_SIZE);

	public MerchantEntity(EntityType<? extends MerchantEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.DANGER_FIRE, 16.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		if (entityData == null) {
			entityData = new PassiveEntity.PassiveData(false);
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	public int getHeadRollingTimeLeft() {
		return dataTracker.get(HEAD_ROLLING_TIME_LEFT);
	}

	public void setHeadRollingTimeLeft(int ticks) {
		dataTracker.set(HEAD_ROLLING_TIME_LEFT, ticks);
	}

	@Override
	public int getExperience() {
		return 0;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(HEAD_ROLLING_TIME_LEFT, 0);
	}

	@Override
	public void setCustomer(@Nullable PlayerEntity customer) {
		this.customer = customer;
	}

	@Override
	public @Nullable PlayerEntity getCustomer() {
		return customer;
	}

	public boolean hasCustomer() {
		return customer != null;
	}

	/**
	 * Возвращает список торговых предложений, инициализируя его при первом обращении.
	 * Вызов на клиенте запрещён — предложения загружаются только на сервере.
	 *
	 * @throws IllegalStateException если вызван на клиентской стороне
	 */
	@Override
	public TradeOfferList getOffers() {
		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			if (offers == null) {
				offers = new TradeOfferList();
				fillRecipes(serverWorld);
			}

			return offers;
		}

		throw new IllegalStateException("Cannot load Villager offers on the client");
	}

	@Override
	public void setOffersFromServer(@Nullable TradeOfferList offers) {
	}

	@Override
	public void setExperienceFromServer(int experience) {
	}

	@Override
	public void trade(TradeOffer offer) {
		offer.use();
		ambientSoundChance = -getMinAmbientSoundDelay();
		afterUsing(offer);
		if (customer instanceof ServerPlayerEntity serverPlayer) {
			Criteria.VILLAGER_TRADE.trigger(serverPlayer, this, offer.getSellItem());
		}
	}

	protected abstract void afterUsing(TradeOffer offer);

	@Override
	public boolean isLeveledMerchant() {
		return true;
	}

	@Override
	public void onSellingItem(ItemStack stack) {
		if (getEntityWorld().isClient() || ambientSoundChance <= -getMinAmbientSoundDelay() + 20) {
			return;
		}

		ambientSoundChance = -getMinAmbientSoundDelay();
		playSound(getTradingSound(!stack.isEmpty()));
	}

	@Override
	public SoundEvent getYesSound() {
		return SoundEvents.ENTITY_VILLAGER_YES;
	}

	protected SoundEvent getTradingSound(boolean sold) {
		return sold ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO;
	}

	public void playCelebrateSound() {
		playSound(SoundEvents.ENTITY_VILLAGER_CELEBRATE);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		if (!getEntityWorld().isClient()) {
			TradeOfferList tradeOffers = getOffers();
			if (!tradeOffers.isEmpty()) {
				view.put("Offers", TradeOfferList.CODEC, tradeOffers);
			}
		}

		writeInventory(view);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		offers = view.<TradeOfferList>read("Offers", TradeOfferList.CODEC).orElse(null);
		readInventory(view);
	}

	@Override
	public @Nullable Entity teleportTo(TeleportTarget teleportTarget) {
		resetCustomer();
		return super.teleportTo(teleportTarget);
	}

	protected void resetCustomer() {
		setCustomer(null);
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);
		resetCustomer();
	}

	protected void produceParticles(ParticleEffect parameters) {
		for (int count = 0; count < PRODUCE_PARTICLE_COUNT; count++) {
			double vx = random.nextGaussian() * 0.02;
			double vy = random.nextGaussian() * 0.02;
			double vz = random.nextGaussian() * 0.02;
			getEntityWorld().addParticleClient(
				parameters,
				getParticleX(1.0),
				getRandomBodyY() + 1.0,
				getParticleZ(1.0),
				vx,
				vy,
				vz
			);
		}
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public SimpleInventory getInventory() {
		return inventory;
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		int inventorySlot = slot - INVENTORY_SLOT_OFFSET;
		return inventorySlot >= 0 && inventorySlot < inventory.size()
			? inventory.getStackReference(inventorySlot)
			: super.getStackReference(slot);
	}

	protected abstract void fillRecipes(ServerWorld world);

	/**
	 * Заполняет список предложений случайными записями из пула фабрик.
	 *
	 * @param world      серверный мир
	 * @param recipeList список, в который добавляются предложения
	 * @param pool       массив фабрик предложений
	 * @param count      сколько предложений нужно добавить
	 */
	protected void fillRecipesFromPool(
		ServerWorld world,
		TradeOfferList recipeList,
		TradeOffers.Factory[] pool,
		int count
	) {
		ArrayList<TradeOffers.Factory> factories = Lists.newArrayList(pool);
		int added = 0;

		while (added < count && !factories.isEmpty()) {
			TradeOffer offer = factories.remove(random.nextInt(factories.size())).create(world, this, random);
			if (offer != null) {
				recipeList.add(offer);
				added++;
			}
		}
	}

	@Override
	public Vec3d getLeashPos(float tickProgress) {
		float yawRad = MathHelper.lerp(tickProgress, lastBodyYaw, bodyYaw) * (float) (Math.PI / 180.0);
		Vec3d offset = new Vec3d(0.0, getBoundingBox().getLengthY() - 1.0, 0.2);
		return getLerpedPos(tickProgress).add(offset.rotateY(-yawRad));
	}

	@Override
	public boolean isClient() {
		return getEntityWorld().isClient();
	}

	@Override
	public boolean canInteract(PlayerEntity player) {
		return getCustomer() == player && isAlive() && player.canInteractWithEntity(this, INTERACT_DISTANCE);
	}
}
