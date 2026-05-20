package net.minecraft.entity.ai.brain.task;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.collection.WeightedList;

public class Tasks {
   public static <E extends LivingEntity> SingleTickTask<E> pickRandomly(List<Pair<? extends TaskRunnable<? super E>, Integer>> weightedTasks) {
      return weighted(weightedTasks, CompositeTask.Order.SHUFFLED, CompositeTask.RunMode.RUN_ONE);
   }

   public static <E extends LivingEntity> SingleTickTask<E> weighted(
      List<Pair<? extends TaskRunnable<? super E>, Integer>> weightedTasks, CompositeTask.Order order, CompositeTask.RunMode runMode
   ) {
      WeightedList<TaskRunnable<? super E>> weightedList = new WeightedList<>();
      weightedTasks.forEach(task -> weightedList.add((TaskRunnable)task.getFirst(), (Integer)task.getSecond()));
      return TaskTriggerer.task(context -> context.point((world, entity, time) -> {
         if (order == CompositeTask.Order.SHUFFLED) {
            weightedList.shuffle();
         }

         for (TaskRunnable<? super E> taskRunnable : weightedList) {
            if (taskRunnable.trigger(world, entity, time) && runMode == CompositeTask.RunMode.RUN_ONE) {
               break;
            }
         }

         return true;
      }));
   }
}
