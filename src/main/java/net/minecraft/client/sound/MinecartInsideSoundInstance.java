package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

@Environment(EnvType.CLIENT)
/**
 * {@code MinecartInsideSoundInstance}.
 */
public class MinecartInsideSoundInstance extends HappyGhastRidingSoundInstance {

	private final PlayerEntity player;
	private final AbstractMinecartEntity minecart;
	private final boolean underwater;

	public MinecartInsideSoundInstance(
			PlayerEntity player,
			AbstractMinecartEntity minecart,
			boolean underwater,
			SoundEvent soundEvent,
			float f,
			float g,
			float h
	) {
		super(player, minecart, underwater, soundEvent, SoundCategory.NEUTRAL, f, g, h);
		this.player = player;
		this.minecart = minecart;
		this.underwater = underwater;
	}

	@Override
	protected boolean shouldSwitchToMinVolume() {
		return this.underwater != this.player.isSubmergedInWater();
	}

	@Override
	protected float getVehicleSpeed() {
		return (float) this.minecart.getVelocity().horizontalLength();
	}

	@Override
	protected boolean isVolumeScalingEnabled() {
		return this.minecart.isOnRail() || !(this.minecart.getController() instanceof ExperimentalMinecartController);
	}
}
