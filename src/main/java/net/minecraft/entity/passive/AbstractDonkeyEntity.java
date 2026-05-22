package net.minecraft.entity.passive;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для осла и мула. Добавляет поддержку сундука:
 * при наличии сундука инвентарь расширяется до 5 колонок (15 слотов).
 * Сундук можно навесить в живую, взаимодействуя с предметом {@link Items#CHEST}.
 */
public abstract class AbstractDonkeyEntity extends AbstractHorseEntity {

	private static final int CHEST_SLOT_ID = 499;
	private static final int INVENTORY_COLUMNS_WITH_CHEST = 5;

	private static final TrackedData<Boolean> CHEST = DataTracker.registerData(
		AbstractDonkeyEntity.class,
		TrackedDataHandlerRegistry.BOOLEAN
	);

	private final EntityDimensions babyBaseDimensions;

	protected AbstractDonkeyEntity(EntityType<? extends AbstractDonkeyEntity> entityType, World world) {
		super(entityType, world);
		playExtraHorseSounds = false;
		babyBaseDimensions = entityType.getDimensions()
			.withAttachments(
				EntityAttachments.builder()
					.add(EntityAttachmentType.PASSENGER, 0.0F, entityType.getHeight() - 0.15625F, 0.0F)
			)
			.scaled(0.5F);
	}

	@Override
	protected void initAttributes(Random random) {
		getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(getChildHealthBonus(random::nextInt));
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(CHEST, false);
	}

	public static DefaultAttributeContainer.Builder createAbstractDonkeyAttributes() {
		return createBaseHorseAttributes()
			.add(EntityAttributes.MOVEMENT_SPEED, 0.175F)
			.add(EntityAttributes.JUMP_STRENGTH, 0.5);
	}

	public boolean hasChest() {
		return dataTracker.get(CHEST);
	}

	public void setHasChest(boolean hasChest) {
		dataTracker.set(CHEST, hasChest);
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		return isBaby() ? babyBaseDimensions : super.getBaseDimensions(pose);
	}

	@Override
	protected void dropInventory(ServerWorld world) {
		super.dropInventory(world);
		if (hasChest()) {
			dropItem(world, Blocks.CHEST);
			setHasChest(false);
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("ChestedHorse", hasChest());
		if (!hasChest()) {
			return;
		}

		WriteView.ListAppender<StackWithSlot> listAppender = view.getListAppender("Items", StackWithSlot.CODEC);
		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack stack = items.getStack(slot);
			if (!stack.isEmpty()) {
				listAppender.add(new StackWithSlot(slot, stack));
			}
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setHasChest(view.getBoolean("ChestedHorse", false));
		onChestedStatusChanged();
		if (!hasChest()) {
			return;
		}

		for (StackWithSlot stackWithSlot : view.getTypedListView("Items", StackWithSlot.CODEC)) {
			if (stackWithSlot.isValidSlot(items.size())) {
				items.setStack(stackWithSlot.slot(), stackWithSlot.stack());
			}
		}
	}

	/**
	 * Предоставляет виртуальный слот 499 для управления сундуком через интерфейс контейнера.
	 * Установка пустого стека снимает сундук, установка {@link Items#CHEST} — навешивает.
	 */
	@Override
	public @Nullable StackReference getStackReference(int slot) {
		if (slot != CHEST_SLOT_ID) {
			return super.getStackReference(slot);
		}

		return new StackReference() {
			@Override
			public ItemStack get() {
				return hasChest() ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
			}

			@Override
			public boolean set(ItemStack stack) {
				if (stack.isEmpty()) {
					if (hasChest()) {
						setHasChest(false);
						onChestedStatusChanged();
					}

					return true;
				}

				if (stack.isOf(Items.CHEST)) {
					if (!hasChest()) {
						setHasChest(true);
						onChestedStatusChanged();
					}

					return true;
				}

				return false;
			}
		};
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

			if (!hasChest() && stack.isOf(Items.CHEST)) {
				addChest(player, stack);
				return ActionResult.SUCCESS;
			}
		}

		return super.interactMob(player, hand);
	}

	private void addChest(PlayerEntity player, ItemStack chest) {
		setHasChest(true);
		playAddChestSound();
		chest.decrementUnlessCreative(1, player);
		onChestedStatusChanged();
	}

	@Override
	public Vec3d[] getQuadLeashOffsets() {
		return Leashable.createQuadLeashOffsets(this, 0.04, 0.41, 0.18, 0.73);
	}

	protected void playAddChestSound() {
		playSound(
			SoundEvents.ENTITY_DONKEY_CHEST,
			1.0F,
			(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
		);
	}

	@Override
	public int getInventoryColumns() {
		return hasChest() ? INVENTORY_COLUMNS_WITH_CHEST : 0;
	}
}
