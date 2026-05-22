package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Предмет, который можно разместить на поверхности воды. Переопределяет
 * {@link #useOnBlock} чтобы запретить прямое взаимодействие с блоком, и
 * перенаправляет {@link #use} на размещение блока над поверхностью воды
 * через рейкаст с учётом источников жидкости.
 */
public class PlaceableOnWaterItem extends BlockItem {

	public PlaceableOnWaterItem(Block block, Item.Settings settings) {
		super(block, settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		return ActionResult.PASS;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		BlockHitResult waterHit = raycast(world, user, RaycastContext.FluidHandling.SOURCE_ONLY);
		BlockHitResult aboveWater = waterHit.withBlockPos(waterHit.getBlockPos().up());

		return super.useOnBlock(new ItemUsageContext(user, hand, aboveWater));
	}
}
