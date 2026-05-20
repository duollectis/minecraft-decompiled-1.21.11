package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;

@Environment(EnvType.CLIENT)
public interface Submittable {
   void submit(OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState);

   default void onFrameEnd() {
   }
}
