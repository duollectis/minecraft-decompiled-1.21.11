package net.minecraft.entity.passive;

import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Лошадь — верховое животное с уникальной комбинацией цвета и маркировки.
 * <p>
 * Вариант лошади упакован в один {@code int}: младший байт — цвет ({@link HorseColor}),
 * старший байт — маркировка ({@link HorseMarking}). При скрещивании двух лошадей
 * цвет наследуется с вероятностью 4/9 от каждого родителя и 1/9 случайно;
 * маркировка — 2/5, 2/5 и 1/5 соответственно. Лошадь также может скрещиваться
 * с ослом, порождая мула.
 */
public class HorseEntity extends AbstractHorseEntity {

	private static final int COLOR_MASK = 0xFF;
	private static final int MARKING_MASK = 0xFF00;
	private static final int MARKING_SHIFT = 8;
	private static final int COLOR_ROLL_PARENT_A_MAX = 4;
	private static final int COLOR_ROLL_PARENT_B_MAX = 8;
	private static final int COLOR_ROLL_TOTAL = 9;
	private static final int MARKING_ROLL_PARENT_A_MAX = 2;
	private static final int MARKING_ROLL_PARENT_B_MAX = 4;
	private static final int MARKING_ROLL_TOTAL = 5;

	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(
		HorseEntity.class,
		TrackedDataHandlerRegistry.INTEGER
	);
	private static final EntityDimensions BABY_BASE_DIMENSIONS = EntityType.HORSE
		.getDimensions()
		.withAttachments(
			EntityAttachments.builder()
				.add(EntityAttachmentType.PASSENGER, 0.0F, EntityType.HORSE.getHeight() + 0.125F, 0.0F)
		)
		.scaled(0.5F);

