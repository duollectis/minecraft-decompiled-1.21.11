package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Менеджер счётчика просматривающих игроков для контейнеров (сундук, эндер-сундук).
 * Отслеживает количество открывших контейнер игроков и уведомляет подклассы
 * об открытии/закрытии для воспроизведения звуков и обновления состояния блока.
 */
public abstract class ViewerCountManager {

	private static final int SCHEDULE_TICK_DELAY = 5;
	private int viewerCount;
	private double maxBlockInteractionRange;

	protected abstract void onContainerOpen(World world, BlockPos pos, BlockState state);

	protected abstract void onContainerClose(World world, BlockPos pos, BlockState state);

	protected abstract void onViewerCountUpdate(
			World world,
			BlockPos pos,
			BlockState state,
			int oldViewerCount,
			int newViewerCount
	);

	public abstract boolean isPlayerViewing(PlayerEntity player);

	public void openContainer(
			LivingEntity user,
			World world,
			BlockPos pos,
			BlockState state,
			double userInteractionRange
	) {
		int previousCount = viewerCount++;

		if (previousCount == 0) {
			onContainerOpen(world, pos, state);
			world.emitGameEvent(user, GameEvent.CONTAINER_OPEN, pos);
			scheduleBlockTick(world, pos, state);
		}

		onViewerCountUpdate(world, pos, state, previousCount, viewerCount);
		maxBlockInteractionRange = Math.max(userInteractionRange, maxBlockInteractionRange);
	}

	public void closeContainer(LivingEntity user, World world, BlockPos pos, BlockState state) {
		int previousCount = viewerCount--;

		if (viewerCount == 0) {
			onContainerClose(world, pos, state);
			world.emitGameEvent(user, GameEvent.CONTAINER_CLOSE, pos);
			maxBlockInteractionRange = 0.0;
		}

		onViewerCountUpdate(world, pos, state, previousCount, viewerCount);
	}

	public List<ContainerUser> getViewingUsers(World world, BlockPos pos) {
		double searchRadius = maxBlockInteractionRange + 4.0;
		Box searchBox = new Box(pos).expand(searchRadius);

		return world.getOtherEntities((Entity) null, searchBox, entity -> hasViewingUsers(entity, pos))
				.stream()
				.map(entity -> (ContainerUser) entity)
				.collect(Collectors.toList());
	}

	private boolean hasViewingUsers(Entity entity, BlockPos blockPos) {
		return entity instanceof ContainerUser containerUser && !containerUser.asLivingEntity().isSpectator()
				? containerUser.isViewingContainerAt(this, blockPos)
				: false;
	}

	/**
	 * Пересчитывает реальное количество просматривающих игроков путём поиска сущностей
	 * в радиусе взаимодействия. Вызывается по расписанию тика блока.
	 */
	public void updateViewerCount(World world, BlockPos pos, BlockState state) {
		List<ContainerUser> viewers = getViewingUsers(world, pos);
		maxBlockInteractionRange = 0.0;

		for (ContainerUser viewer : viewers) {
			maxBlockInteractionRange = Math.max(viewer.getContainerInteractionRange(), maxBlockInteractionRange);
		}

		int newCount = viewers.size();
		int oldCount = viewerCount;

		if (oldCount != newCount) {
			boolean wasOpen = oldCount != 0;
			boolean isOpen = newCount != 0;

			if (isOpen && !wasOpen) {
				onContainerOpen(world, pos, state);
				world.emitGameEvent(null, GameEvent.CONTAINER_OPEN, pos);
			} else if (!isOpen) {
				onContainerClose(world, pos, state);
				world.emitGameEvent(null, GameEvent.CONTAINER_CLOSE, pos);
			}

			viewerCount = newCount;
		}

		onViewerCountUpdate(world, pos, state, oldCount, newCount);

		if (newCount > 0) {
			scheduleBlockTick(world, pos, state);
		}
	}

	public int getViewerCount() {
		return viewerCount;
	}

	private static void scheduleBlockTick(World world, BlockPos pos, BlockState state) {
		world.scheduleBlockTick(pos, state.getBlock(), SCHEDULE_TICK_DELAY);
	}
}
