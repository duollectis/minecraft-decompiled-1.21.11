package net.minecraft.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Предмет лодки. При использовании выполняет рейкаст по жидкостям и
 * спавнит соответствующую сущность лодки в точке попадания.
 */
public class BoatItem extends Item {

	private static final double ENTITY_SEARCH_RANGE = 5.0;

	private final EntityType<? extends AbstractBoatEntity> boatEntityType;

	public BoatItem(EntityType<? extends AbstractBoatEntity> boatEntityType, Item.Settings settings) {
		super(settings);
		this.boatEntityType = boatEntityType;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		HitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.ANY);

		if (hitResult.getType() == HitResult.Type.MISS) {
			return ActionResult.PASS;
		}

		Vec3d lookVec = user.getRotationVec(1.0F);
		List<Entity> nearbyEntities = world.getOtherEntities(
			user,
			user.getBoundingBox().stretch(lookVec.multiply(ENTITY_SEARCH_RANGE)).expand(1.0),
			EntityPredicates.CAN_HIT
		);

		if (!nearbyEntities.isEmpty()) {
			Vec3d eyePos = user.getEyePos();

			for (Entity entity : nearbyEntities) {
				Box box = entity.getBoundingBox().expand(entity.getTargetingMargin());

				if (box.contains(eyePos)) {
					return ActionResult.PASS;
				}
			}
		}

		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return ActionResult.PASS;
		}

		AbstractBoatEntity boat = createEntity(world, hitResult, stack, user);

		if (boat == null) {
			return ActionResult.FAIL;
		}

		boat.setYaw(user.getYaw());

		if (!world.isSpaceEmpty(boat, boat.getBoundingBox())) {
			return ActionResult.FAIL;
		}

		if (!world.isClient()) {
			world.spawnEntity(boat);
			world.emitGameEvent(user, GameEvent.ENTITY_PLACE, hitResult.getPos());
			stack.decrementUnlessCreative(1, user);
		}

		user.incrementStat(Stats.USED.getOrCreateStat(this));
		return ActionResult.SUCCESS;
	}

	private @Nullable AbstractBoatEntity createEntity(
		World world,
		HitResult hitResult,
		ItemStack stack,
		PlayerEntity player
	) {
		AbstractBoatEntity boat = boatEntityType.create(world, SpawnReason.SPAWN_ITEM_USE);

		if (boat == null) {
			return null;
		}

		Vec3d spawnPos = hitResult.getPos();
		boat.initPosition(spawnPos.x, spawnPos.y, spawnPos.z);

		if (world instanceof ServerWorld serverWorld) {
			EntityType.<AbstractBoatEntity>copier(serverWorld, stack, player).accept(boat);
		}

		return boat;
	}
}
