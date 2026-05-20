package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ItemFrameEntityRenderState extends EntityRenderState {
   public Direction facing = Direction.NORTH;
   public final ItemRenderState itemRenderState = new ItemRenderState();
   public int rotation;
   public boolean glow;
   public @Nullable MapIdComponent mapId;
   public final MapRenderState mapRenderState = new MapRenderState();
}
