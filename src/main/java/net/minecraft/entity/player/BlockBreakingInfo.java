package net.minecraft.entity.player;

import net.minecraft.util.math.BlockPos;

/**
 * Хранит информацию о процессе разрушения блока одним из игроков или сущностей.
 * Используется для синхронизации анимации разрушения между клиентом и сервером.
 */
public class BlockBreakingInfo implements Comparable<BlockBreakingInfo> {

	private static final int MAX_STAGE = 10;

	private final int actorNetworkId;
	private final BlockPos pos;
	private int stage;
	private int lastUpdateTick;

	public BlockBreakingInfo(int actorNetworkId, BlockPos pos) {
		this.actorNetworkId = actorNetworkId;
		this.pos = pos;
	}

	public int getActorId() {
		return actorNetworkId;
	}

	public BlockPos getPos() {
		return pos;
	}

	/**
	 * Устанавливает стадию разрушения блока.
	 * Значение ограничено сверху константой {@link #MAX_STAGE}.
	 *
	 * @param stage новая стадия разрушения (0–10)
	 */
	public void setStage(int stage) {
		this.stage = Math.min(stage, MAX_STAGE);
	}

	public int getStage() {
		return stage;
	}

	public void setLastUpdateTick(int lastUpdateTick) {
		this.lastUpdateTick = lastUpdateTick;
	}

	public int getLastUpdateTick() {
		return lastUpdateTick;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		BlockBreakingInfo other = (BlockBreakingInfo) o;
		return actorNetworkId == other.actorNetworkId;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(actorNetworkId);
	}

	/**
	 * Сравнивает по стадии разрушения, при равенстве — по ID актора.
	 * Используется для сортировки при отображении анимации разрушения.
	 */
	@Override
	public int compareTo(BlockBreakingInfo other) {
		return stage != other.stage
			? Integer.compare(stage, other.stage)
			: Integer.compare(actorNetworkId, other.actorNetworkId);
	}
}
