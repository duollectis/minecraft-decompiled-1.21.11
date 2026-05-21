package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
/**
 * {@code BeaconBlockEntityRenderState}.
 */
public class BeaconBlockEntityRenderState extends BlockEntityRenderState {

	public float beamRotationDegrees;
	public float beamScale;
	public List<BeaconBlockEntityRenderState.BeamSegment> beamSegments = new ArrayList<>();

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BeamSegment}.
	 */
	public record BeamSegment(int color, int height) {
	}
}
