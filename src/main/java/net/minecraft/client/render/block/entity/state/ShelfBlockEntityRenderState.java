package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ShelfBlockEntityRenderState extends BlockEntityRenderState {
   public @Nullable ItemRenderState[] itemRenderStates = new ItemRenderState[3];
   public boolean alignItemsToBottom;
}
