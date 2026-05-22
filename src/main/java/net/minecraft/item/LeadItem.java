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
 * Предмет «Поводок». При использовании на заборе привязывает всех существ,
 * которых игрок ведёт на поводке, к узлу поводка на этом блоке.
 */
public class LeadItem extends Item {

	public LeadItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);

		if (!blockState.isIn(BlockTags.FENCES)) {
			return ActionResult.PASS;
		}

		PlayerEntity player = context.getPlayer();

		if (world.isClient() || player == null) {
			return ActionResult.PASS;
		}

		return attachHeldMobsToBlock(player, world, pos);
	}

	/**
	 * Привязывает всех существ, которых ведёт {@code player}, к узлу поводка
	 * на позиции {@code pos}. Узел создаётся лениво — только при первом
	 * подходящем существе. Если хотя бы одно существо было привязано,
	 * испускает игровое событие {@link GameEvent#BLOCK_ATTACH}.
	 *
	 * @param player игрок, держащий поводки
	 * @param world мир, в котором происходит действие
	 * @param pos позиция блока забора
	 * @return {@link ActionResult#SUCCESS_SERVER} если привязано хотя бы одно существо,
	 *         иначе {@link ActionResult#PASS}
	 */
	public static ActionResult attachHeldMobsToBlock(PlayerEntity player, World world, BlockPos pos) {
		List<Leashable> leashedMobs = Leashable.collectLeashablesAround(
			world,
			Vec3d.ofCenter(pos),
			entity -> entity.getLeashHolder() == player
		);

		LeashKnotEntity knot = null;
		boolean didAttach = false;

		for (Leashable leashable : leashedMobs) {
			if (knot == null) {
				knot = LeashKnotEntity.getOrCreate(world, pos);
				knot.onPlace();
			}

			if (leashable.canBeLeashedTo(knot)) {
				leashable.attachLeash(knot, true);
				didAttach = true;
			}
		}

		if (didAttach) {
			world.emitGameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Emitter.of(player));
			return ActionResult.SUCCESS_SERVER;
		}

		return ActionResult.PASS;
	}
}
