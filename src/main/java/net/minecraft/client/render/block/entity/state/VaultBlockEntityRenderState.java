package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class VaultBlockEntityRenderState extends BlockEntityRenderState {
   public @Nullable ItemStackEntityRenderState displayItemStackState;
   public float displayRotationDegrees;
}
