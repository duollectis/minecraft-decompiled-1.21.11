package net.minecraft.world.dimension;

import net.minecraft.block.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import org.jspecify.annotations.Nullable;

/**
 * Отслеживает состояние сущности внутри портала: накапливает тики пребывания
 * и инициирует телепортацию по достижении задержки портала.
 * Каждый тик без контакта с порталом уменьшает счётчик на 4.
 */
public class PortalManager {

	private static final int DECAY_PER_TICK = 4;

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
	 * Обновляет состояние портала за один тик.
	 * Если сущность не находится в портале — уменьшает счётчик тиков.
	 * Если находится — увеличивает счётчик и проверяет готовность к телепортации.
	 *
	 * @param world         серверный мир
	 * @param entity        сущность в портале
	 * @param canUsePortals разрешено ли использование порталов для данной сущности
	 * @return true, если сущность готова к телепортации (накоплено достаточно тиков)
	 */
	public boolean tick(ServerWorld world, Entity entity, boolean canUsePortals) {
		if (!inPortal) {
			decayTicksInPortal();
			return false;
		}

		inPortal = false;
		return canUsePortals && ticksInPortal++ >= portal.getPortalDelay(world, entity);
	}

	/**
	 * Создаёт цель телепортации для данного портала и сущности.
	 *
	 * @param world  серверный мир
	 * @param entity телепортируемая сущность
	 * @return цель телепортации или null, если портал не может создать цель
	 */
	public @Nullable TeleportTarget createTeleportTarget(ServerWorld world, Entity entity) {
		return portal.createTeleportTarget(world, entity, pos);
	}

	public Portal.Effect getEffect() {
		return portal.getPortalEffect();
	}

	public boolean hasExpired() {
		return ticksInPortal <= 0;
	}

	public BlockPos getPortalPos() {
		return pos;
	}

	public void setPortalPos(BlockPos pos) {
		this.pos = pos;
	}

	public int getTicksInPortal() {
		return ticksInPortal;
	}

	public boolean isInPortal() {
		return inPortal;
	}

	public void setInPortal(boolean inPortal) {
		this.inPortal = inPortal;
	}

	public boolean portalMatches(Portal portal) {
		return this.portal == portal;
	}

	private void decayTicksInPortal() {
		ticksInPortal = Math.max(ticksInPortal - DECAY_PER_TICK, 0);
	}
}
