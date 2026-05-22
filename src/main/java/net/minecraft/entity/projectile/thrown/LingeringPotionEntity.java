package net.minecraft.entity.projectile.thrown;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * Длительное зелье — при взрыве создаёт {@link AreaEffectCloudEntity},
 * постепенно применяющее эффекты ко всем сущностям в радиусе.
 * <p>
 * Облако начинает с радиуса {@link #CLOUD_INITIAL_RADIUS} и уменьшается
 * до нуля за время {@link AreaEffectCloudEntity#DEFAULT_LINGERING_DURATION} тиков.
 */
public class LingeringPotionEntity extends PotionEntity {

	private static final float CLOUD_INITIAL_RADIUS = 3.0F;
	private static final float CLOUD_RADIUS_ON_USE = -0.5F;
	private static final int CLOUD_WAIT_TIME = 10;

	public LingeringPotionEntity(EntityType<? extends LingeringPotionEntity> entityType, World world) {
		super(entityType, world);
	}

	public LingeringPotionEntity(World world, LivingEntity owner, ItemStack stack) {
		super(EntityType.LINGERING_POTION, world, owner, stack);
	}

	public LingeringPotionEntity(World world, double x, double y, double z, ItemStack stack) {
		super(EntityType.LINGERING_POTION, world, x, y, z, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return Items.LINGERING_POTION;
	}

	@Override
	public void spawnAreaEffectCloud(ServerWorld world, ItemStack stack, HitResult hitResult) {
		AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(getEntityWorld(), getX(), getY(), getZ());

		if (getOwner() instanceof LivingEntity livingOwner) {
			cloud.setOwner(livingOwner);
		}

		cloud.setRadius(CLOUD_INITIAL_RADIUS);
		cloud.setRadiusOnUse(CLOUD_RADIUS_ON_USE);
		cloud.setDuration(AreaEffectCloudEntity.DEFAULT_LINGERING_DURATION);
		cloud.setWaitTime(CLOUD_WAIT_TIME);
		cloud.setRadiusGrowth(-cloud.getRadius() / cloud.getDuration());
		cloud.copyComponentsFrom(stack);
		world.spawnEntity(cloud);
	}
}
