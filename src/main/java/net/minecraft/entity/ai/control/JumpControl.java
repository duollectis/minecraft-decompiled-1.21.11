package net.minecraft.entity.ai.control;

import net.minecraft.entity.mob.MobEntity;

/** Контроллер прыжка моба. Активируется на один тик через {@link #setActive()}. */
public class JumpControl implements Control {

	private final MobEntity entity;
	protected boolean active;

	public JumpControl(MobEntity entity) {
		this.entity = entity;
	}

	public void setActive() {
		active = true;
	}

	public void tick() {
		entity.setJumping(active);
		active = false;
	}
}
