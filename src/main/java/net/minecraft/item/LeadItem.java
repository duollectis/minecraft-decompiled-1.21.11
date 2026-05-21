package net.minecraft.item;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

/**
 * {@code LeadItem}.
 */
public class LeadItem extends Item {

	public LeadItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos blockPos = context.getBlockPos();
		BlockState blockState = world.getBlockState(blockPos);
		if (blockState.isIn(BlockTags.FENCES)) {
			PlayerEntity playerEntity = context.getPlayer();
			if (!world.isClient() && playerEntity != null) {
				return attachHeldMobsToBlock(playerEntity, world, blockPos);
			}
		}

		return ActionResult.PASS;
	}

	public static ActionResult attachHeldMobsToBlock(PlayerEntity player, World world, BlockPos pos) {
		LeashKnotEntity leashKnotEntity = null;
		List<Leashable>
				list =
				Leashable.collectLeashablesAround(
						world,
						Vec3d.ofCenter(pos),
						entity -> entity.getLeashHolder() == player
				);
		boolean bl = false;

		for (Leashable leashable : list) {
			if (leashKnotEntity == null) {
				leashKnotEntity = LeashKnotEntity.getOrCreate(world, pos);
				leashKnotEntity.onPlace();
			}

			if (leashable.canBeLeashedTo(leashKnotEntity)) {
				leashable.attachLeash(leashKnotEntity, true);
				bl = true;
			}
		}

		if (bl) {
			world.emitGameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Emitter.of(player));
			return ActionResult.SUCCESS_SERVER;
		}
		else {
			return ActionResult.PASS;
		}
	}
}
