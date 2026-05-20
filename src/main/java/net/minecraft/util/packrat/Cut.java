package net.minecraft.util.packrat;

public interface Cut {
   Cut NOOP = new Cut() {
      @Override
      public void cut() {
      }

      @Override
      public boolean isCut() {
         return false;
      }
   };

   void cut();

   boolean isCut();
}
