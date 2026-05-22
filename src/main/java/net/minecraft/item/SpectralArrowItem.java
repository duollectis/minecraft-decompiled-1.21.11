package net.minecraft.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Стрела-призрак, накладывающая эффект свечения на поражённую цель.
 * При выстреле из диспенсера создаёт стрелу с разрешённым подбором.
 */
public class SpectralArrowItem extends ArrowItem {

	public SpectralArrowItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public PersistentProjectileEntity createArrow(
			World world,
			ItemStack stack,
			LivingEntity shooter,
			@Nullable ItemStack shotFrom
	) {
		return new SpectralArrowEntity(world, shooter, stack.copyWithCount(1), shotFrom);
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		SpectralArrowEntity arrow = new SpectralArrowEntity(
			world,
			pos.getX(),
			pos.getY(),
			pos.getZ(),
			stack.copyWithCount(1),
			null
		);
		arrow.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
		return arrow;
	}
}
