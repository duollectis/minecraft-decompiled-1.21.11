package net.minecraft.entity;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * {@code Bucketable}.
 */
public interface Bucketable {

	boolean isFromBucket();

	void setFromBucket(boolean fromBucket);

	void copyDataToStack(ItemStack stack);

	void copyDataFromNbt(NbtCompound nbt);

	ItemStack getBucketItem();

	SoundEvent getBucketFillSound();

	@Deprecated
	static void copyDataToStack(MobEntity entity, ItemStack stack) {
		stack.copy(DataComponentTypes.CUSTOM_NAME, entity);
		NbtComponent.set(
				DataComponentTypes.BUCKET_ENTITY_DATA, stack, nbtCompound -> {
					if (entity.isAiDisabled()) {
						nbtCompound.putBoolean("NoAI", entity.isAiDisabled());
					}

					if (entity.isSilent()) {
						nbtCompound.putBoolean("Silent", entity.isSilent());
					}

					if (entity.hasNoGravity()) {
						nbtCompound.putBoolean("NoGravity", entity.hasNoGravity());
					}

					if (entity.isGlowingLocal()) {
						nbtCompound.putBoolean("Glowing", entity.isGlowingLocal());
					}

					if (entity.isInvulnerable()) {
						nbtCompound.putBoolean("Invulnerable", entity.isInvulnerable());
					}

					nbtCompound.putFloat("Health", entity.getHealth());
				}
		);
	}

	@Deprecated
	static void copyDataFromNbt(MobEntity entity, NbtCompound nbt) {
		nbt.getBoolean("NoAI").ifPresent(entity::setAiDisabled);
		nbt.getBoolean("Silent").ifPresent(entity::setSilent);
		nbt.getBoolean("NoGravity").ifPresent(entity::setNoGravity);
		nbt.getBoolean("Glowing").ifPresent(entity::setGlowing);
		nbt.getBoolean("Invulnerable").ifPresent(entity::setInvulnerable);
		nbt.getFloat("Health").ifPresent(entity::setHealth);
	}

	static <T extends LivingEntity & Bucketable> Optional<ActionResult> tryBucket(
			PlayerEntity player,
			Hand hand,
			T entity
	) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (itemStack.getItem() == Items.WATER_BUCKET && entity.isAlive()) {
			entity.playSound(entity.getBucketFillSound(), 1.0F, 1.0F);
			ItemStack itemStack2 = entity.getBucketItem();
			entity.copyDataToStack(itemStack2);
			ItemStack itemStack3 = ItemUsage.exchangeStack(itemStack, player, itemStack2, false);
			player.setStackInHand(hand, itemStack3);
			World world = entity.getEntityWorld();
			if (!world.isClient()) {
				Criteria.FILLED_BUCKET.trigger((ServerPlayerEntity) player, itemStack2);
			}

			entity.discard();
			return Optional.of(ActionResult.SUCCESS);
		}
		else {
			return Optional.empty();
		}
	}
}
