package net.minecraft.item;

import net.minecraft.block.*;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Предмет «Огненный заряд». Поджигает костры, свечи и блоки огня.
 * При выстреле из диспенсера запускает снаряд {@link SmallFireballEntity}.
 */
public class FireChargeItem extends Item implements ProjectileItem {

	/**
	 * Разброс направления снаряда при создании через диспенсер.
	 * Значение соответствует стандартному разбросу огненного шара.
	 */
	private static final double FIREBALL_SPREAD = 0.11485000000000001;

	/** Неопределённость траектории при выстреле из диспенсера. */
	private static final float DISPENSER_UNCERTAINTY = 6.6666665F;

	/** ID события диспенсера для звука выстрела огненным зарядом. */
	private static final int DISPENSE_EVENT_ID = 1018;

	public FireChargeItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Поджигает блок при использовании. Если блок — костёр или свеча, включает его.
	 * Иначе пытается разместить огонь на соседнем блоке.
	 */
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);
		boolean didIgnite;

		if (CampfireBlock.canBeLit(blockState)
				|| CandleBlock.canBeLit(blockState)
				|| CandleCakeBlock.canBeLit(blockState)
		) {
			playUseSound(world, pos);
			world.setBlockState(pos, blockState.with(Properties.LIT, true));
			world.emitGameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, pos);
			didIgnite = true;
		} else {
			BlockPos firePos = pos.offset(context.getSide());

			if (AbstractFireBlock.canPlaceAt(world, firePos, context.getHorizontalPlayerFacing())) {
				playUseSound(world, firePos);
				world.setBlockState(firePos, AbstractFireBlock.getState(world, firePos));
				world.emitGameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, firePos);
				didIgnite = true;
			} else {
				didIgnite = false;
			}
		}

		if (didIgnite) {
			context.getStack().decrement(1);
			return ActionResult.SUCCESS;
		}

		return ActionResult.FAIL;
	}

	private void playUseSound(World world, BlockPos pos) {
		Random random = world.getRandom();
		world.playSound(
				null,
				pos,
				SoundEvents.ITEM_FIRECHARGE_USE,
				SoundCategory.BLOCKS,
				1.0F,
				(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
		);
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		Random random = world.getRandom();
		double velX = random.nextTriangular(direction.getOffsetX(), FIREBALL_SPREAD);
		double velY = random.nextTriangular(direction.getOffsetY(), FIREBALL_SPREAD);
		double velZ = random.nextTriangular(direction.getOffsetZ(), FIREBALL_SPREAD);
		Vec3d velocity = new Vec3d(velX, velY, velZ);
		SmallFireballEntity fireball = new SmallFireballEntity(world, pos.getX(), pos.getY(), pos.getZ(), velocity.normalize());
		fireball.setItem(stack);
		return fireball;
	}

	@Override
	public void initializeProjectile(ProjectileEntity entity, double x, double y, double z, float power, float uncertainty) {
	}

	@Override
	public ProjectileItem.Settings getProjectileSettings() {
		return ProjectileItem.Settings.builder()
				.positionFunction((pointer, facing) -> DispenserBlock.getOutputLocation(pointer, 1.0, Vec3d.ZERO))
				.uncertainty(DISPENSER_UNCERTAINTY)
				.power(1.0F)
				.overrideDispenseEvent(DISPENSE_EVENT_ID)
				.build();
	}
}
