package net.minecraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Предмет «Фейерверк». Используется для полёта с элитрами или для запуска с поверхности.
 * При выстреле из диспенсера создаёт снаряд {@link FireworkRocketEntity}.
 */
public class FireworkRocketItem extends Item implements ProjectileItem {

	public static final byte[] FLIGHT_VALUES = new byte[]{1, 2, 3};

	/** Смещение позиции спавна ракеты от точки попадания при клике на блок. */
	public static final double OFFSET_POS_MULTIPLIER = 0.15;

	/**
	 * Небольшое смещение от центра диспенсера, чтобы ракета не застревала в блоке.
	 * Значение подобрано эмпирически для корректного вылета.
	 */
	private static final double DISPENSER_OFFSET = 0.5000099999997474;

	/** ID события диспенсера для звука запуска фейерверка. */
	private static final int DISPENSE_EVENT_ID = 1004;

	public FireworkRocketItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Запускает ракету при клике на блок. Недоступно во время полёта с элитрами
	 * (в этом случае используется {@link #use}).
	 */
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();

		if (player != null && player.isGliding()) {
			return ActionResult.PASS;
		}

		World world = context.getWorld();

		if (world instanceof ServerWorld serverWorld) {
			ItemStack stack = context.getStack();
			Vec3d hitPos = context.getHitPos();
			Direction side = context.getSide();

			ProjectileEntity.spawn(
					new FireworkRocketEntity(
							world,
							context.getPlayer(),
							hitPos.x + side.getOffsetX() * OFFSET_POS_MULTIPLIER,
							hitPos.y + side.getOffsetY() * OFFSET_POS_MULTIPLIER,
							hitPos.z + side.getOffsetZ() * OFFSET_POS_MULTIPLIER,
							stack
					),
					serverWorld,
					stack
			);

			stack.decrement(1);
		}

		return ActionResult.SUCCESS;
	}

	/**
	 * Запускает ракету для ускорения полёта на элитрах.
	 * Перед запуском отцепляет все поводки игрока.
	 */
	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (!user.isGliding()) {
			return ActionResult.PASS;
		}

		ItemStack stack = user.getStackInHand(hand);

		if (world instanceof ServerWorld serverWorld) {
			if (user.detachAllHeldLeashes(null)) {
				world.playSoundFromEntity(null, user, SoundEvents.ITEM_LEAD_BREAK, SoundCategory.NEUTRAL, 1.0F, 1.0F);
			}

			ProjectileEntity.spawn(new FireworkRocketEntity(world, stack, user), serverWorld, stack);
			stack.decrementUnlessCreative(1, user);
			user.incrementStat(Stats.USED.getOrCreateStat(this));
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		return new FireworkRocketEntity(world, stack.copyWithCount(1), pos.getX(), pos.getY(), pos.getZ(), true);
	}

	@Override
	public ProjectileItem.Settings getProjectileSettings() {
		return ProjectileItem.Settings.builder()
				.positionFunction(FireworkRocketItem::position)
				.uncertainty(1.0F)
				.power(0.5F)
				.overrideDispenseEvent(DISPENSE_EVENT_ID)
				.build();
	}

	private static Vec3d position(BlockPointer pointer, Direction facing) {
		return pointer.centerPos()
				.add(
						facing.getOffsetX() * DISPENSER_OFFSET,
						facing.getOffsetY() * DISPENSER_OFFSET,
						facing.getOffsetZ() * DISPENSER_OFFSET
				);
	}
}
