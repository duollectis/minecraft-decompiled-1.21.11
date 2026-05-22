package net.minecraft.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.function.Consumer;

/**
 * Предмет, размещающий стойку для брони при использовании на блоке.
 * Стойка ориентируется по направлению взгляда игрока с шагом 45 градусов.
 */
public class ArmorStandItem extends Item {

	private static final float YAW_STEP_DEGREES = 45.0F;
	private static final float YAW_HALF_STEP_DEGREES = 22.5F;
	private static final float PLACE_SOUND_VOLUME = 0.75F;
	private static final float PLACE_SOUND_PITCH = 0.8F;

	public ArmorStandItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (context.getSide() == Direction.DOWN) {
			return ActionResult.FAIL;
		}

		World world = context.getWorld();
		ItemPlacementContext placementContext = new ItemPlacementContext(context);
		BlockPos blockPos = placementContext.getBlockPos();
		ItemStack stack = context.getStack();
		Vec3d spawnPos = Vec3d.ofBottomCenter(blockPos);
		Box box = EntityType.ARMOR_STAND.getDimensions().getBoxAt(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

		if (world.isSpaceEmpty(null, box) && world.getOtherEntities(null, box).isEmpty()) {
			if (world instanceof ServerWorld serverWorld) {
				Consumer<ArmorStandEntity> entityCopier = EntityType.copier(serverWorld, stack, context.getPlayer());
				ArmorStandEntity armorStand = EntityType.ARMOR_STAND.create(
					serverWorld,
					entityCopier,
					blockPos,
					SpawnReason.SPAWN_ITEM_USE,
					true,
					true
				);

				if (armorStand == null) {
					return ActionResult.FAIL;
				}

				float yaw = MathHelper.floor(
					(MathHelper.wrapDegrees(context.getPlayerYaw() - 180.0F) + YAW_HALF_STEP_DEGREES) / YAW_STEP_DEGREES
				) * YAW_STEP_DEGREES;

				armorStand.refreshPositionAndAngles(
					armorStand.getX(),
					armorStand.getY(),
					armorStand.getZ(),
					yaw,
					0.0F
				);
				serverWorld.spawnEntityAndPassengers(armorStand);
				world.playSound(
					null,
					armorStand.getX(),
					armorStand.getY(),
					armorStand.getZ(),
					SoundEvents.ENTITY_ARMOR_STAND_PLACE,
					SoundCategory.BLOCKS,
					PLACE_SOUND_VOLUME,
					PLACE_SOUND_PITCH
				);
				armorStand.emitGameEvent(GameEvent.ENTITY_PLACE, context.getPlayer());
			}

			stack.decrement(1);
			return ActionResult.SUCCESS;
		}

		return ActionResult.FAIL;
	}
}
