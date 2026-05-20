package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.decoration.DisplayEntity;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class TextDisplayEntityRenderState extends DisplayEntityRenderState {
   public DisplayEntity.TextDisplayEntity.@Nullable Data data;
   public DisplayEntity.TextDisplayEntity.@Nullable TextLines textLines;

   @Override
   public boolean canRender() {
      return this.data != null;
   }
}
