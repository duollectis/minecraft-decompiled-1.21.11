package net.minecraft.screen;

public abstract class Property {
   private int oldValue;

   public static Property create(PropertyDelegate delegate, int index) {
      return new Property() {
         @Override
         public int get() {
            return delegate.get(index);
         }

         @Override
         public void set(int value) {
            delegate.set(index, value);
         }
      };
   }

   public static Property create(int[] array, int index) {
      return new Property() {
         @Override
         public int get() {
            return array[index];
         }

         @Override
         public void set(int value) {
            array[index] = value;
         }
      };
   }

   public static Property create() {
      return new Property() {
         private int value;

         @Override
         public int get() {
            return this.value;
         }

         @Override
         public void set(int value) {
            this.value = value;
         }
      };
   }

   public abstract int get();

   public abstract void set(int value);

   public boolean hasChanged() {
      int i = this.get();
      boolean bl = i != this.oldValue;
      this.oldValue = i;
      return bl;
   }
}
