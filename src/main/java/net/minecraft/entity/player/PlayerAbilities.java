package net.minecraft.entity.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * {@code PlayerAbilities}.
 */
public class PlayerAbilities {

	private static final boolean DEFAULT_INVULNERABLE = false;
	private static final boolean DEFAULT_FLYING = false;
	private static final boolean DEFAULT_ALLOW_FLYING = false;
	private static final boolean DEFAULT_CREATIVE_MODE = false;
	private static final boolean DEFAULT_ALLOW_MODIFY_WORLD = true;
	private static final float DEFAULT_FLY_SPEED = 0.05F;
	private static final float DEFAULT_WALK_SPEED = 0.1F;
	public boolean invulnerable;
	public boolean flying;
	public boolean allowFlying;
	public boolean creativeMode;
	public boolean allowModifyWorld = true;
	private float flySpeed = 0.05F;
	private float walkSpeed = 0.1F;

	public float getFlySpeed() {
		return this.flySpeed;
	}

	public void setFlySpeed(float flySpeed) {
		this.flySpeed = flySpeed;
	}

	public float getWalkSpeed() {
		return this.walkSpeed;
	}

	public void setWalkSpeed(float walkSpeed) {
		this.walkSpeed = walkSpeed;
	}

	public PlayerAbilities.Packed pack() {
		return new PlayerAbilities.Packed(
				this.invulnerable,
				this.flying,
				this.allowFlying,
				this.creativeMode,
				this.allowModifyWorld,
				this.flySpeed,
				this.walkSpeed
		);
	}

	public void unpack(PlayerAbilities.Packed packed) {
		this.invulnerable = packed.invulnerable;
		this.flying = packed.flying;
		this.allowFlying = packed.mayFly;
		this.creativeMode = packed.instabuild;
		this.allowModifyWorld = packed.mayBuild;
		this.flySpeed = packed.flyingSpeed;
		this.walkSpeed = packed.walkingSpeed;
	}

	/**
	 * {@code Packed}.
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

		public static final Codec<PlayerAbilities.Packed> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.BOOL
								                    .fieldOf("invulnerable")
								                    .orElse(false)
								                    .forGetter(PlayerAbilities.Packed::invulnerable),
						                    Codec.BOOL.fieldOf("flying").orElse(false).forGetter(PlayerAbilities.Packed::flying),
						                    Codec.BOOL.fieldOf("mayfly").orElse(false).forGetter(PlayerAbilities.Packed::mayFly),
						                    Codec.BOOL.fieldOf("instabuild").orElse(false).forGetter(PlayerAbilities.Packed::instabuild),
						                    Codec.BOOL.fieldOf("mayBuild").orElse(true).forGetter(PlayerAbilities.Packed::mayBuild),
						                    Codec.FLOAT.fieldOf("flySpeed").orElse(0.05F).forGetter(PlayerAbilities.Packed::flyingSpeed),
						                    Codec.FLOAT.fieldOf("walkSpeed").orElse(0.1F).forGetter(PlayerAbilities.Packed::walkingSpeed)
				                    )
				                    .apply(instance, PlayerAbilities.Packed::new)
		);
	}
}
