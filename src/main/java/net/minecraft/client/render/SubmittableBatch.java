package net.minecraft.client.render;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.CameraRenderState;

@Environment(EnvType.CLIENT)
public class SubmittableBatch {
   public final List<Submittable> batch = new ArrayList<>();

   public void onFrameEnd() {
      this.batch.forEach(Submittable::onFrameEnd);
      this.batch.clear();
   }

   public void add(Submittable submittable) {
      this.batch.add(submittable);
   }

   public void submit(OrderedRenderCommandQueueImpl queue, CameraRenderState cameraRenderState) {
      for (Submittable submittable : this.batch) {
         submittable.submit(queue, cameraRenderState);
      }
   }
}
