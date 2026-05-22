package net.minecraft.item;

import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Предмет «заряд ветра» — снаряд, создающий взрывную волну при попадании.
 * При использовании игроком запускает {@link WindChargeEntity} с фиксированной мощью.
 * При выстреле из диспенсера использует треугольное распределение для разброса.
 */
public class WindChargeItem extends Item implements ProjectileItem {

	public static final float POWER = 1.5F;
	private static final float DISPENSER_UNCERTAINTY = 6.6666665F;
	private static final int DISPENSE_EVENT_ID = 1051;
	/** Треугольное распределение разброса при выстреле из диспенсера. */
	private static final double DISPENSER_SPREAD = 0.11485000000000001;

	public WindChargeItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);
		if (world instanceof ServerWorld serverWorld) {
			ProjectileEntity.spawnWithVelocity(
					(world2, shooter, stack) -> new WindChargeEntity(
							user,
							world,
							user.getEntityPos().getX(),
							user.getEyePos().getY(),
							user.getEntityPos().getZ()
					),
					serverWorld,
					itemStack,
					user,
					0.0F,
					POWER,
					1.0F
			);
		}

		world.playSound(
				null,
				user.getX(),
				user.getY(),
				user.getZ(),
				SoundEvents.ENTITY_WIND_CHARGE_THROW,
				SoundCategory.NEUTRAL,
				0.5F,
				0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
		);
		user.incrementStat(Stats.USED.getOrCreateStat(this));
		itemStack.decrementUnlessCreative(1, user);
		return ActionResult.SUCCESS;
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		Random random = world.getRandom();
		Vec3d velocity = new Vec3d(
				random.nextTriangular(direction.getOffsetX(), DISPENSER_SPREAD),
				random.nextTriangular(direction.getOffsetY(), DISPENSER_SPREAD),
				random.nextTriangular(direction.getOffsetZ(), DISPENSER_SPREAD)
		);
		WindChargeEntity windChargeEntity = new WindChargeEntity(world, pos.getX(), pos.getY(), pos.getZ(), velocity);
		windChargeEntity.setVelocity(velocity);
		return windChargeEntity;
	}

	@Override
	public void initializeProjectile(
			ProjectileEntity entity,
			double x,
			double y,
			double z,
			float power,
			float uncertainty
	) {
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
