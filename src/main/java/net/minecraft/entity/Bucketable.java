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
 * Маркерный интерфейс для существ, которых можно поймать в ведро с водой.
 * Реализующие классы обязаны хранить флаг {@code fromBucket} и уметь
 * сериализовать/десериализовать свои данные в/из {@link net.minecraft.item.ItemStack}.
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
			DataComponentTypes.BUCKET_ENTITY_DATA, stack, nbt -> {
				if (entity.isAiDisabled()) {
					nbt.putBoolean("NoAI", entity.isAiDisabled());
				}

				if (entity.isSilent()) {
					nbt.putBoolean("Silent", entity.isSilent());
				}

				if (entity.hasNoGravity()) {
					nbt.putBoolean("NoGravity", entity.hasNoGravity());
				}

				if (entity.isGlowingLocal()) {
					nbt.putBoolean("Glowing", entity.isGlowingLocal());
				}

				if (entity.isInvulnerable()) {
					nbt.putBoolean("Invulnerable", entity.isInvulnerable());
				}

				nbt.putFloat("Health", entity.getHealth());
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

	/**
	 * Пытается поймать сущность в ведро с водой.
	 * Срабатывает только если игрок держит ведро с водой и сущность жива.
	 * После поимки сущность удаляется из мира, а ведро заменяется на ведро с существом.
	 *
	 * @param player игрок, взаимодействующий с сущностью
	 * @param hand   рука, в которой держится ведро
	 * @param entity целевая сущность
	 * @return результат взаимодействия, или {@link Optional#empty()} если условия не выполнены
	 */
	static <T extends LivingEntity & Bucketable> Optional<ActionResult> tryBucket(
		PlayerEntity player,
		Hand hand,
		T entity
	) {
		ItemStack heldStack = player.getStackInHand(hand);
		if (heldStack.getItem() != Items.WATER_BUCKET || !entity.isAlive()) {
			return Optional.empty();
		}

		entity.playSound(entity.getBucketFillSound(), 1.0F, 1.0F);
		ItemStack bucketItem = entity.getBucketItem();
		entity.copyDataToStack(bucketItem);
		ItemStack resultStack = ItemUsage.exchangeStack(heldStack, player, bucketItem, false);
		player.setStackInHand(hand, resultStack);

		World world = entity.getEntityWorld();
		if (!world.isClient()) {
			Criteria.FILLED_BUCKET.trigger((ServerPlayerEntity) player, bucketItem);
		}

		entity.discard();
		return Optional.of(ActionResult.SUCCESS);
	}
}
