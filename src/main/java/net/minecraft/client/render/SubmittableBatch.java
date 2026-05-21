package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.CameraRenderState;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code SubmittableBatch}.
 */
public class SubmittableBatch {

	public final List<Submittable> batch = new ArrayList<>();

	/**
	 * Обрабатывает событие frame end.
	 */
	public void onFrameEnd() {
		this.batch.forEach(Submittable::onFrameEnd);
		this.batch.clear();
	}

	/**
	 * Add.
	 *
	 * @param submittable submittable
	 */
	public void add(Submittable submittable) {
		this.batch.add(submittable);
	}

	/**
	 * Submit.
	 *
	 * @param queue queue
	 * @param cameraRenderState camera render state
	 */
	public void submit(OrderedRenderCommandQueueImpl queue, CameraRenderState cameraRenderState) {
		for (Submittable submittable : this.batch) {
			submittable.submit(queue, cameraRenderState);
		}
	}
}
