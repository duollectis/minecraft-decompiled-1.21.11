package net.minecraft.util;

import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;

public sealed interface ActionResult permits ActionResult.Success, ActionResult.Fail, ActionResult.Pass, ActionResult.PassToDefaultBlockAction {
   ActionResult.Success SUCCESS = new ActionResult.Success(ActionResult.SwingSource.CLIENT, ActionResult.ItemContext.KEEP_HAND_STACK);
   ActionResult.Success SUCCESS_SERVER = new ActionResult.Success(ActionResult.SwingSource.SERVER, ActionResult.ItemContext.KEEP_HAND_STACK);
   ActionResult.Success CONSUME = new ActionResult.Success(ActionResult.SwingSource.NONE, ActionResult.ItemContext.KEEP_HAND_STACK);
   ActionResult.Fail FAIL = new ActionResult.Fail();
   ActionResult.Pass PASS = new ActionResult.Pass();
   ActionResult.PassToDefaultBlockAction PASS_TO_DEFAULT_BLOCK_ACTION = new ActionResult.PassToDefaultBlockAction();

   default boolean isAccepted() {
      return false;
   }

   public record Fail() implements ActionResult {
   }

   public record ItemContext(boolean incrementStat, @Nullable ItemStack newHandStack) {
      static ActionResult.ItemContext KEEP_HAND_STACK_NO_INCREMENT_STAT = new ActionResult.ItemContext(false, null);
      static ActionResult.ItemContext KEEP_HAND_STACK = new ActionResult.ItemContext(true, null);
   }

   public record Pass() implements ActionResult {
   }

   public record PassToDefaultBlockAction() implements ActionResult {
   }

   public record Success(ActionResult.SwingSource swingSource, ActionResult.ItemContext itemContext) implements ActionResult {
      @Override
      public boolean isAccepted() {
         return true;
      }

      public ActionResult.Success withNewHandStack(ItemStack newHandStack) {
         return new ActionResult.Success(this.swingSource, new ActionResult.ItemContext(true, newHandStack));
      }

      public ActionResult.Success noIncrementStat() {
         return new ActionResult.Success(this.swingSource, ActionResult.ItemContext.KEEP_HAND_STACK_NO_INCREMENT_STAT);
      }

      public boolean shouldIncrementStat() {
         return this.itemContext.incrementStat;
      }

      public @Nullable ItemStack getNewHandStack() {
         return this.itemContext.newHandStack;
      }
   }

   public static enum SwingSource {
      NONE,
      CLIENT,
      SERVER;
   }
}
