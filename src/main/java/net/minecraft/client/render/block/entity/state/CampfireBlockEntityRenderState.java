package net.minecraft.client.render.block.entity.state;

import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class CampfireBlockEntityRenderState extends BlockEntityRenderState {
   public List<ItemRenderState> cookedItemStates = Collections.emptyList();
   public Direction facing = Direction.NORTH;
}
