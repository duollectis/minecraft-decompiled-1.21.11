package net.minecraft.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.LingeringPotionEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

/**
 * Предмет «Длительное зелье». Расширяет {@link ThrowablePotionItem}, создавая
 * при броске сущность {@link LingeringPotionEntity}, которая оставляет на земле
 * облако эффектов.
 */
public class LingeringPotionItem extends ThrowablePotionItem {

	public LingeringPotionItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		world.playSound(
			null,
			user.getX(),
			user.getY(),
			user.getZ(),
			SoundEvents.ENTITY_LINGERING_POTION_THROW,
			SoundCategory.NEUTRAL,
			0.5F,
			0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
		);

		return super.use(world, user, hand);
	}

	@Override
	protected PotionEntity createEntity(ServerWorld world, LivingEntity user, ItemStack stack) {
		return new LingeringPotionEntity(world, user, stack);
	}

	@Override
	protected PotionEntity createEntity(World world, Position pos, ItemStack stack) {
		return new LingeringPotionEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
	}
}
