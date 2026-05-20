package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BrushableBlockEntityRenderState extends BlockEntityRenderState {
   public ItemRenderState itemRenderState = new ItemRenderState();
   public int dusted;
   public @Nullable Direction face;
}