	public HorseEntity(EntityType<? extends HorseEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.DANGER_OTHER, -1.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_OTHER, -1.0F);
	}

	@Override
	protected void initAttributes(Random random) {
		getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(getChildHealthBonus(random::nextInt));
		getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(getChildMovementSpeedBonus(random::nextDouble));
		getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(getChildJumpStrengthBonus(random::nextDouble));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, 0);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putInt("Variant", getHorseVariant());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setHorseVariant(view.getInt("Variant", 0));
	}

	private void setHorseVariant(int variant) {
		dataTracker.set(VARIANT, variant);
	}

	private int getHorseVariant() {
		return dataTracker.get(VARIANT);
	}

	/**
	 * Упаковывает цвет и маркировку в один {@code int}: цвет — в младший байт,
	 * маркировка — в старший байт.
	 */
	private void setHorseVariant(HorseColor color, HorseMarking marking) {
		setHorseVariant(color.getIndex() & COLOR_MASK | marking.getIndex() << MARKING_SHIFT & MARKING_MASK);
	}

	public HorseColor getHorseColor() {
		return HorseColor.byIndex(getHorseVariant() & COLOR_MASK);
	}

	private void setHorseColor(HorseColor color) {
		setHorseVariant(color.getIndex() & COLOR_MASK | getHorseVariant() & ~COLOR_MASK);
	}

	public HorseMarking getMarking() {
		return HorseMarking.byIndex((getHorseVariant() & MARKING_MASK) >> MARKING_SHIFT);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.HORSE_VARIANT
			? castComponentValue((ComponentType<T>) type, getHorseColor())
			: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.HORSE_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.HORSE_VARIANT) {
			setHorseColor(castComponentValue(DataComponentTypes.HORSE_VARIANT, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	@Override
	protected void playWalkSound(BlockSoundGroup group) {
		super.playWalkSound(group);
		if (random.nextInt(10) == 0) {
			playSound(SoundEvents.ENTITY_HORSE_BREATHE, group.getVolume() * 0.6F, group.getPitch());
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_HORSE_DEATH;
	}

	@Override
	protected SoundEvent getEatSound() {
		return SoundEvents.ENTITY_HORSE_EAT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_HORSE_HURT;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.ENTITY_HORSE_ANGRY;
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		boolean wantsToOpenInventory = !isBaby() && isTame() && player.shouldCancelInteraction();
		if (hasPassengers() || wantsToOpenInventory) {
			return super.interactMob(player, hand);
		}

		ItemStack stack = player.getStackInHand(hand);
		if (!stack.isEmpty()) {
			if (isBreedingItem(stack)) {
				return interactHorse(player, stack);
			}

			if (!isTame()) {
				playAngrySound();
				return ActionResult.SUCCESS;
			}
		}

		return super.interactMob(player, hand);
	}

	/**
	 * Лошадь может скрещиваться с другой лошадью или ослом (для создания мула).
	 * Скрещивание с самим собой запрещено.
	 */
	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		}

		if (other instanceof DonkeyEntity donkey) {
			return canBreed() && donkey.canBreed();
		}

		if (other instanceof HorseEntity otherHorse) {
			return canBreed() && otherHorse.canBreed();
		}

		return false;
	}

	/**
	 * Создаёт потомка. При скрещивании с ослом — мул, при скрещивании с лошадью —
	 * жеребёнок с унаследованными цветом и маркировкой.
	 * <p>
	 * Цвет: 4/9 от первого родителя, 4/9 от второго, 1/9 случайный.
	 * Маркировка: 2/5 от первого, 2/5 от второго, 1/5 случайная.
	 */
	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		if (entity instanceof DonkeyEntity) {
			MuleEntity mule = EntityType.MULE.create(world, SpawnReason.BREEDING);
			if (mule != null) {
				setChildAttributes(entity, mule);
			}

			return mule;
		}

		HorseEntity otherHorse = (HorseEntity) entity;
		HorseEntity child = EntityType.HORSE.create(world, SpawnReason.BREEDING);
		if (child == null) {
			return null;
		}

		int colorRoll = random.nextInt(COLOR_ROLL_TOTAL);
		HorseColor color;
		if (colorRoll < COLOR_ROLL_PARENT_A_MAX) {
			color = getHorseColor();
		} else if (colorRoll < COLOR_ROLL_PARENT_B_MAX) {
			color = otherHorse.getHorseColor();
		} else {
			color = Util.getRandom(HorseColor.values(), random);
		}

		int markingRoll = random.nextInt(MARKING_ROLL_TOTAL);
		HorseMarking marking;
		if (markingRoll < MARKING_ROLL_PARENT_A_MAX) {
			marking = getMarking();
		} else if (markingRoll < MARKING_ROLL_PARENT_B_MAX) {
			marking = otherHorse.getMarking();
		} else {
			marking = Util.getRandom(HorseMarking.values(), random);
		}

		child.setHorseVariant(color, marking);
		setChildAttributes(entity, child);

		return child;
	}

	@Override
	public boolean canUseSlot(EquipmentSlot slot) {
		return true;
	}

	@Override
	public void damageArmor(DamageSource source, float amount) {
		damageEquipment(source, amount, EquipmentSlot.BODY);
	}

	@Override
	public @Nullable EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		Random random = world.getRandom();
		HorseColor color;
		if (entityData instanceof HorseData horseData) {
			color = horseData.color;
		} else {
			color = Util.getRandom(HorseColor.values(), random);
			entityData = new HorseData(color);
		}

		setHorseVariant(color, Util.getRandom(HorseMarking.values(), random));
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? BABY_BASE_DIMENSIONS : super.getBaseDimensions(pose);
	}

	/**
	 * Данные спавна лошади. Хранит фиксированный цвет для всего табуна,
	 * чтобы лошади в одной группе имели одинаковый окрас.
	 */
	public static class HorseData extends PassiveEntity.PassiveData {

		public final HorseColor color;

		public HorseData(HorseColor color) {
			super(true);
			this.color = color;
		}
	}
}
