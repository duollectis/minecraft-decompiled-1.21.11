package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ClientPlayerLikeEntity {
   ClientPlayerLikeState getState();

   SkinTextures getSkin();

   @Nullable Text getMannequinName();

   ParrotEntity.@Nullable Variant getShoulderParrotVariant(boolean leftShoulder);

   boolean hasExtraEars();
}
