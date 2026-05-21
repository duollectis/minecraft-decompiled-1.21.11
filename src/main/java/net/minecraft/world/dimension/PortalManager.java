package net.minecraft.world.dimension;

import net.minecraft.block.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import org.jspecify.annotations.Nullable;

/**
 * {@code PortalManager}.
 */
public class PortalManager {

	private final Portal portal;
	private BlockPos pos;
	private int ticksInPortal;
	private boolean inPortal;

	public PortalManager(Portal portal, BlockPos pos) {
		this.portal = portal;
		this.pos = pos;
		this.inPortal = true;
	}

	/**
	 * Tick.
	 *
	 * @param world world
	 * @param entity entity
	 * @param canUsePortals can use portals
	 *
	 * @return boolean — результат операции
	 */
	public boolean tick(ServerWorld world, Entity entity, boolean canUsePortals) {
		if (!this.inPortal) {
			this.decayTicksInPortal();
			return false;
		}
		else {
			this.inPortal = false;
			return canUsePortals && this.ticksInPortal++ >= this.portal.getPortalDelay(world, entity);
		}
	}

	/**
	 * Создаёт teleport target.
	 *
	 * @param world world
	 * @param entity entity
	 *
	 * @return @Nullable TeleportTarget — результат операции
	 */
	public @Nullable TeleportTarget createTeleportTarget(ServerWorld world, Entity entity) {
		return this.portal.createTeleportTarget(world, entity, this.pos);
	}

	public Portal.Effect getEffect() {
		return this.portal.getPortalEffect();
	}

	private void decayTicksInPortal() {
		this.ticksInPortal = Math.max(this.ticksInPortal - 4, 0);
	}

	public boolean hasExpired() {
		return this.ticksInPortal <= 0;
	}

	public BlockPos getPortalPos() {
		return this.pos;
	}

	public void setPortalPos(BlockPos pos) {
		this.pos = pos;
	}

	public int getTicksInPortal() {
		return this.ticksInPortal;
	}

	public boolean isInPortal() {
		return this.inPortal;
	}

	public void setInPortal(boolean inPortal) {
		this.inPortal = inPortal;
	}

	/**
	 * Portal matches.
	 *
	 * @param portal portal
	 *
	 * @return boolean — результат операции
	 */
	public boolean portalMatches(Portal portal) {
		return this.portal == portal;
	}
}
