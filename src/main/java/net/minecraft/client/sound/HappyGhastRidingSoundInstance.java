package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class HappyGhastRidingSoundInstance extends MovingSoundInstance {
   private final PlayerEntity player;
   private final Entity field_63920;
   private final boolean field_63921;
   private final float field_63922;
   private final float field_63923;
   private final float field_63924;

   public HappyGhastRidingSoundInstance(
      PlayerEntity player, Entity entity, boolean bl, SoundEvent soundEvent, SoundCategory soundCategory, float f, float g, float h
   ) {
      super(soundEvent, soundCategory, SoundInstance.createRandom());
      this.player = player;
      this.field_63920 = entity;
      this.field_63921 = bl;
      this.field_63922 = f;
      this.field_63923 = g;
      this.field_63924 = h;
      this.attenuationType = SoundInstance.AttenuationType.NONE;
      this.repeat = true;
      this.repeatDelay = 0;
      this.volume = f;
   }

   @Override
   public boolean canPlay() {
      return !this.field_63920.isSilent();
   }

   @Override
   public boolean shouldAlwaysPlay() {
      return true;
   }

   protected boolean method_75841() {
      return this.field_63921 != this.field_63920.isSubmergedInWater();
   }

   protected float method_75842() {
      return (float)this.field_63920.getVelocity().length();
   }

   protected boolean method_75843() {
      return true;
   }

   @Override
   public void tick() {
      if (this.field_63920.isRemoved() || !this.player.hasVehicle() || this.player.getVehicle() != this.field_63920) {
         this.setDone();
      } else if (this.method_75841()) {
         this.volume = this.field_63922;
      } else {
         float f = this.method_75842();
         if (f >= 0.01F && this.method_75843()) {
            this.volume = this.field_63924 * MathHelper.clampedLerp(f, this.field_63922, this.field_63923);
         } else {
            this.volume = this.field_63922;
         }
      }
   }
}
