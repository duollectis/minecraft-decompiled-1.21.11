package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code EndLightFlashSoundInstance}.
 */
public class EndLightFlashSoundInstance extends MovingSoundInstance {

	private final Camera camera;
	private final float lightFlashPitch;
	private final float lightFlashYaw;

	public EndLightFlashSoundInstance(
			SoundEvent soundEvent,
			SoundCategory category,
			Random random,
			Camera camera,
			float rotationDegreesX,
			float rotationDegreesY
	) {
		super(soundEvent, category, random);
		this.camera = camera;
		this.lightFlashPitch = rotationDegreesX;
		this.lightFlashYaw = rotationDegreesY;
		this.update();
	}

	private void update() {
		Vec3d vec3d = Vec3d.fromPolar(this.lightFlashPitch, this.lightFlashYaw).multiply(10.0);
		this.x = this.camera.getCameraPos().x + vec3d.x;
		this.y = this.camera.getCameraPos().y + vec3d.y;
		this.z = this.camera.getCameraPos().z + vec3d.z;
		this.attenuationType = SoundInstance.AttenuationType.NONE;
	}

	@Override
	public void tick() {
		this.update();
	}
}
