package net.minecraft.entity.projectile;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public abstract class AbstractFireballEntity extends ExplosiveProjectileEntity implements FlyingItemEntity {
   private static final float MAX_RENDER_DISTANCE_WHEN_NEWLY_SPAWNED = 12.25F;
   private static final TrackedData<ItemStack> ITEM = DataTracker.registerData(AbstractFireballEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

   public AbstractFireballEntity(EntityType<? extends AbstractFireballEntity> entityType, World world) {
      super(entityType, world);
   }

   public AbstractFireballEntity(EntityType<? extends AbstractFireballEntity> entityType, double d, double e, double f, Vec3d vec3d, World world) {
      super(entityType, d, e, f, vec3d, world);
   }

   public AbstractFireballEntity(EntityType<? extends AbstractFireballEntity> entityType, LivingEntity livingEntity, Vec3d vec3d, World world) {
      super(entityType, livingEntity, vec3d, world);
   }

   public void setItem(ItemStack stack) {
      if (stack.isEmpty()) {
         this.getDataTracker().set(ITEM, this.getItem());
      } else {
         this.getDataTracker().set(ITEM, stack.copyWithCount(1));
      }
   }

   @Override
   protected void playExtinguishSound() {
   }

   @Override
   public ItemStack getStack() {
      return this.getDataTracker().get(ITEM);
   }

   @Override
   protected void initDataTracker(DataTracker.Builder builder) {
      builder.add(ITEM, this.getItem());
   }

   @Override
   protected void writeCustomData(WriteView view) {
      super.writeCustomData(view);
      view.put("Item", ItemStack.CODEC, this.getStack());
   }

   @Override
   protected void readCustomData(ReadView view) {
      super.readCustomData(view);
      this.setItem(view.<ItemStack>read("Item", ItemStack.CODEC).orElse(this.getItem()));
   }

   private ItemStack getItem() {
      return new ItemStack(Items.FIRE_CHARGE);
   }

   @Override
   public @Nullable StackReference getStackReference(int slot) {
      return slot == 0 ? StackReference.of(this::getStack, this::setItem) : super.getStackReference(slot);
   }

   @Override
   public boolean shouldRender(double distance) {
      return this.age < 2 && distance < 12.25 ? false : super.shouldRender(distance);
   }
}
