package net.minecraft.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

/**
 * Предмет «Кристалл Края». Размещается на обсидиане или бедроке в Крае,
 * после чего может возродить Дракона Края при наличии полного кольца кристаллов.
 */
public class EndCrystalItem extends Item {

	public EndCrystalItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Размещает кристалл Края на блоке обсидиана или бедрока.
	 * Проверяет, что над блоком есть два свободных блока воздуха и нет других сущностей.
	 * При завершении кольца кристаллов инициирует возрождение Дракона Края.
	 */
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);

		if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK)) {
			return ActionResult.FAIL;
		}

		BlockPos spawnPos = pos.up();

		if (!world.isAir(spawnPos)) {
			return ActionResult.FAIL;
		}

		double spawnX = spawnPos.getX();
		double spawnY = spawnPos.getY();
		double spawnZ = spawnPos.getZ();
		List<Entity> blockingEntities = world.getOtherEntities(
				null,
				new Box(spawnX, spawnY, spawnZ, spawnX + 1.0, spawnY + 2.0, spawnZ + 1.0)
		);

		if (!blockingEntities.isEmpty()) {
			return ActionResult.FAIL;
		}

		if (world instanceof ServerWorld serverWorld) {
			EndCrystalEntity crystal = new EndCrystalEntity(world, spawnX + 0.5, spawnY, spawnZ + 0.5);
			crystal.setShowBottom(false);
			world.spawnEntity(crystal);
			world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);

			EnderDragonFight dragonFight = serverWorld.getEnderDragonFight();

			if (dragonFight != null) {
				dragonFight.respawnDragon();
			}
		}

		context.getStack().decrement(1);

		return ActionResult.SUCCESS;
	}
}
