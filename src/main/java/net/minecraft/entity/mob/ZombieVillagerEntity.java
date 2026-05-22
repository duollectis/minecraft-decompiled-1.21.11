package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.*;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Зомби-житель — заражённый вариант жителя. Может быть исцелён золотым яблоком при наличии
 * эффекта слабости. Сохраняет профессию, сделки и сплетни оригинального жителя.
 * Скорость исцеления ускоряется при наличии рядом железных прутьев и кроватей.
 */
public class ZombieVillagerEntity extends ZombieEntity implements VillagerDataContainer {

	private static final TrackedData<Boolean>
			CONVERTING =
			DataTracker.registerData(ZombieVillagerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<VillagerData>
			VILLAGER_DATA =
			DataTracker.registerData(ZombieVillagerEntity.class, TrackedDataHandlerRegistry.VILLAGER_DATA);
	private static final int BASE_CONVERSION_DELAY = 3600;
	private static final int MAX_CONVERSION_DELAY = 6000;
	private static final int MAX_CONVERSION_BOOST_BLOCKS = 14;
	private static final int CONVERSION_BOOST_RADIUS = 4;
	private static final int DEFAULT_CONVERSION_TIME = -1;
	private static final Set<SpawnReason> SPAWN_REASONS_WITHOUT_PROFESSION = EnumSet.of(
			SpawnReason.LOAD,
			SpawnReason.DIMENSION_TRAVEL,
			SpawnReason.CONVERSION,
			SpawnReason.SPAWN_ITEM_USE,
			SpawnReason.SPAWNER,
			SpawnReason.TRIAL_SPAWNER
	);
	private int conversionTimer;
	private @Nullable UUID converter;
	private @Nullable VillagerGossips gossip;
	private @Nullable TradeOfferList offerData;
	private int experience;

	public ZombieVillagerEntity(EntityType<? extends ZombieVillagerEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CONVERTING, false);
		builder.add(VILLAGER_DATA, createVillagerData());
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("VillagerData", VillagerData.CODEC, getVillagerData());
		view.putNullable("Offers", TradeOfferList.CODEC, offerData);
		view.putNullable("Gossips", VillagerGossips.CODEC, gossip);
		view.putInt("ConversionTime", isConverting() ? conversionTimer : DEFAULT_CONVERSION_TIME);
		view.putNullable("ConversionPlayer", Uuids.INT_STREAM_CODEC, converter);
		view.putInt("Xp", experience);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		dataTracker.set(
				VILLAGER_DATA,
				view.<VillagerData>read("VillagerData", VillagerData.CODEC).orElseGet(this::createVillagerData)
		);
		offerData = view.<TradeOfferList>read("Offers", TradeOfferList.CODEC).orElse(null);
		gossip = view.<VillagerGossips>read("Gossips", VillagerGossips.CODEC).orElse(null);

		int savedConversionTime = view.getInt("ConversionTime", DEFAULT_CONVERSION_TIME);

		if (savedConversionTime != DEFAULT_CONVERSION_TIME) {
			UUID converterUuid = view.<UUID>read("ConversionPlayer", Uuids.INT_STREAM_CODEC).orElse(null);
			setConverting(converterUuid, savedConversionTime);
		}
		else {
			getDataTracker().set(CONVERTING, false);
			conversionTimer = DEFAULT_CONVERSION_TIME;
		}

		experience = view.getInt("Xp", 0);
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (!SPAWN_REASONS_WITHOUT_PROFESSION.contains(spawnReason)) {
			setVillagerData(
					getVillagerData().withType(world.getRegistryManager(), VillagerType.forBiome(world.getBiome(getBlockPos())))
			);
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	private VillagerData createVillagerData() {
		Optional<RegistryEntry.Reference<VillagerProfession>> profession = Registries.VILLAGER_PROFESSION.getRandom(random);
		VillagerData villagerData = VillagerEntity.createVillagerData();

		if (profession.isPresent()) {
			villagerData = villagerData.withProfession(profession.get());
		}

		return villagerData;
	}

	@Override
	public void tick() {
		if (!getEntityWorld().isClient() && isAlive() && isConverting()) {
			conversionTimer -= getConversionRate();

			if (conversionTimer <= 0) {
				finishConversion((ServerWorld) getEntityWorld());
			}
		}

		super.tick();
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack heldStack = player.getStackInHand(hand);

		if (!heldStack.isOf(Items.GOLDEN_APPLE)) {
			return super.interactMob(player, hand);
		}

		if (!hasStatusEffect(StatusEffects.WEAKNESS)) {
			return ActionResult.CONSUME;
		}

		heldStack.decrementUnlessCreative(1, player);

		if (!getEntityWorld().isClient()) {
			setConverting(player.getUuid(), random.nextInt(2401) + BASE_CONVERSION_DELAY);
		}

		return ActionResult.SUCCESS_SERVER;
	}

	@Override
	protected boolean canConvertInWater() {
		return false;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return !isConverting() && experience == 0;
	}

	public boolean isConverting() {
		return getDataTracker().get(CONVERTING);
	}

	/**
	 * Запускает процесс исцеления: снимает слабость, добавляет силу на время конвертации,
	 * отправляет клиентам статус-событие для воспроизведения звука лечения.
	 */
	private void setConverting(@Nullable UUID converterUuid, int delay) {
		converter = converterUuid;
		conversionTimer = delay;
		getDataTracker().set(CONVERTING, true);
		removeStatusEffect(StatusEffects.WEAKNESS);
		addStatusEffect(new StatusEffectInstance(
				StatusEffects.STRENGTH,
				delay,
				Math.min(getEntityWorld().getDifficulty().getId() - 1, 0)
		));
		getEntityWorld().sendEntityStatus(this, (byte) 16);
	}

	@Override
	public void handleStatus(byte status) {
		if (status != 16) {
			super.handleStatus(status);
			return;
		}

		if (!isSilent()) {
			getEntityWorld().playSoundClient(
					getX(),
					getEyeY(),
					getZ(),
					SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
					getSoundCategory(),
					1.0F + random.nextFloat(),
					random.nextFloat() * 0.7F + 0.3F,
					false
			);
		}
	}

	/**
	 * Завершает превращение в жителя: переносит снаряжение, профессию, сделки, сплетни и опыт.
	 * Выдаёт достижение игроку, который инициировал исцеление.
	 */
	private void finishConversion(ServerWorld world) {
		convertTo(
				EntityType.VILLAGER,
				EntityConversionContext.create(this, false, false),
				villager -> {
					for (EquipmentSlot slot : dropForeignEquipment(
							world,
							stack -> !EnchantmentHelper.hasAnyEnchantmentsWith(
									stack,
									EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE
							)
					)) {
						StackReference slotRef = villager.getStackReference(slot.getEntitySlotId() + 300);

						if (slotRef != null) {
							slotRef.set(getEquippedStack(slot));
						}
					}

					villager.setVillagerData(getVillagerData());

					if (gossip != null) {
						villager.readGossipData(gossip);
					}

					if (offerData != null) {
						villager.setOffers(offerData.copy());
					}

					villager.setExperience(experience);
					villager.initialize(world, world.getLocalDifficulty(villager.getBlockPos()), SpawnReason.CONVERSION, null);
					villager.reinitializeBrain(world);

					if (converter != null) {
						PlayerEntity converterPlayer = world.getPlayerByUuid(converter);

						if (converterPlayer instanceof ServerPlayerEntity serverPlayer) {
							Criteria.CURED_ZOMBIE_VILLAGER.trigger(serverPlayer, this, villager);
							world.handleInteraction(EntityInteraction.ZOMBIE_VILLAGER_CURED, converterPlayer, villager);
						}
					}

					villager.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));

					if (!isSilent()) {
							world.syncWorldEvent(null, 1027, getBlockPos(), 0);
						}
				}
		);
	}

