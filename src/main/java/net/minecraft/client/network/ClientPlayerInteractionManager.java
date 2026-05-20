package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.OperatorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.RideableInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CraftRequestC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.SlotChangedStateC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientPlayerInteractionManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final MinecraftClient client;
   private final ClientPlayNetworkHandler networkHandler;
   private BlockPos currentBreakingPos = new BlockPos(-1, -1, -1);
   private ItemStack selectedStack = ItemStack.EMPTY;
   private float currentBreakingProgress;
   private float blockBreakingSoundCooldown;
   private int blockBreakingCooldown;
   private boolean breakingBlock;
   private GameMode gameMode = GameMode.DEFAULT;
   private @Nullable GameMode previousGameMode;
   private int lastSelectedSlot;

   public ClientPlayerInteractionManager(MinecraftClient client, ClientPlayNetworkHandler networkHandler) {
      this.client = client;
      this.networkHandler = networkHandler;
   }

   public void copyAbilities(PlayerEntity player) {
      this.gameMode.setAbilities(player.getAbilities());
   }

   public void setGameModes(GameMode gameMode, @Nullable GameMode previousGameMode) {
      this.gameMode = gameMode;
      this.previousGameMode = previousGameMode;
      this.gameMode.setAbilities(this.client.player.getAbilities());
   }

   public void setGameMode(GameMode gameMode) {
      if (gameMode != this.gameMode) {
         this.previousGameMode = this.gameMode;
      }

      this.gameMode = gameMode;
      this.gameMode.setAbilities(this.client.player.getAbilities());
   }

   public boolean hasStatusBars() {
      return this.gameMode.isSurvivalLike();
   }

   public boolean breakBlock(BlockPos pos) {
      if (this.client.player.isBlockBreakingRestricted(this.client.world, pos, this.gameMode)) {
         return false;
      } else {
         World world = this.client.world;
         BlockState blockState = world.getBlockState(pos);
         if (!this.client.player.getMainHandStack().canMine(blockState, world, pos, this.client.player)) {
            return false;
         } else {
            Block block = blockState.getBlock();
            if (block instanceof OperatorBlock && !this.client.player.isCreativeLevelTwoOp()) {
               return false;
            } else if (blockState.isAir()) {
               return false;
            } else {
               block.onBreak(world, pos, blockState, this.client.player);
               FluidState fluidState = world.getFluidState(pos);
               boolean bl = world.setBlockState(pos, fluidState.getBlockState(), 11);
               if (bl) {
                  block.onBroken(world, pos, blockState);
               }

               if (SharedConstants.BLOCK_BREAK) {
                  LOGGER.error("client broke {} {} -> {}", new Object[]{pos, blockState, world.getBlockState(pos)});
               }

               return bl;
            }
         }
      }
   }

   public boolean attackBlock(BlockPos pos, Direction direction) {
      if (this.client.player.isBlockBreakingRestricted(this.client.world, pos, this.gameMode)) {
         return false;
      } else if (!this.client.world.getWorldBorder().contains(pos)) {
         return false;
      } else {
         if (this.client.player.getAbilities().creativeMode) {
            BlockState blockState = this.client.world.getBlockState(pos);
            this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, 1.0F);
            if (SharedConstants.BLOCK_BREAK) {
               LOGGER.info("Creative start {} {}", pos, blockState);
            }

            this.sendSequencedPacket(this.client.world, sequence -> {
               this.breakBlock(pos);
               return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
            this.blockBreakingCooldown = 5;
         } else if (!this.breakingBlock || !this.isCurrentlyBreaking(pos)) {
            if (this.breakingBlock) {
               if (SharedConstants.BLOCK_BREAK) {
                  LOGGER.info("Abort old break {} {}", pos, this.client.world.getBlockState(pos));
               }

               this.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, this.currentBreakingPos, direction));
            }

            BlockState blockState = this.client.world.getBlockState(pos);
            this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, 0.0F);
            if (SharedConstants.BLOCK_BREAK) {
               LOGGER.info("Start break {} {}", pos, blockState);
            }

            this.sendSequencedPacket(this.client.world, sequence -> {
               boolean bl = !blockState.isAir();
               if (bl && this.currentBreakingProgress == 0.0F) {
                  blockState.onBlockBreakStart(this.client.world, pos, this.client.player);
               }

               if (bl && blockState.calcBlockBreakingDelta(this.client.player, this.client.player.getEntityWorld(), pos) >= 1.0F) {
                  this.breakBlock(pos);
               } else {
                  this.breakingBlock = true;
                  this.currentBreakingPos = pos;
                  this.selectedStack = this.client.player.getMainHandStack();
                  this.currentBreakingProgress = 0.0F;
                  this.blockBreakingSoundCooldown = 0.0F;
                  this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, this.getBlockBreakingProgress());
               }

               return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
         }

         return true;
      }
   }

   public void cancelBlockBreaking() {
      if (this.breakingBlock) {
         BlockState blockState = this.client.world.getBlockState(this.currentBreakingPos);
         this.client.getTutorialManager().onBlockBreaking(this.client.world, this.currentBreakingPos, blockState, -1.0F);
         if (SharedConstants.BLOCK_BREAK) {
            LOGGER.info("Stop dest {} {}", this.currentBreakingPos, blockState);
         }

         this.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, this.currentBreakingPos, Direction.DOWN));
         this.breakingBlock = false;
         this.currentBreakingProgress = 0.0F;
         this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, -1);
         this.client.player.resetTicksSince();
      }
   }

   public boolean updateBlockBreakingProgress(BlockPos pos, Direction direction) {
      this.syncSelectedSlot();
      if (this.blockBreakingCooldown > 0) {
         this.blockBreakingCooldown--;
         return true;
      } else if (this.client.player.getAbilities().creativeMode && this.client.world.getWorldBorder().contains(pos)) {
         this.blockBreakingCooldown = 5;
         BlockState blockState = this.client.world.getBlockState(pos);
         this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, 1.0F);
         if (SharedConstants.BLOCK_BREAK) {
            LOGGER.info("Creative cont {} {}", pos, blockState);
         }

         this.sendSequencedPacket(this.client.world, sequence -> {
            this.breakBlock(pos);
            return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
         });
         return true;
      } else if (this.isCurrentlyBreaking(pos)) {
         BlockState blockState = this.client.world.getBlockState(pos);
         if (blockState.isAir()) {
            this.breakingBlock = false;
            return false;
         } else {
            this.currentBreakingProgress = this.currentBreakingProgress
               + blockState.calcBlockBreakingDelta(this.client.player, this.client.player.getEntityWorld(), pos);
            if (this.blockBreakingSoundCooldown % 4.0F == 0.0F) {
               BlockSoundGroup blockSoundGroup = blockState.getSoundGroup();
               this.client
                  .getSoundManager()
                  .play(
                     new PositionedSoundInstance(
                        blockSoundGroup.getHitSound(),
                        SoundCategory.BLOCKS,
                        (blockSoundGroup.getVolume() + 1.0F) / 8.0F,
                        blockSoundGroup.getPitch() * 0.5F,
                        SoundInstance.createRandom(),
                        pos
                     )
                  );
            }

            this.blockBreakingSoundCooldown++;
            this.client.getTutorialManager().onBlockBreaking(this.client.world, pos, blockState, MathHelper.clamp(this.currentBreakingProgress, 0.0F, 1.0F));
            if (this.currentBreakingProgress >= 1.0F) {
               this.breakingBlock = false;
               if (SharedConstants.BLOCK_BREAK) {
                  LOGGER.info("Finished breaking {} {}", pos, blockState);
               }

               this.sendSequencedPacket(this.client.world, sequence -> {
                  this.breakBlock(pos);
                  return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
               });
               this.currentBreakingProgress = 0.0F;
               this.blockBreakingSoundCooldown = 0.0F;
               this.blockBreakingCooldown = 5;
            }

            this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, this.getBlockBreakingProgress());
            return true;
         }
      } else {
         return this.attackBlock(pos, direction);
      }
   }

   public final void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator) {
      try (PendingUpdateManager pendingUpdateManager = world.getPendingUpdateManager().incrementSequence()) {
         int i = pendingUpdateManager.getSequence();
         Packet<ServerPlayPacketListener> packet = packetCreator.predict(i);
         this.networkHandler.sendPacket(packet);
      }
   }

   public void tick() {
      this.syncSelectedSlot();
      if (this.networkHandler.getConnection().isOpen()) {
         this.networkHandler.getConnection().tick();
      } else {
         this.networkHandler.getConnection().handleDisconnection();
      }
   }

   private boolean isCurrentlyBreaking(BlockPos pos) {
      ItemStack itemStack = this.client.player.getMainHandStack();
      return pos.equals(this.currentBreakingPos) && ItemStack.areItemsAndComponentsEqual(itemStack, this.selectedStack);
   }

   public final void syncSelectedSlot() {
      int i = this.client.player.getInventory().getSelectedSlot();
      if (i != this.lastSelectedSlot) {
         this.lastSelectedSlot = i;
         this.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(this.lastSelectedSlot));
      }
   }

   public ActionResult interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
      this.syncSelectedSlot();
      if (!this.client.world.getWorldBorder().contains(hitResult.getBlockPos())) {
         return ActionResult.FAIL;
      } else {
         MutableObject<ActionResult> mutableObject = new MutableObject();
         this.sendSequencedPacket(this.client.world, sequence -> {
            mutableObject.setValue(this.interactBlockInternal(player, hand, hitResult));
            return new PlayerInteractBlockC2SPacket(hand, hitResult, sequence);
         });
         return (ActionResult)mutableObject.get();
      }
   }

   private ActionResult interactBlockInternal(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
      BlockPos blockPos = hitResult.getBlockPos();
      ItemStack itemStack = player.getStackInHand(hand);
      if (this.gameMode == GameMode.SPECTATOR) {
         return ActionResult.CONSUME;
      } else {
         boolean bl = !player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty();
         boolean bl2 = player.shouldCancelInteraction() && bl;
         if (!bl2) {
            BlockState blockState = this.client.world.getBlockState(blockPos);
            if (!this.networkHandler.hasFeature(blockState.getBlock().getRequiredFeatures())) {
               return ActionResult.FAIL;
            }

            ActionResult actionResult = blockState.onUseWithItem(player.getStackInHand(hand), this.client.world, player, hand, hitResult);
            if (actionResult.isAccepted()) {
               return actionResult;
            }

            if (actionResult instanceof ActionResult.PassToDefaultBlockAction && hand == Hand.MAIN_HAND) {
               ActionResult actionResult2 = blockState.onUse(this.client.world, player, hitResult);
               if (actionResult2.isAccepted()) {
                  return actionResult2;
               }
            }
         }

         if (!itemStack.isEmpty() && !player.getItemCooldownManager().isCoolingDown(itemStack)) {
            ItemUsageContext itemUsageContext = new ItemUsageContext(player, hand, hitResult);
            ActionResult actionResult3;
            if (player.isInCreativeMode()) {
               int i = itemStack.getCount();
               actionResult3 = itemStack.useOnBlock(itemUsageContext);
               itemStack.setCount(i);
            } else {
               actionResult3 = itemStack.useOnBlock(itemUsageContext);
            }

            return actionResult3;
         } else {
            return ActionResult.PASS;
         }
      }
   }

   public ActionResult interactItem(PlayerEntity player, Hand hand) {
      if (this.gameMode == GameMode.SPECTATOR) {
         return ActionResult.PASS;
      } else {
         this.syncSelectedSlot();
         MutableObject<ActionResult> mutableObject = new MutableObject();
         this.sendSequencedPacket(this.client.world, sequence -> {
            PlayerInteractItemC2SPacket playerInteractItemC2SPacket = new PlayerInteractItemC2SPacket(hand, sequence, player.getYaw(), player.getPitch());
            ItemStack itemStack = player.getStackInHand(hand);
            if (player.getItemCooldownManager().isCoolingDown(itemStack)) {
               mutableObject.setValue(ActionResult.PASS);
               return playerInteractItemC2SPacket;
            } else {
               ActionResult actionResult = itemStack.use(this.client.world, player, hand);
               ItemStack itemStack2;
               if (actionResult instanceof ActionResult.Success success) {
                  itemStack2 = Objects.requireNonNullElseGet(success.getNewHandStack(), () -> player.getStackInHand(hand));
               } else {
                  itemStack2 = player.getStackInHand(hand);
               }

               if (itemStack2 != itemStack) {
                  player.setStackInHand(hand, itemStack2);
               }

               mutableObject.setValue(actionResult);
               return playerInteractItemC2SPacket;
            }
         });
         return (ActionResult)mutableObject.get();
      }
   }

   public ClientPlayerEntity createPlayer(ClientWorld world, StatHandler statHandler, ClientRecipeBook recipeBook) {
      return this.createPlayer(world, statHandler, recipeBook, PlayerInput.DEFAULT, false);
   }

   public ClientPlayerEntity createPlayer(
      ClientWorld world, StatHandler statHandler, ClientRecipeBook recipeBook, PlayerInput lastPlayerInput, boolean lastSprinting
   ) {
      return new ClientPlayerEntity(this.client, world, this.networkHandler, statHandler, recipeBook, lastPlayerInput, lastSprinting);
   }

   public void attackEntity(PlayerEntity player, Entity target) {
      this.syncSelectedSlot();
      this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
      if (this.gameMode != GameMode.SPECTATOR) {
         player.attack(target);
         player.resetTicksSince();
      }
   }

   public ActionResult interactEntity(PlayerEntity player, Entity entity, Hand hand) {
      this.syncSelectedSlot();
      this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interact(entity, player.isSneaking(), hand));
      return (ActionResult)(this.gameMode == GameMode.SPECTATOR ? ActionResult.PASS : player.interact(entity, hand));
   }

   public ActionResult interactEntityAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand) {
      this.syncSelectedSlot();
      Vec3d vec3d = hitResult.getPos().subtract(entity.getX(), entity.getY(), entity.getZ());
      this.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.interactAt(entity, player.isSneaking(), hand, vec3d));
      return (ActionResult)(this.gameMode == GameMode.SPECTATOR ? ActionResult.PASS : entity.interactAt(player, vec3d, hand));
   }

   public void clickSlot(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player) {
      ScreenHandler screenHandler = player.currentScreenHandler;
      if (syncId != screenHandler.syncId) {
         LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", syncId, screenHandler.syncId);
      } else {
         DefaultedList<Slot> defaultedList = screenHandler.slots;
         int i = defaultedList.size();
         List<ItemStack> list = Lists.newArrayListWithCapacity(i);

         for (Slot slot : defaultedList) {
            list.add(slot.getStack().copy());
         }

         screenHandler.onSlotClick(slotId, button, actionType, player);
         Int2ObjectMap<ItemStackHash> int2ObjectMap = new Int2ObjectOpenHashMap();

         for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2 = defaultedList.get(j).getStack();
            if (!ItemStack.areEqual(itemStack, itemStack2)) {
               int2ObjectMap.put(j, ItemStackHash.fromItemStack(itemStack2, this.networkHandler.getComponentHasher()));
            }
         }

         ItemStackHash itemStackHash = ItemStackHash.fromItemStack(screenHandler.getCursorStack(), this.networkHandler.getComponentHasher());
         this.networkHandler
            .sendPacket(
               new ClickSlotC2SPacket(
                  syncId, screenHandler.getRevision(), Shorts.checkedCast(slotId), SignedBytes.checkedCast(button), actionType, int2ObjectMap, itemStackHash
               )
            );
      }
   }

   public void clickRecipe(int syncId, NetworkRecipeId recipeId, boolean craftAll) {
      this.networkHandler.sendPacket(new CraftRequestC2SPacket(syncId, recipeId, craftAll));
   }

   public void clickButton(int syncId, int buttonId) {
      this.networkHandler.sendPacket(new ButtonClickC2SPacket(syncId, buttonId));
   }

   public void clickCreativeStack(ItemStack stack, int slotId) {
      if (this.client.player.isInCreativeMode() && this.networkHandler.hasFeature(stack.getItem().getRequiredFeatures())) {
         this.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slotId, stack));
      }
   }

   public void dropCreativeStack(ItemStack stack) {
      boolean bl = this.client.currentScreen instanceof HandledScreen && !(this.client.currentScreen instanceof CreativeInventoryScreen);
      if (this.client.player.isInCreativeMode() && !bl && !stack.isEmpty() && this.networkHandler.hasFeature(stack.getItem().getRequiredFeatures())) {
         this.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(-1, stack));
         this.client.player.getItemDropCooldown().increment();
      }
   }

   public void stopUsingItem(PlayerEntity player) {
      this.syncSelectedSlot();
      this.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
      player.stopUsingItem();
   }

   public void attackWithPiercingWeapon(PiercingWeaponComponent piercingWeaponComponent) {
      this.syncSelectedSlot();
      this.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STAB, BlockPos.ORIGIN, Direction.DOWN));
      this.client.player.beforePlayerAttack();
      this.client.player.useAttackEnchantmentEffects();
      piercingWeaponComponent.playSound(this.client.player);
   }

   public boolean hasExperienceBar() {
      return this.gameMode.isSurvivalLike();
   }

   public boolean hasLimitedAttackSpeed() {
      return !this.gameMode.isCreative();
   }

   public boolean hasRidingInventory() {
      return this.client.player.hasVehicle() && this.client.player.getVehicle() instanceof RideableInventory;
   }

   public boolean isFlyingLocked() {
      return this.gameMode == GameMode.SPECTATOR;
   }

   public @Nullable GameMode getPreviousGameMode() {
      return this.previousGameMode;
   }

   public GameMode getCurrentGameMode() {
      return this.gameMode;
   }

   public boolean isBreakingBlock() {
      return this.breakingBlock;
   }

   public int getBlockBreakingProgress() {
      return this.currentBreakingProgress > 0.0F ? (int)(this.currentBreakingProgress * 10.0F) : -1;
   }

   public void pickItemFromBlock(BlockPos pos, boolean includeData) {
      this.networkHandler.sendPacket(new PickItemFromBlockC2SPacket(pos, includeData));
   }

   public void pickItemFromEntity(Entity entity, boolean includeData) {
      this.networkHandler.sendPacket(new PickItemFromEntityC2SPacket(entity.getId(), includeData));
   }

   public void slotChangedState(int slot, int screenHandlerId, boolean newState) {
      this.networkHandler.sendPacket(new SlotChangedStateC2SPacket(slot, screenHandlerId, newState));
   }
}
