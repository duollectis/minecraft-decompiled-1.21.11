package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code HappyGhastRidingSoundInstance}.
 */
public class HappyGhastRidingSoundInstance extends MovingSoundInstance {

	private final PlayerEntity player;
	private final Entity vehicle;
	private final boolean wasSubmerged;
	private final float minVolume;
	private final float maxVolume;
	private final float volumeMultiplier;

	public HappyGhastRidingSoundInstance(
			PlayerEntity player,
			Entity entity,
			boolean bl,
			SoundEvent soundEvent,
			SoundCategory soundCategory,
			float f,
			float g,
			float h
	) {
		super(soundEvent, soundCategory, SoundInstance.createRandom());
		this.player = player;
		this.vehicle = entity;
		this.wasSubmerged = bl;
		this.minVolume = f;
		this.maxVolume = g;
		this.volumeMultiplier = h;
		this.attenuationType = SoundInstance.AttenuationType.NONE;
		this.repeat = true;
		this.repeatDelay = 0;
		this.volume = f;
	}

	@Override
	public boolean canPlay() {
		return !this.vehicle.isSilent();
	}

	@Override
	public boolean shouldAlwaysPlay() {
		return true;
	}

	protected boolean shouldSwitchToMinVolume() {
		return this.wasSubmerged != this.vehicle.isSubmergedInWater();
	}

	protected float getVehicleSpeed() {
		return (float) this.vehicle.getVelocity().length();
	}

	protected boolean isVolumeScalingEnabled() {
		return true;
	}

	@Override
	public void tick() {
		if (this.vehicle.isRemoved() || !this.player.hasVehicle() || this.player.getVehicle() != this.vehicle) {
			this.setDone();
		}
		else if (this.shouldSwitchToMinVolume()) {
			this.volume = this.minVolume;
		}
		else {
			float f = this.getVehicleSpeed();
			if (f >= 0.01F && this.isVolumeScalingEnabled()) {
				this.volume = this.volumeMultiplier * MathHelper.clampedLerp(f, this.minVolume, this.maxVolume);
			}
			else {
				this.volume = this.minVolume;
			}
		}
	}
}