	@VisibleForTesting
	public void setConversionTimer(int conversionTimer) {
		this.conversionTimer = conversionTimer;
	}

	/**
	 * Вычисляет скорость убывания таймера конвертации. С вероятностью 1% проверяет
	 * до 14 блоков железных прутьев и кроватей в радиусе 4 блоков — каждый с шансом 30%
	 * увеличивает скорость на 1 (ускоряет исцеление).
	 */
	private int getConversionRate() {
		int rate = 1;

		if (random.nextFloat() >= 0.01F) {
			return rate;
		}

		int boostCount = 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int x = (int) getX() - CONVERSION_BOOST_RADIUS; x < (int) getX() + CONVERSION_BOOST_RADIUS && boostCount < MAX_CONVERSION_BOOST_BLOCKS; x++) {
			for (int y = (int) getY() - CONVERSION_BOOST_RADIUS; y < (int) getY() + CONVERSION_BOOST_RADIUS && boostCount < MAX_CONVERSION_BOOST_BLOCKS; y++) {
				for (int z = (int) getZ() - CONVERSION_BOOST_RADIUS; z < (int) getZ() + CONVERSION_BOOST_RADIUS && boostCount < MAX_CONVERSION_BOOST_BLOCKS; z++) {
					BlockState blockState = getEntityWorld().getBlockState(mutable.set(x, y, z));

					if (blockState.isOf(Blocks.IRON_BARS) || blockState.getBlock() instanceof BedBlock) {
						if (random.nextFloat() < 0.3F) {
							rate++;
						}

						boostCount++;
					}
				}
			}
		}

		return rate;
	}

	@Override
	public float getSoundPitch() {
		return isBaby()
				? (random.nextFloat() - random.nextFloat()) * 0.2F + 2.0F
				: (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F;
	}

	@Override
	public SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_HURT;
	}

	@Override
	public SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_DEATH;
	}

	@Override
	public SoundEvent getStepSound() {
		return SoundEvents.ENTITY_ZOMBIE_VILLAGER_STEP;
	}

	public void setOfferData(TradeOfferList offerData) {
		this.offerData = offerData;
	}

	public void setGossip(VillagerGossips gossip) {
		this.gossip = gossip;
	}

	@Override
	public void setVillagerData(VillagerData villagerData) {
		VillagerData current = getVillagerData();

		if (!current.profession().equals(villagerData.profession())) {
			offerData = null;
		}

		dataTracker.set(VILLAGER_DATA, villagerData);
	}

	@Override
	public VillagerData getVillagerData() {
		return dataTracker.get(VILLAGER_DATA);
	}

	public int getExperience() {
		return experience;
	}

	public void setExperience(int experience) {
		this.experience = experience;
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.VILLAGER_VARIANT
				? castComponentValue((ComponentType<T>) type, getVillagerData().type())
				: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.VILLAGER_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.VILLAGER_VARIANT) {
			RegistryEntry<VillagerType> villagerType = castComponentValue(DataComponentTypes.VILLAGER_VARIANT, value);
			setVillagerData(getVillagerData().withType(villagerType));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}
}
