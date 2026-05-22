package net.minecraft.block.dispenser;

import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Shearable;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.event.GameEvent;

/**
 * Поведение диспенсера для ножниц: стрижёт улей или существо перед диспенсером.
 * При успехе наносит 1 единицу урона ножницам.
 */
public class ShearsDispenserBehavior extends FallibleItemDispenserBehavior {

	/** Урон ножницам при каждом успешном использовании через диспенсер. */
	private static final int SHEARS_DAMAGE_PER_USE = 1;

	@Override
	protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
		ServerWorld serverWorld = pointer.world();
		if (serverWorld.isClient()) {
			return stack;
		}

		BlockPos targetPos = pointer.pos().offset(pointer.state().get(DispenserBlock.FACING));
		setSuccess(tryShearBlock(serverWorld, stack, targetPos) || tryShearEntity(serverWorld, targetPos, stack));

		if (isSuccess()) {
			stack.damage(SHEARS_DAMAGE_PER_USE, serverWorld, null, item -> {});
		}

		return stack;
	}

	private static boolean tryShearBlock(ServerWorld world, ItemStack tool, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		if (blockState.isIn(
				BlockTags.BEEHIVES,
				state -> state.contains(BeehiveBlock.HONEY_LEVEL) && state.getBlock() instanceof BeehiveBlock
		) == false) {
			return false;
		}

		if (blockState.get(BeehiveBlock.HONEY_LEVEL) < BeehiveBlock.FULL_HONEY_LEVEL) {
			return false;
		}

		world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
		BeehiveBlock.dropHoneycomb(world, tool, blockState, world.getBlockEntity(pos), null, pos);
		((BeehiveBlock) blockState.getBlock()).takeHoney(
				world,
				blockState,
				pos,
				null,
				BeehiveBlockEntity.BeeState.BEE_RELEASED
		);
		world.emitGameEvent(null, GameEvent.SHEAR, pos);
		return true;
	}

	private static boolean tryShearEntity(ServerWorld world, BlockPos pos, ItemStack shears) {
		for (Entity entity : world.getEntitiesByClass(Entity.class, new Box(pos), EntityPredicates.EXCEPT_SPECTATOR)) {
			if (entity.snipAllHeldLeashes(null)) {
				return true;
			}

			if (entity instanceof Shearable shearable && shearable.isShearable()) {
				shearable.sheared(world, SoundCategory.BLOCKS, shears);
				world.emitGameEvent(null, GameEvent.SHEAR, pos);
				return true;
			}
		}

		return false;
	}
}
