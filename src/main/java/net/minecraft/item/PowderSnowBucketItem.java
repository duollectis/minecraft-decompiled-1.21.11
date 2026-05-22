package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Ведро с порошковым снегом. Размещает блок порошкового снега
 * только в воздухе и в пределах высотных ограничений мира.
 */
public class PowderSnowBucketItem extends BlockItem implements FluidModificationItem {

	/** Флаги обновления блока при установке порошкового снега. */
	private static final int BLOCK_PLACE_FLAGS = 3;

	private final SoundEvent placeSound;

	public PowderSnowBucketItem(Block block, SoundEvent placeSound, Item.Settings settings) {
		super(block, settings);
		this.placeSound = placeSound;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		ActionResult result = super.useOnBlock(context);
		PlayerEntity player = context.getPlayer();

		if (result.isAccepted() && player != null) {
			player.setStackInHand(
				context.getHand(),
				BucketItem.getEmptiedStack(context.getStack(), player)
			);
		}

		return result;
	}

	@Override
	protected SoundEvent getPlaceSound(BlockState state) {
		return placeSound;
	}

	@Override
	public boolean placeFluid(
		@Nullable LivingEntity user,
		World world,
		BlockPos pos,
		@Nullable BlockHitResult hitResult
	) {
		if (!world.isInBuildLimit(pos) || !world.isAir(pos)) {
			return false;
		}

		if (!world.isClient()) {
			world.setBlockState(pos, getBlock().getDefaultState(), BLOCK_PLACE_FLAGS);
		}

		world.emitGameEvent(user, GameEvent.FLUID_PLACE, pos);
		world.playSound(user, pos, placeSound, SoundCategory.BLOCKS, 1.0F, 1.0F);
		return true;
	}
}
