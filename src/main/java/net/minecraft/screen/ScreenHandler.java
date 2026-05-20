package net.minecraft.screen;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.screen.sync.TrackedSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ClickType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ScreenHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int EMPTY_SPACE_SLOT_INDEX = -999;
   public static final int field_30731 = 0;
   public static final int field_30732 = 1;
   public static final int field_30733 = 2;
   public static final int field_30734 = 0;
   public static final int field_30735 = 1;
   public static final int field_30736 = 2;
   public static final int field_30737 = Integer.MAX_VALUE;
   public static final int field_52557 = 9;
   public static final int field_52558 = 18;
   private final DefaultedList<ItemStack> trackedStacks = DefaultedList.of();
   public final DefaultedList<Slot> slots = DefaultedList.of();
   private final List<Property> properties = Lists.newArrayList();
   private ItemStack cursorStack = ItemStack.EMPTY;
   private final DefaultedList<TrackedSlot> trackedSlots = DefaultedList.of();
   private final IntList trackedPropertyValues = new IntArrayList();
   private TrackedSlot trackedCursorSlot = TrackedSlot.ALWAYS_IN_SYNC;
   private int revision;
   private final @Nullable ScreenHandlerType<?> type;
   public final int syncId;
   private int quickCraftButton = -1;
   private int quickCraftStage;
   private final Set<Slot> quickCraftSlots = Sets.newHashSet();
   private final List<ScreenHandlerListener> listeners = Lists.newArrayList();
   private @Nullable ScreenHandlerSyncHandler syncHandler;
   private boolean disableSync;

   protected ScreenHandler(@Nullable ScreenHandlerType<?> type, int syncId) {
      this.type = type;
      this.syncId = syncId;
   }

   protected void addPlayerHotbarSlots(Inventory playerInventory, int left, int y) {
      for (int i = 0; i < 9; i++) {
         this.addSlot(new Slot(playerInventory, i, left + i * 18, y));
      }
   }

   protected void addPlayerInventorySlots(Inventory playerInventory, int left, int top) {
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(playerInventory, j + (i + 1) * 9, left + j * 18, top + i * 18));
         }
      }
   }

   protected void addPlayerSlots(Inventory playerInventory, int left, int top) {
      this.addPlayerInventorySlots(playerInventory, left, top);
      int i = 4;
      int j = 58;
      this.addPlayerHotbarSlots(playerInventory, left, top + 58);
   }

   protected static boolean canUse(ScreenHandlerContext context, PlayerEntity player, Block block) {
      return context.get((world, pos) -> !world.getBlockState(pos).isOf(block) ? false : player.canInteractWithBlockAt(pos, 4.0), true);
   }

   public ScreenHandlerType<?> getType() {
      if (this.type == null) {
         throw new UnsupportedOperationException("Unable to construct this menu by type");
      } else {
         return this.type;
      }
   }

   protected static void checkSize(Inventory inventory, int expectedSize) {
      int i = inventory.size();
      if (i < expectedSize) {
         throw new IllegalArgumentException("Container size " + i + " is smaller than expected " + expectedSize);
      }
   }

   protected static void checkDataCount(PropertyDelegate data, int expectedCount) {
      int i = data.size();
      if (i < expectedCount) {
         throw new IllegalArgumentException("Container data count " + i + " is smaller than expected " + expectedCount);
      }
   }

   public boolean isValid(int slot) {
      return slot == -1 || slot == -999 || slot < this.slots.size();
   }

   protected Slot addSlot(Slot slot) {
      slot.id = this.slots.size();
      this.slots.add(slot);
      this.trackedStacks.add(ItemStack.EMPTY);
      this.trackedSlots.add(this.syncHandler != null ? this.syncHandler.createTrackedSlot() : TrackedSlot.ALWAYS_IN_SYNC);
      return slot;
   }

   protected Property addProperty(Property property) {
      this.properties.add(property);
      this.trackedPropertyValues.add(0);
      return property;
   }

   protected void addProperties(PropertyDelegate propertyDelegate) {
      for (int i = 0; i < propertyDelegate.size(); i++) {
         this.addProperty(Property.create(propertyDelegate, i));
      }
   }

   public void addListener(ScreenHandlerListener listener) {
      if (!this.listeners.contains(listener)) {
         this.listeners.add(listener);
         this.sendContentUpdates();
      }
   }

   public void updateSyncHandler(ScreenHandlerSyncHandler handler) {
      this.syncHandler = handler;
      this.trackedCursorSlot = handler.createTrackedSlot();
      this.trackedSlots.replaceAll(slot -> handler.createTrackedSlot());
      this.syncState();
   }

   public void syncState() {
      List<ItemStack> list = new ArrayList<>(this.slots.size());
      int i = 0;

      for (int j = this.slots.size(); i < j; i++) {
         ItemStack itemStack = this.slots.get(i).getStack();
         list.add(itemStack.copy());
         this.trackedSlots.get(i).setReceivedStack(itemStack);
      }

      ItemStack itemStack2 = this.getCursorStack();
      this.trackedCursorSlot.setReceivedStack(itemStack2);
      int j = 0;

      for (int k = this.properties.size(); j < k; j++) {
         this.trackedPropertyValues.set(j, this.properties.get(j).get());
      }

      if (this.syncHandler != null) {
         this.syncHandler.updateState(this, list, itemStack2.copy(), this.trackedPropertyValues.toIntArray());
      }
   }

   public void removeListener(ScreenHandlerListener listener) {
      this.listeners.remove(listener);
   }

   public DefaultedList<ItemStack> getStacks() {
      DefaultedList<ItemStack> defaultedList = DefaultedList.of();

      for (Slot slot : this.slots) {
         defaultedList.add(slot.getStack());
      }

      return defaultedList;
   }

   public void sendContentUpdates() {
      for (int i = 0; i < this.slots.size(); i++) {
         ItemStack itemStack = this.slots.get(i).getStack();
         Supplier<ItemStack> supplier = Suppliers.memoize(itemStack::copy);
         this.updateTrackedSlot(i, itemStack, supplier);
         this.checkSlotUpdates(i, itemStack, supplier);
      }

      this.checkCursorStackUpdates();

      for (int i = 0; i < this.properties.size(); i++) {
         Property property = this.properties.get(i);
         int j = property.get();
         if (property.hasChanged()) {
            this.notifyPropertyUpdate(i, j);
         }

         this.checkPropertyUpdates(i, j);
      }
   }

   public void updateToClient() {
      for (int i = 0; i < this.slots.size(); i++) {
         ItemStack itemStack = this.slots.get(i).getStack();
         this.updateTrackedSlot(i, itemStack, itemStack::copy);
      }

      for (int i = 0; i < this.properties.size(); i++) {
         Property property = this.properties.get(i);
         if (property.hasChanged()) {
            this.notifyPropertyUpdate(i, property.get());
         }
      }

      this.syncState();
   }

   private void notifyPropertyUpdate(int index, int value) {
      for (ScreenHandlerListener screenHandlerListener : this.listeners) {
         screenHandlerListener.onPropertyUpdate(this, index, value);
      }
   }

   private void updateTrackedSlot(int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
      ItemStack itemStack = this.trackedStacks.get(slot);
      if (!ItemStack.areEqual(itemStack, stack)) {
         ItemStack itemStack2 = copySupplier.get();
         this.trackedStacks.set(slot, itemStack2);

         for (ScreenHandlerListener screenHandlerListener : this.listeners) {
            screenHandlerListener.onSlotUpdate(this, slot, itemStack2);
         }
      }
   }

   private void checkSlotUpdates(int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
      if (!this.disableSync) {
         TrackedSlot trackedSlot = this.trackedSlots.get(slot);
         if (!trackedSlot.isInSync(stack)) {
            trackedSlot.setReceivedStack(stack);
            if (this.syncHandler != null) {
               this.syncHandler.updateSlot(this, slot, copySupplier.get());
            }
         }
      }
   }

   private void checkPropertyUpdates(int id, int value) {
      if (!this.disableSync) {
         int i = this.trackedPropertyValues.getInt(id);
         if (i != value) {
            this.trackedPropertyValues.set(id, value);
            if (this.syncHandler != null) {
               this.syncHandler.updateProperty(this, id, value);
            }
         }
      }
   }

   private void checkCursorStackUpdates() {
      if (!this.disableSync) {
         ItemStack itemStack = this.getCursorStack();
         if (!this.trackedCursorSlot.isInSync(itemStack)) {
            this.trackedCursorSlot.setReceivedStack(itemStack);
            if (this.syncHandler != null) {
               this.syncHandler.updateCursorStack(this, itemStack.copy());
            }
         }
      }
   }

   public void setReceivedStack(int slot, ItemStack stack) {
      this.trackedSlots.get(slot).setReceivedStack(stack);
   }

   public void setReceivedHash(int slot, ItemStackHash hash) {
      if (slot >= 0 && slot < this.trackedSlots.size()) {
         this.trackedSlots.get(slot).setReceivedHash(hash);
      } else {
         LOGGER.debug("Incorrect slot index: {} available slots: {}", slot, this.trackedSlots.size());
      }
   }

   public void setReceivedCursorHash(ItemStackHash cursorStackHash) {
      this.trackedCursorSlot.setReceivedHash(cursorStackHash);
   }

   public boolean onButtonClick(PlayerEntity player, int id) {
      return false;
   }

   public Slot getSlot(int index) {
      return this.slots.get(index);
   }

   public abstract ItemStack quickMove(PlayerEntity player, int slot);

   public void selectBundleStack(int slot, int selectedStack) {
      if (slot >= 0 && slot < this.slots.size()) {
         ItemStack itemStack = this.slots.get(slot).getStack();
         BundleItem.setSelectedStackIndex(itemStack, selectedStack);
      }
   }

   public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      try {
         this.internalOnSlotClick(slotIndex, button, actionType, player);
      } catch (Exception var8) {
         CrashReport crashReport = CrashReport.create(var8, "Container click");
         CrashReportSection crashReportSection = crashReport.addElement("Click info");
         crashReportSection.add("Menu Type", () -> this.type != null ? Registries.SCREEN_HANDLER.getId(this.type).toString() : "<no type>");
         crashReportSection.add("Menu Class", () -> this.getClass().getCanonicalName());
         crashReportSection.add("Slot Count", this.slots.size());
         crashReportSection.add("Slot", slotIndex);
         crashReportSection.add("Button", button);
         crashReportSection.add("Type", actionType);
         throw new CrashException(crashReport);
      }
   }

   private void internalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      PlayerInventory playerInventory = player.getInventory();
      if (actionType == SlotActionType.QUICK_CRAFT) {
         int i = this.quickCraftStage;
         this.quickCraftStage = unpackQuickCraftStage(button);
         if ((i != 1 || this.quickCraftStage != 2) && i != this.quickCraftStage) {
            this.endQuickCraft();
         } else if (this.getCursorStack().isEmpty()) {
            this.endQuickCraft();
         } else if (this.quickCraftStage == 0) {
            this.quickCraftButton = unpackQuickCraftButton(button);
            if (shouldQuickCraftContinue(this.quickCraftButton, player)) {
               this.quickCraftStage = 1;
               this.quickCraftSlots.clear();
            } else {
               this.endQuickCraft();
            }
         } else if (this.quickCraftStage == 1) {
            Slot slot = this.slots.get(slotIndex);
            ItemStack itemStack = this.getCursorStack();
            if (canInsertItemIntoSlot(slot, itemStack, true)
               && slot.canInsert(itemStack)
               && (this.quickCraftButton == 2 || itemStack.getCount() > this.quickCraftSlots.size())
               && this.canInsertIntoSlot(slot)) {
               this.quickCraftSlots.add(slot);
            }
         } else if (this.quickCraftStage == 2) {
            if (!this.quickCraftSlots.isEmpty()) {
               if (this.quickCraftSlots.size() == 1) {
                  int j = this.quickCraftSlots.iterator().next().id;
                  this.endQuickCraft();
                  this.internalOnSlotClick(j, this.quickCraftButton, SlotActionType.PICKUP, player);
                  return;
               }

               ItemStack itemStack2 = this.getCursorStack().copy();
               if (itemStack2.isEmpty()) {
                  this.endQuickCraft();
                  return;
               }

               int k = this.getCursorStack().getCount();

               for (Slot slot2 : this.quickCraftSlots) {
                  ItemStack itemStack3 = this.getCursorStack();
                  if (slot2 != null
                     && canInsertItemIntoSlot(slot2, itemStack3, true)
                     && slot2.canInsert(itemStack3)
                     && (this.quickCraftButton == 2 || itemStack3.getCount() >= this.quickCraftSlots.size())
                     && this.canInsertIntoSlot(slot2)) {
                     int l = slot2.hasStack() ? slot2.getStack().getCount() : 0;
                     int m = Math.min(itemStack2.getMaxCount(), slot2.getMaxItemCount(itemStack2));
                     int n = Math.min(calculateStackSize(this.quickCraftSlots, this.quickCraftButton, itemStack2) + l, m);
                     k -= n - l;
                     slot2.setStack(itemStack2.copyWithCount(n));
                  }
               }

               itemStack2.setCount(k);
               this.setCursorStack(itemStack2);
            }

            this.endQuickCraft();
         } else {
            this.endQuickCraft();
         }
      } else if (this.quickCraftStage != 0) {
         this.endQuickCraft();
      } else if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) && (button == 0 || button == 1)) {
         ClickType clickType = button == 0 ? ClickType.LEFT : ClickType.RIGHT;
         if (slotIndex == -999) {
            if (!this.getCursorStack().isEmpty()) {
               if (clickType == ClickType.LEFT) {
                  player.dropItem(this.getCursorStack(), true);
                  this.setCursorStack(ItemStack.EMPTY);
               } else {
                  player.dropItem(this.getCursorStack().split(1), true);
               }
            }
         } else if (actionType == SlotActionType.QUICK_MOVE) {
            if (slotIndex < 0) {
               return;
            }

            Slot slot = this.slots.get(slotIndex);
            if (!slot.canTakeItems(player)) {
               return;
            }

            ItemStack itemStack = this.quickMove(player, slotIndex);

            while (!itemStack.isEmpty() && ItemStack.areItemsEqual(slot.getStack(), itemStack)) {
               itemStack = this.quickMove(player, slotIndex);
            }
         } else {
            if (slotIndex < 0) {
               return;
            }

            Slot slot = this.slots.get(slotIndex);
            ItemStack itemStack = slot.getStack();
            ItemStack itemStack4 = this.getCursorStack();
            player.onPickupSlotClick(itemStack4, slot.getStack(), clickType);
            if (!this.handleSlotClick(player, clickType, slot, itemStack, itemStack4)) {
               if (itemStack.isEmpty()) {
                  if (!itemStack4.isEmpty()) {
                     int o = clickType == ClickType.LEFT ? itemStack4.getCount() : 1;
                     this.setCursorStack(slot.insertStack(itemStack4, o));
                  }
               } else if (slot.canTakeItems(player)) {
                  if (itemStack4.isEmpty()) {
                     int o = clickType == ClickType.LEFT ? itemStack.getCount() : (itemStack.getCount() + 1) / 2;
                     Optional<ItemStack> optional = slot.tryTakeStackRange(o, Integer.MAX_VALUE, player);
                     optional.ifPresent(stack -> {
                        this.setCursorStack(stack);
                        slot.onTakeItem(player, stack);
                     });
                  } else if (slot.canInsert(itemStack4)) {
                     if (ItemStack.areItemsAndComponentsEqual(itemStack, itemStack4)) {
                        int o = clickType == ClickType.LEFT ? itemStack4.getCount() : 1;
                        this.setCursorStack(slot.insertStack(itemStack4, o));
                     } else if (itemStack4.getCount() <= slot.getMaxItemCount(itemStack4)) {
                        this.setCursorStack(itemStack);
                        slot.setStack(itemStack4);
                     }
                  } else if (ItemStack.areItemsAndComponentsEqual(itemStack, itemStack4)) {
                     Optional<ItemStack> optional2 = slot.tryTakeStackRange(itemStack.getCount(), itemStack4.getMaxCount() - itemStack4.getCount(), player);
                     optional2.ifPresent(stack -> {
                        itemStack4.increment(stack.getCount());
                        slot.onTakeItem(player, stack);
                     });
                  }
               }
            }

            slot.markDirty();
         }
      } else if (actionType == SlotActionType.SWAP && (button >= 0 && button < 9 || button == 40)) {
         ItemStack itemStack5 = playerInventory.getStack(button);
         Slot slot = this.slots.get(slotIndex);
         ItemStack itemStack = slot.getStack();
         if (!itemStack5.isEmpty() || !itemStack.isEmpty()) {
            if (itemStack5.isEmpty()) {
               if (slot.canTakeItems(player)) {
                  playerInventory.setStack(button, itemStack);
                  slot.onTake(itemStack.getCount());
                  slot.setStack(ItemStack.EMPTY);
                  slot.onTakeItem(player, itemStack);
               }
            } else if (itemStack.isEmpty()) {
               if (slot.canInsert(itemStack5)) {
                  int p = slot.getMaxItemCount(itemStack5);
                  if (itemStack5.getCount() > p) {
                     slot.setStack(itemStack5.split(p));
                  } else {
                     playerInventory.setStack(button, ItemStack.EMPTY);
                     slot.setStack(itemStack5);
                  }
               }
            } else if (slot.canTakeItems(player) && slot.canInsert(itemStack5)) {
               int p = slot.getMaxItemCount(itemStack5);
               if (itemStack5.getCount() > p) {
                  slot.setStack(itemStack5.split(p));
                  slot.onTakeItem(player, itemStack);
                  if (!playerInventory.insertStack(itemStack)) {
                     player.dropItem(itemStack, true);
                  }
               } else {
                  playerInventory.setStack(button, itemStack);
                  slot.setStack(itemStack5);
                  slot.onTakeItem(player, itemStack);
               }
            }
         }
      } else if (actionType == SlotActionType.CLONE && player.isInCreativeMode() && this.getCursorStack().isEmpty() && slotIndex >= 0) {
         Slot slot3 = this.slots.get(slotIndex);
         if (slot3.hasStack()) {
            ItemStack itemStack2 = slot3.getStack();
            this.setCursorStack(itemStack2.copyWithCount(itemStack2.getMaxCount()));
         }
      } else if (actionType == SlotActionType.THROW && this.getCursorStack().isEmpty() && slotIndex >= 0) {
         Slot slot3 = this.slots.get(slotIndex);
         int j = button == 0 ? 1 : slot3.getStack().getCount();
         if (!player.canDropItems()) {
            return;
         }

         ItemStack itemStack = slot3.takeStackRange(j, Integer.MAX_VALUE, player);
         player.dropItem(itemStack, true);
         player.dropCreativeStack(itemStack);
         if (button == 1) {
            while (!itemStack.isEmpty() && ItemStack.areItemsEqual(slot3.getStack(), itemStack)) {
               if (!player.canDropItems()) {
                  return;
               }

               itemStack = slot3.takeStackRange(j, Integer.MAX_VALUE, player);
               player.dropItem(itemStack, true);
               player.dropCreativeStack(itemStack);
            }
         }
      } else if (actionType == SlotActionType.PICKUP_ALL && slotIndex >= 0) {
         Slot slot3x = this.slots.get(slotIndex);
         ItemStack itemStack2 = this.getCursorStack();
         if (!itemStack2.isEmpty() && (!slot3x.hasStack() || !slot3x.canTakeItems(player))) {
            int k = button == 0 ? 0 : this.slots.size() - 1;
            int p = button == 0 ? 1 : -1;

            for (int o = 0; o < 2; o++) {
               for (int q = k; q >= 0 && q < this.slots.size() && itemStack2.getCount() < itemStack2.getMaxCount(); q += p) {
                  Slot slot4 = this.slots.get(q);
                  if (slot4.hasStack()
                     && canInsertItemIntoSlot(slot4, itemStack2, true)
                     && slot4.canTakeItems(player)
                     && this.canInsertIntoSlot(itemStack2, slot4)) {
                     ItemStack itemStack6 = slot4.getStack();
                     if (o != 0 || itemStack6.getCount() != itemStack6.getMaxCount()) {
                        ItemStack itemStack7 = slot4.takeStackRange(itemStack6.getCount(), itemStack2.getMaxCount() - itemStack2.getCount(), player);
                        itemStack2.increment(itemStack7.getCount());
                     }
                  }
               }
            }
         }
      }
   }

   private boolean handleSlotClick(PlayerEntity player, ClickType clickType, Slot slot, ItemStack stack, ItemStack cursorStack) {
      FeatureSet featureSet = player.getEntityWorld().getEnabledFeatures();
      return cursorStack.isItemEnabled(featureSet) && cursorStack.onStackClicked(slot, clickType, player)
         ? true
         : stack.isItemEnabled(featureSet) && stack.onClicked(cursorStack, slot, clickType, player, this.getCursorStackReference());
   }

   private StackReference getCursorStackReference() {
      return new StackReference() {
         @Override
         public ItemStack get() {
            return ScreenHandler.this.getCursorStack();
         }

         @Override
         public boolean set(ItemStack stack) {
            ScreenHandler.this.setCursorStack(stack);
            return true;
         }
      };
   }

   public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
      return true;
   }

   public void onClosed(PlayerEntity player) {
      if (player instanceof ServerPlayerEntity) {
         ItemStack itemStack = this.getCursorStack();
         if (!itemStack.isEmpty()) {
            offerOrDropStack(player, itemStack);
            this.setCursorStack(ItemStack.EMPTY);
         }
      }
   }

   private static void offerOrDropStack(PlayerEntity player, ItemStack stack) {
      boolean bl = player.isRemoved() && player.getRemovalReason() != Entity.RemovalReason.CHANGED_DIMENSION;
      boolean bl2 = player instanceof ServerPlayerEntity serverPlayerEntity && serverPlayerEntity.isDisconnected();
      if (bl || bl2) {
         player.dropItem(stack, false);
      } else if (player instanceof ServerPlayerEntity) {
         player.getInventory().offerOrDrop(stack);
      }
   }

   protected void dropInventory(PlayerEntity player, Inventory inventory) {
      for (int i = 0; i < inventory.size(); i++) {
         offerOrDropStack(player, inventory.removeStack(i));
      }
   }

   public void onContentChanged(Inventory inventory) {
      this.sendContentUpdates();
   }

   public void setStackInSlot(int slot, int revision, ItemStack stack) {
      this.getSlot(slot).setStackNoCallbacks(stack);
      this.revision = revision;
   }

   public void updateSlotStacks(int revision, List<ItemStack> stacks, ItemStack cursorStack) {
      for (int i = 0; i < stacks.size(); i++) {
         this.getSlot(i).setStackNoCallbacks(stacks.get(i));
      }

      this.cursorStack = cursorStack;
      this.revision = revision;
   }

   public void setProperty(int id, int value) {
      this.properties.get(id).set(value);
   }

   public abstract boolean canUse(PlayerEntity player);

   protected boolean insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
      boolean bl = false;
      int i = startIndex;
      if (fromLast) {
         i = endIndex - 1;
      }

      if (stack.isStackable()) {
         while (!stack.isEmpty() && (fromLast ? i >= startIndex : i < endIndex)) {
            Slot slot = this.slots.get(i);
            ItemStack itemStack = slot.getStack();
            if (!itemStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, itemStack)) {
               int j = itemStack.getCount() + stack.getCount();
               int k = slot.getMaxItemCount(itemStack);
               if (j <= k) {
                  stack.setCount(0);
                  itemStack.setCount(j);
                  slot.markDirty();
                  bl = true;
               } else if (itemStack.getCount() < k) {
                  stack.decrement(k - itemStack.getCount());
                  itemStack.setCount(k);
                  slot.markDirty();
                  bl = true;
               }
            }

            if (fromLast) {
               i--;
            } else {
               i++;
            }
         }
      }

      if (!stack.isEmpty()) {
         if (fromLast) {
            i = endIndex - 1;
         } else {
            i = startIndex;
         }

         while (fromLast ? i >= startIndex : i < endIndex) {
            Slot slotx = this.slots.get(i);
            ItemStack itemStackx = slotx.getStack();
            if (itemStackx.isEmpty() && slotx.canInsert(stack)) {
               int j = slotx.getMaxItemCount(stack);
               slotx.setStack(stack.split(Math.min(stack.getCount(), j)));
               slotx.markDirty();
               bl = true;
               break;
            }

            if (fromLast) {
               i--;
            } else {
               i++;
            }
         }
      }

      return bl;
   }

   public static int unpackQuickCraftButton(int quickCraftData) {
      return quickCraftData >> 2 & 3;
   }

   public static int unpackQuickCraftStage(int quickCraftData) {
      return quickCraftData & 3;
   }

   public static int packQuickCraftData(int quickCraftStage, int buttonId) {
      return quickCraftStage & 3 | (buttonId & 3) << 2;
   }

   public static boolean shouldQuickCraftContinue(int stage, PlayerEntity player) {
      if (stage == 0) {
         return true;
      } else {
         return stage == 1 ? true : stage == 2 && player.isInCreativeMode();
      }
   }

   protected void endQuickCraft() {
      this.quickCraftStage = 0;
      this.quickCraftSlots.clear();
   }

   public static boolean canInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow) {
      boolean bl = slot == null || !slot.hasStack();
      return !bl && ItemStack.areItemsAndComponentsEqual(stack, slot.getStack())
         ? slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= stack.getMaxCount()
         : bl;
   }

   public static int calculateStackSize(Set<Slot> slots, int mode, ItemStack stack) {
      return switch (mode) {
         case 0 -> MathHelper.floor((float)stack.getCount() / slots.size());
         case 1 -> 1;
         case 2 -> stack.getMaxCount();
         default -> stack.getCount();
      };
   }

   public boolean canInsertIntoSlot(Slot slot) {
      return true;
   }

   public static int calculateComparatorOutput(@Nullable BlockEntity entity) {
      return entity instanceof Inventory ? calculateComparatorOutput((Inventory)entity) : 0;
   }

   public static int calculateComparatorOutput(@Nullable Inventory inventory) {
      if (inventory == null) {
         return 0;
      } else {
         float f = 0.0F;

         for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (!itemStack.isEmpty()) {
               f += (float)itemStack.getCount() / inventory.getMaxCount(itemStack);
            }
         }

         f /= inventory.size();
         return MathHelper.lerpPositive(f, 0, 15);
      }
   }

   public void setCursorStack(ItemStack stack) {
      this.cursorStack = stack;
   }

   public ItemStack getCursorStack() {
      return this.cursorStack;
   }

   public void disableSyncing() {
      this.disableSync = true;
   }

   public void enableSyncing() {
      this.disableSync = false;
   }

   public void copySharedSlots(ScreenHandler handler) {
      Table<Inventory, Integer, Integer> table = HashBasedTable.create();

      for (int i = 0; i < handler.slots.size(); i++) {
         Slot slot = handler.slots.get(i);
         table.put(slot.inventory, slot.getIndex(), i);
      }

      for (int i = 0; i < this.slots.size(); i++) {
         Slot slot = this.slots.get(i);
         Integer integer = (Integer)table.get(slot.inventory, slot.getIndex());
         if (integer != null) {
            this.trackedStacks.set(i, handler.trackedStacks.get(integer));
            TrackedSlot trackedSlot = handler.trackedSlots.get(integer);
            TrackedSlot trackedSlot2 = this.trackedSlots.get(i);
            if (trackedSlot instanceof TrackedSlot.Impl impl && trackedSlot2 instanceof TrackedSlot.Impl impl2) {
               impl2.copyFrom(impl);
            }
         }
      }
   }

   public OptionalInt getSlotIndex(Inventory inventory, int index) {
      for (int i = 0; i < this.slots.size(); i++) {
         Slot slot = this.slots.get(i);
         if (slot.inventory == inventory && index == slot.getIndex()) {
            return OptionalInt.of(i);
         }
      }

      return OptionalInt.empty();
   }

   public int getRevision() {
      return this.revision;
   }

   public int nextRevision() {
      this.revision = this.revision + 1 & 32767;
      return this.revision;
   }
}
