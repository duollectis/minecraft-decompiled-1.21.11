package net.minecraft.entity.decoration;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * Узел поводка — невидимая сущность, прикреплённая к забору.
 * Позволяет привязывать к ней мобов через поводок.
 * Автоматически исчезает, когда все привязанные мобы отвязаны.
 */
public class LeashKnotEntity extends BlockAttachedEntity {

	/** Вертикальное смещение центра узла относительно нижней грани блока. */
	public static final double KNOT_Y_OFFSET = 0.375;

	public LeashKnotEntity(EntityType<? extends LeashKnotEntity> entityType, World world) {
		super(entityType, world);
	}

	public LeashKnotEntity(World world, BlockPos pos) {
		super(EntityType.LEASH_KNOT, world, pos);
		setPosition(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	protected void updateAttachmentPosition() {
		setPos(
				attachedBlockPos.getX() + 0.5,
				attachedBlockPos.getY() + KNOT_Y_OFFSET,
				attachedBlockPos.getZ() + 0.5
		);
		double halfWidth = getType().getWidth() / 2.0;
		double height = getType().getHeight();
		setBoundingBox(new Box(
				getX() - halfWidth, getY(), getZ() - halfWidth,
				getX() + halfWidth, getY() + height, getZ() + halfWidth
		));
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 1024.0;
	}

	@Override
	public void onBreak(ServerWorld world, @Nullable Entity breaker) {
		playSound(SoundEvents.ITEM_LEAD_UNTIED, 1.0F, 1.0F);
	}

	@Override
	protected void writeCustomData(WriteView view) {
	}

	@Override
	protected void readCustomData(ReadView view) {
	}

	/**
	 * Обрабатывает взаимодействие игрока с узлом:
	 * <ul>
	 *   <li>Ножницы — стандартное взаимодействие (отвязка).</li>
	 *   <li>Игрок держит мобов на поводке — привязывает их к узлу.</li>
	 *   <li>Иначе — передаёт поводки от узла игроку.</li>
	 * </ul>
	 */
	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		if (getEntityWorld().isClient()) {
			return ActionResult.SUCCESS;
		}

		if (player.getStackInHand(hand).isOf(Items.SHEARS)) {
			ActionResult actionResult = super.interact(player, hand);
			if (actionResult instanceof ActionResult.Success success && success.shouldIncrementStat()) {
				return actionResult;
			}
		}

		boolean attachedToKnot = false;
		for (Leashable leashable : Leashable.collectLeashablesHeldBy(player)) {
			if (leashable.canBeLeashedTo(this)) {
				leashable.attachLeash(this, true);
				attachedToKnot = true;
			}
		}

		boolean transferredToPlayer = false;
		if (!attachedToKnot && !player.shouldCancelInteraction()) {
			for (Leashable leashable : Leashable.collectLeashablesHeldBy(this)) {
				if (leashable.canBeLeashedTo(player)) {
					leashable.attachLeash(player, true);
					transferredToPlayer = true;
				}
			}
		}

		if (!attachedToKnot && !transferredToPlayer) {
			return super.interact(player, hand);
		}

		emitGameEvent(GameEvent.BLOCK_ATTACH, player);
		playSoundIfNotSilent(SoundEvents.ITEM_LEAD_TIED);
		return ActionResult.SUCCESS;
	}

	@Override
	public void onHeldLeashUpdate(Leashable heldLeashable) {
		if (Leashable.collectLeashablesHeldBy(this).isEmpty()) {
			discard();
		}
	}

	@Override
	public boolean canStayAttached() {
		return getEntityWorld().getBlockState(attachedBlockPos).isIn(BlockTags.FENCES);
	}

	/**
	 * Возвращает существующий узел поводка на заборе или создаёт новый.
	 * Поиск ведётся в радиусе 1 блока от указанной позиции.
	 */
	public static LeashKnotEntity getOrCreate(World world, BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		for (LeashKnotEntity knot : world.getNonSpectatingEntities(
				LeashKnotEntity.class,
				new Box(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0)
		)) {
			if (knot.getAttachedBlockPos().equals(pos)) {
				return knot;
			}
		}

		LeashKnotEntity newKnot = new LeashKnotEntity(world, pos);
		world.spawnEntity(newKnot);
		return newKnot;
	}

	public void onPlace() {
		playSound(SoundEvents.ITEM_LEAD_TIED, 1.0F, 1.0F);
	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
		return new EntitySpawnS2CPacket(this, 0, getAttachedBlockPos());
	}

	@Override
	public Vec3d getLeashPos(float tickProgress) {
		return getLerpedPos(tickProgress).add(0.0, 0.2, 0.0);
	}

	@Override
	public ItemStack getPickBlockStack() {
		return new ItemStack(Items.LEAD);
	}
}
