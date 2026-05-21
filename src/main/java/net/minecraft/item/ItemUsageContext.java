package net.minecraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * {@code ItemUsageContext}.
 */
public class ItemUsageContext {

	private final @Nullable PlayerEntity player;
	private final Hand hand;
	private final BlockHitResult hit;
	private final World world;
	private final ItemStack stack;

	public ItemUsageContext(PlayerEntity player, Hand hand, BlockHitResult hit) {
		this(player.getEntityWorld(), player, hand, player.getStackInHand(hand), hit);
	}

	public ItemUsageContext(
			World world,
			@Nullable PlayerEntity player,
			Hand hand,
			ItemStack stack,
			BlockHitResult hit
	) {
		this.player = player;
		this.hand = hand;
		this.hit = hit;
		this.stack = stack;
		this.world = world;
	}

	protected final BlockHitResult getHitResult() {
		return this.hit;
	}

	public BlockPos getBlockPos() {
		return this.hit.getBlockPos();
	}

	public Direction getSide() {
		return this.hit.getSide();
	}

	public Vec3d getHitPos() {
		return this.hit.getPos();
	}

	/**
	 * Hits inside block.
	 *
	 * @return boolean — результат операции
	 */
	public boolean hitsInsideBlock() {
		return this.hit.isInsideBlock();
	}

	public ItemStack getStack() {
		return this.stack;
	}

	public @Nullable PlayerEntity getPlayer() {
		return this.player;
	}

	public Hand getHand() {
		return this.hand;
	}

	public World getWorld() {
		return this.world;
	}

	public Direction getHorizontalPlayerFacing() {
		return this.player == null ? Direction.NORTH : this.player.getHorizontalFacing();
	}

	/**
	 * Определяет, следует ли cancel interaction.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldCancelInteraction() {
		return this.player != null && this.player.shouldCancelInteraction();
	}

	public float getPlayerYaw() {
		return this.player == null ? 0.0F : this.player.getYaw();
	}
}
