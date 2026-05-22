package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Ведро с живой сущностью (рыбой, аксолотлем и т.д.).
 * При выливании спавнит сущность и восстанавливает её сохранённые данные.
 */
public class EntityBucketItem extends BucketItem {

	private final EntityType<? extends MobEntity> entityType;
	private final SoundEvent emptyingSound;

	public EntityBucketItem(
		EntityType<? extends MobEntity> type,
		Fluid fluid,
		SoundEvent emptyingSound,
		Item.Settings settings
	) {
		super(fluid, settings);
		this.entityType = type;
		this.emptyingSound = emptyingSound;
	}

	@Override
	public void onEmptied(@Nullable LivingEntity user, World world, ItemStack stack, BlockPos pos) {
		if (world instanceof ServerWorld serverWorld) {
			spawnEntity(serverWorld, stack, pos);
			world.emitGameEvent(user, GameEvent.ENTITY_PLACE, pos);
		}
	}

	@Override
	protected void playEmptyingSound(@Nullable LivingEntity user, WorldAccess world, BlockPos pos) {
		world.playSound(user, pos, emptyingSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
	}

	private void spawnEntity(ServerWorld world, ItemStack stack, BlockPos pos) {
		MobEntity mob = entityType.create(
			world,
			EntityType.copier(world, stack, null),
			pos,
			SpawnReason.BUCKET,
			true,
			false
		);

		if (mob instanceof Bucketable bucketable) {
			NbtComponent nbtData = stack.getOrDefault(DataComponentTypes.BUCKET_ENTITY_DATA, NbtComponent.DEFAULT);
			bucketable.copyDataFromNbt(nbtData.copyNbt());
			bucketable.setFromBucket(true);
		}

		if (mob != null) {
			world.spawnEntityAndPassengers(mob);
			mob.playAmbientSound();
		}
	}
}
