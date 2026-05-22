package net.minecraft.entity.ai.brain.task;

import com.mojang.datafixers.kinds.OptionalBox.Mu;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.MemoryQueryResult;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.HashSet;

/**
 * Фабричный класс задачи мозга, открывающей двери на пути сущности и закрывающей их после прохода.
 * Отслеживает двери, которые нужно закрыть, через память {@code DOORS_TO_CLOSE}.
 */
public class OpenDoorsTask {

	private static final int RUN_TIME = 20;
	private static final double PATHING_DISTANCE = 3.0;
	private static final double REACH_DISTANCE = 2.0;

	public static Task<LivingEntity> create() {
		MutableObject<PathNode> lastPathNode = new MutableObject<>();
		MutableInt countdown = new MutableInt(0);

		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryValue(MemoryModuleType.PATH),
						                  context.queryMemoryOptional(MemoryModuleType.DOORS_TO_CLOSE),
						                  context.queryMemoryOptional(MemoryModuleType.MOBS)
				                  )
				                  .apply(
						                  context, (path, doorsToClose, mobs) -> (world, entity, time) -> {
							                  Path currentPath = context.getValue(path);
							                  Optional<Set<GlobalPos>> doorsSet = context.getOptionalValue(doorsToClose);

							                  if (currentPath.isStart() || currentPath.isFinished()) {
								                  return false;
							                  }

							                  if (Objects.equals(lastPathNode.get(), currentPath.getCurrentNode())) {
								                  countdown.setValue(RUN_TIME);
							                  } else if (countdown.decrementAndGet() > 0) {
								                  return false;
							                  }

							                  lastPathNode.setValue(currentPath.getCurrentNode());
							                  PathNode lastNode = currentPath.getLastNode();
							                  PathNode currentNode = currentPath.getCurrentNode();

							                  BlockPos lastPos = lastNode.getBlockPos();
							                  BlockState lastState = world.getBlockState(lastPos);

							                  if (lastState.isIn(
									                  BlockTags.MOB_INTERACTABLE_DOORS,
									                  state -> state.getBlock() instanceof DoorBlock
							                  )) {
								                  DoorBlock lastDoor = (DoorBlock) lastState.getBlock();

								                  if (!lastDoor.isOpen(lastState)) {
									                  lastDoor.setOpen(entity, world, lastState, lastPos, true);
								                  }

								                  doorsSet = storePos(doorsToClose, doorsSet, world, lastPos);
							                  }

							                  BlockPos currentPos = currentNode.getBlockPos();
							                  BlockState currentState = world.getBlockState(currentPos);

							                  if (currentState.isIn(
									                  BlockTags.MOB_INTERACTABLE_DOORS,
									                  state -> state.getBlock() instanceof DoorBlock
							                  )) {
								                  DoorBlock currentDoor = (DoorBlock) currentState.getBlock();

								                  if (!currentDoor.isOpen(currentState)) {
									                  currentDoor.setOpen(entity, world, currentState, currentPos, true);
									                  doorsSet = storePos(doorsToClose, doorsSet, world, currentPos);
								                  }
							                  }

							                  doorsSet.ifPresent(doors -> pathToDoor(
									                  world,
									                  entity,
									                  lastNode,
									                  currentNode,
									                  (Set<GlobalPos>) doors,
									                  context.getOptionalValue(mobs)
							                  ));

							                  return true;
						                  }
				                  )
		);
	}

	public static void pathToDoor(
			ServerWorld world,
			LivingEntity entity,
			@Nullable PathNode lastNode,
			@Nullable PathNode currentNode,
			Set<GlobalPos> doors,
			Optional<List<LivingEntity>> otherMobs
	) {
		Iterator<GlobalPos> iterator = doors.iterator();

		while (iterator.hasNext()) {
			GlobalPos globalPos = iterator.next();
			BlockPos pos = globalPos.pos();

			boolean isLastNode = lastNode != null && lastNode.getBlockPos().equals(pos);
			boolean isCurrentNode = currentNode != null && currentNode.getBlockPos().equals(pos);

			if (isLastNode || isCurrentNode) {
				continue;
			}

			if (cannotReachDoor(world, entity, globalPos)) {
				iterator.remove();
				continue;
			}

			BlockState blockState = world.getBlockState(pos);

			if (!blockState.isIn(BlockTags.MOB_INTERACTABLE_DOORS, state -> state.getBlock() instanceof DoorBlock)) {
				iterator.remove();
				continue;
			}

			DoorBlock doorBlock = (DoorBlock) blockState.getBlock();

			if (!doorBlock.isOpen(blockState)) {
				iterator.remove();
				continue;
			}

			if (hasOtherMobReachedDoor(entity, pos, otherMobs)) {
				iterator.remove();
				continue;
			}

			doorBlock.setOpen(entity, world, blockState, pos, false);
			iterator.remove();
		}
	}

	private static boolean hasOtherMobReachedDoor(
			LivingEntity entity,
			BlockPos pos,
			Optional<List<LivingEntity>> otherMobs
	) {
		if (otherMobs.isEmpty()) {
			return false;
		}

		return otherMobs.get()
		                .stream()
		                .filter(mob -> mob.getType() == entity.getType())
		                .filter(mob -> pos.isWithinDistance(mob.getEntityPos(), REACH_DISTANCE))
		                .anyMatch(mob -> hasReached(mob.getBrain(), pos));
	}

	private static boolean hasReached(Brain<?> brain, BlockPos pos) {
		if (!brain.hasMemoryModule(MemoryModuleType.PATH)) {
			return false;
		}

		Path path = brain.getOptionalRegisteredMemory(MemoryModuleType.PATH).get();

		if (path.isFinished()) {
			return false;
		}

		PathNode lastNode = path.getLastNode();

		if (lastNode == null) {
			return false;
		}

		PathNode currentNode = path.getCurrentNode();
		return pos.equals(lastNode.getBlockPos()) || pos.equals(currentNode.getBlockPos());
	}

	private static boolean cannotReachDoor(ServerWorld world, LivingEntity entity, GlobalPos doorPos) {
		return doorPos.dimension() != world.getRegistryKey()
				|| !doorPos.pos().isWithinDistance(entity.getEntityPos(), PATHING_DISTANCE);
	}

	private static Optional<Set<GlobalPos>> storePos(
			MemoryQueryResult<Mu, Set<GlobalPos>> queryResult,
			Optional<Set<GlobalPos>> doors,
			ServerWorld world,
			BlockPos pos
	) {
		GlobalPos globalPos = GlobalPos.create(world.getRegistryKey(), pos);

		return Optional.of(doors.<Set<GlobalPos>>map(doorSet -> {
			doorSet.add(globalPos);
			return doorSet;
		}).orElseGet(() -> {
			Set<GlobalPos> newSet = new HashSet<>(List.of(globalPos));
			queryResult.remember(newSet);
			return newSet;
		}));
	}
}
