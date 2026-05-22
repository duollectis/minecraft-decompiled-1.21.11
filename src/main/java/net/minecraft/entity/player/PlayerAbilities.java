package net.minecraft.entity.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Хранит набор способностей игрока: неуязвимость, полёт, режим творчества и скорости.
 * Синхронизируется с клиентом через {@link Packed} — сериализуемое представление.
 */
public class PlayerAbilities {

	private static final float DEFAULT_FLY_SPEED = 0.05F;
	private static final float DEFAULT_WALK_SPEED = 0.1F;

	public boolean invulnerable;
	public boolean flying;
	public boolean allowFlying;
	public boolean creativeMode;
	public boolean allowModifyWorld = true;
	private float flySpeed = DEFAULT_FLY_SPEED;
	private float walkSpeed = DEFAULT_WALK_SPEED;

	public float getFlySpeed() {
		return flySpeed;
	}

	public void setFlySpeed(float flySpeed) {
		this.flySpeed = flySpeed;
	}

	public float getWalkSpeed() {
		return walkSpeed;
	}

	public void setWalkSpeed(float walkSpeed) {
		this.walkSpeed = walkSpeed;
	}

	/**
	 * Упаковывает текущее состояние способностей в сериализуемый объект для отправки по сети или записи в NBT.
	 */
	public Packed pack() {
		return new Packed(
			invulnerable,
			flying,
			allowFlying,
			creativeMode,
			allowModifyWorld,
			flySpeed,
			walkSpeed
		);
	}

	/**
	 * Распаковывает состояние способностей из сетевого/NBT-представления.
	 *
	 * @param packed упакованные данные способностей
	 */
	public void unpack(Packed packed) {
		invulnerable = packed.invulnerable;
		flying = packed.flying;
		allowFlying = packed.mayFly;
		creativeMode = packed.instabuild;
		allowModifyWorld = packed.mayBuild;
		flySpeed = packed.flyingSpeed;
		walkSpeed = packed.walkingSpeed;
	}

	/**
	 * Сериализуемое представление способностей игрока для NBT и сетевых пакетов.
	 */
	public record Packed(
		boolean invulnerable,
		boolean flying,
		boolean mayFly,
		boolean instabuild,
		boolean mayBuild,
		float flyingSpeed,
		float walkingSpeed
	) {

		public static final Codec<Packed> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.BOOL.fieldOf("invulnerable").orElse(false).forGetter(Packed::invulnerable),
				Codec.BOOL.fieldOf("flying").orElse(false).forGetter(Packed::flying),
				Codec.BOOL.fieldOf("mayfly").orElse(false).forGetter(Packed::mayFly),
				Codec.BOOL.fieldOf("instabuild").orElse(false).forGetter(Packed::instabuild),
				Codec.BOOL.fieldOf("mayBuild").orElse(true).forGetter(Packed::mayBuild),
				Codec.FLOAT.fieldOf("flySpeed").orElse(DEFAULT_FLY_SPEED).forGetter(Packed::flyingSpeed),
				Codec.FLOAT.fieldOf("walkSpeed").orElse(DEFAULT_WALK_SPEED).forGetter(Packed::walkingSpeed)
			).apply(instance, Packed::new)
		);
	}
}
