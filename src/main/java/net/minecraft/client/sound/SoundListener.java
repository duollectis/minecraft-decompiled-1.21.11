package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.openal.AL10;

@Environment(EnvType.CLIENT)
/**
 * {@code SoundListener}.
 */
public class SoundListener {

	private SoundListenerTransform transform = SoundListenerTransform.DEFAULT;

	public void setTransform(SoundListenerTransform transform) {
		this.transform = transform;
		Vec3d vec3d = transform.position();
		Vec3d vec3d2 = transform.forward();
		Vec3d vec3d3 = transform.up();
		AL10.alListener3f(4100, (float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
		AL10.alListenerfv(
				4111,
				new float[]{
						(float) vec3d2.x,
						(float) vec3d2.y,
						(float) vec3d2.z,
						(float) vec3d3.getX(),
						(float) vec3d3.getY(),
						(float) vec3d3.getZ()
				}
		);
	}

	public void init() {
		this.setTransform(SoundListenerTransform.DEFAULT);
	}

	public SoundListenerTransform getTransform() {
		return this.transform;
	}
}
