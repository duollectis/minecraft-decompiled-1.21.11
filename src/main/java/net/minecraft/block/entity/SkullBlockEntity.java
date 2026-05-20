package net.minecraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class SkullBlockEntity extends BlockEntity {
   private static final String PROFILE_NBT_KEY = "profile";
   private static final String NOTE_BLOCK_SOUND_NBT_KEY = "note_block_sound";
   private static final String CUSTOM_NAME_NBT_KEY = "custom_name";
   private @Nullable ProfileComponent owner;
   private @Nullable Identifier noteBlockSound;
   private int poweredTicks;
   private boolean powered;
   private @Nullable Text customName;

   public SkullBlockEntity(BlockPos pos, BlockState state) {
      super(BlockEntityType.SKULL, pos, state);
   }

   @Override
   protected void writeData(WriteView view) {
      super.writeData(view);
      view.putNullable("profile", ProfileComponent.CODEC, this.owner);
      view.putNullable("note_block_sound", Identifier.CODEC, this.noteBlockSound);
      view.putNullable("custom_name", TextCodecs.CODEC, this.customName);
   }

   @Override
   protected void readData(ReadView view) {
      super.readData(view);
      this.owner = view.<ProfileComponent>read("profile", ProfileComponent.CODEC).orElse(null);
      this.noteBlockSound = view.<Identifier>read("note_block_sound", Identifier.CODEC).orElse(null);
      this.customName = tryParseCustomName(view, "custom_name");
   }

   public static void tick(World world, BlockPos pos, BlockState state, SkullBlockEntity blockEntity) {
      if (state.contains(SkullBlock.POWERED) && state.get(SkullBlock.POWERED)) {
         blockEntity.powered = true;
         blockEntity.poweredTicks++;
      } else {
         blockEntity.powered = false;
      }
   }

   public float getPoweredTicks(float tickProgress) {
      return this.powered ? this.poweredTicks + tickProgress : this.poweredTicks;
   }

   public @Nullable ProfileComponent getOwner() {
      return this.owner;
   }

   public @Nullable Identifier getNoteBlockSound() {
      return this.noteBlockSound;
   }

   public BlockEntityUpdateS2CPacket toUpdatePacket() {
      return BlockEntityUpdateS2CPacket.create(this);
   }

   @Override
   public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
      return this.createComponentlessNbt(registries);
   }

   @Override
   protected void readComponents(ComponentsAccess components) {
      super.readComponents(components);
      this.owner = components.get(DataComponentTypes.PROFILE);
      this.noteBlockSound = components.get(DataComponentTypes.NOTE_BLOCK_SOUND);
      this.customName = components.get(DataComponentTypes.CUSTOM_NAME);
   }

   @Override
   protected void addComponents(ComponentMap.Builder builder) {
      super.addComponents(builder);
      builder.add(DataComponentTypes.PROFILE, this.owner);
      builder.add(DataComponentTypes.NOTE_BLOCK_SOUND, this.noteBlockSound);
      builder.add(DataComponentTypes.CUSTOM_NAME, this.customName);
   }

   @Override
   public void removeFromCopiedStackData(WriteView view) {
      super.removeFromCopiedStackData(view);
      view.remove("profile");
      view.remove("note_block_sound");
      view.remove("custom_name");
   }
}
