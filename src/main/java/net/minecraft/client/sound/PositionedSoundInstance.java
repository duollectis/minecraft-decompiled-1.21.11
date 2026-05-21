package net.minecraft.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code PositionedSoundInstance}.
 */
public class PositionedSoundInstance extends AbstractSoundInstance {

	public PositionedSoundInstance(
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch,
			Random random,
			BlockPos pos
	) {
		this(sound, category, volume, pitch, random, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	/**
	 * Master.
	 *
	 * @param sound sound
	 * @param pitch pitch
	 *
	 * @return PositionedSoundInstance — результат операции
	 */
	public static PositionedSoundInstance master(SoundEvent sound, float pitch) {
		return master(sound, pitch, 0.25F);
	}

	/**
	 * Master.
	 *
	 * @param sound sound
	 * @param pitch pitch
	 *
	 * @return PositionedSoundInstance — результат операции
	 */
	public static PositionedSoundInstance master(RegistryEntry<SoundEvent> sound, float pitch) {
		return master(sound.value(), pitch);
	}

	/**
	 * Master.
	 *
	 * @param sound sound
	 * @param pitch pitch
	 * @param volume volume
	 *
	 * @return PositionedSoundInstance — результат операции
	 */
	public static PositionedSoundInstance master(SoundEvent sound, float pitch, float volume) {
		return new PositionedSoundInstance(
				sound.id(),
				SoundCategory.UI,
				volume,
				pitch,
				SoundInstance.createRandom(),
				false,
				0,
				SoundInstance.AttenuationType.NONE,
				0.0,
				0.0,
				0.0,
				true
		);
	}

	/**
	 * Music.
	 *
	 * @param sound sound
	 *
	 * @return PositionedSoundInstance — результат операции
	 */
	public static PositionedSoundInstance music(SoundEvent sound) {
		return new PositionedSoundInstance(
				sound.id(),
				SoundCategory.MUSIC,
				1.0F,
				1.0F,
				SoundInstance.createRandom(),
				false,
				0,
				SoundInstance.AttenuationType.NONE,
				0.0,
				0.0,
				0.0,
				true
		);
	}

	/**
	 * Record.
	 *
	 * @param sound sound
	 * @param pos pos
	 *
	 * @return PositionedSoundInstance — результат операции
	 */
	public static PositionedSoundInstance record(SoundEvent sound, Vec3d pos) {
		return new PositionedSoundInstance(
				sound,
				SoundCategory.RECORDS,
				4.0F,
				1.0F,
				SoundInstance.createRandom(),
				false,
				0,
				SoundInstance.AttenuationType.LINEAR,
				pos.x,
				pos.y,
				pos.z
		);
	}

	/**
	 * Ambient.
	 *
	 * @param sound sound
	 * @param pitch pitch
	 * @param volume volume
	 *
	 * @return PositionedSoundInstance — результат операции
	 */
	public static PositionedSoundInstance ambient(SoundEvent sound, float pitch, float volume) {
		return new PositionedSoundInstance(
				sound.id(),
				SoundCategory.AMBIENT,
				volume,
				pitch,
				SoundInstance.createRandom(),
				false,
				0,
				SoundInstance.AttenuationType.NONE,
				0.0,
				0.0,
				0.0,
				true
		);
	}

	/**
	 * Ambient.
	 *
	 * @param sound sound
	 *
	 * @return PositionedSoundInstance — результат операции
	 */
	public static PositionedSoundInstance ambient(SoundEvent sound) {
		return ambient(sound, 1.0F, 1.0F);
	}

	/**
	 * Ambient.
	 *
	 * @param sound sound
	 * @param random random
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return PositionedSoundInstance — результат операции
	 */
	public static PositionedSoundInstance ambient(SoundEvent sound, Random random, double x, double y, double z) {
		return new PositionedSoundInstance(
				sound,
				SoundCategory.AMBIENT,
				1.0F,
				1.0F,
				random,
				false,
				0,
				SoundInstance.AttenuationType.LINEAR,
				x,
				y,
				z
		);
	}

	public PositionedSoundInstance(
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch,
			Random random,
			double x,
			double y,
			double z
	) {
		this(sound, category, volume, pitch, random, false, 0, SoundInstance.AttenuationType.LINEAR, x, y, z);
	}

	private PositionedSoundInstance(
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch,
			Random random,
			boolean repeat,
			int repeatDelay,
			SoundInstance.AttenuationType attenuationType,
			double x,
			double y,
			double z
	) {
		this(sound.id(), category, volume, pitch, random, repeat, repeatDelay, attenuationType, x, y, z, false);
	}

	public PositionedSoundInstance(
			Identifier id,
			SoundCategory category,
			float volume,
			float pitch,
			Random random,
			boolean repeat,
			int repeatDelay,
			SoundInstance.AttenuationType attenuationType,
			double x,
			double y,
			double z,
			boolean relative
	) {
		super(id, category, random);
		this.volume = volume;
		this.pitch = pitch;
		this.x = x;
		this.y = y;
		this.z = z;
		this.repeat = repeat;
		this.repeatDelay = repeatDelay;
		this.attenuationType = attenuationType;
		this.relative = relative;
	}
}
