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
 * Контекст использования предмета на блоке.
 * <p>Содержит всю необходимую информацию о взаимодействии: мир, игрок, рука,
 * стек предмета и результат рейкаста по блоку.</p>
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
		return hit;
	}

	public BlockPos getBlockPos() {
		return hit.getBlockPos();
	}

	public Direction getSide() {
		return hit.getSide();
	}

	public Vec3d getHitPos() {
		return hit.getPos();
	}

	public boolean hitsInsideBlock() {
		return hit.isInsideBlock();
	}

	public ItemStack getStack() {
		return stack;
	}

	public @Nullable PlayerEntity getPlayer() {
		return player;
	}

	public Hand getHand() {
		return hand;
	}

	public World getWorld() {
		return world;
	}

	public Direction getHorizontalPlayerFacing() {
		return player == null ? Direction.NORTH : player.getHorizontalFacing();
	}

	/**
	 * Проверяет, должно ли взаимодействие быть отменено (например, игрок приседает).
	 *
	 * @return {@code true} если взаимодействие следует отменить
	 */
	public boolean shouldCancelInteraction() {
		return player != null && player.shouldCancelInteraction();
	}

	public float getPlayerYaw() {
		return player == null ? 0.0F : player.getYaw();
	}
}
