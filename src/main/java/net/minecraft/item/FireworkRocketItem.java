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
 * {@code FireworkRocketItem}.
 */
public class FireworkRocketItem extends Item implements ProjectileItem {

	public static final byte[] FLIGHT_VALUES = new byte[]{1, 2, 3};
	public static final double OFFSET_POS_MULTIPLIER = 0.15;

	public FireworkRocketItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity playerEntity = context.getPlayer();
		if (playerEntity != null && playerEntity.isGliding()) {
			return ActionResult.PASS;
		}
		else {
			if (world instanceof ServerWorld serverWorld) {
				ItemStack itemStack = context.getStack();
				Vec3d vec3d = context.getHitPos();
				Direction direction = context.getSide();
				ProjectileEntity.spawn(
						new FireworkRocketEntity(
								world,
								context.getPlayer(),
								vec3d.x + direction.getOffsetX() * 0.15,
								vec3d.y + direction.getOffsetY() * 0.15,
								vec3d.z + direction.getOffsetZ() * 0.15,
								itemStack
						),
						serverWorld,
						itemStack
				);
				itemStack.decrement(1);
			}

			return ActionResult.SUCCESS;
		}
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		if (user.isGliding()) {
			ItemStack itemStack = user.getStackInHand(hand);
			if (world instanceof ServerWorld serverWorld) {
				if (user.detachAllHeldLeashes(null)) {
					world.playSoundFromEntity(
							null,
							user,
							SoundEvents.ITEM_LEAD_BREAK,
							SoundCategory.NEUTRAL,
							1.0F,
							1.0F
					);
				}

				ProjectileEntity.spawn(new FireworkRocketEntity(world, itemStack, user), serverWorld, itemStack);
				itemStack.decrementUnlessCreative(1, user);
				user.incrementStat(Stats.USED.getOrCreateStat(this));
			}

			return ActionResult.SUCCESS;
		}
		else {
			return ActionResult.PASS;
		}
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		return new FireworkRocketEntity(world, stack.copyWithCount(1), pos.getX(), pos.getY(), pos.getZ(), true);
	}

	@Override
	public ProjectileItem.Settings getProjectileSettings() {
		return ProjectileItem.Settings
				.builder()
				.positionFunction(FireworkRocketItem::position)
				.uncertainty(1.0F)
				.power(0.5F)
				.overrideDispenseEvent(1004)
				.build();
	}

	private static Vec3d position(BlockPointer pointer, Direction facing) {
		return pointer.centerPos()
		              .add(
				              facing.getOffsetX() * 0.5000099999997474,
				              facing.getOffsetY() * 0.5000099999997474,
				              facing.getOffsetZ() * 0.5000099999997474
		              );
	}
}
