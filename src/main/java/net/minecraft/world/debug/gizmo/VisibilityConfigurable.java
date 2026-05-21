package net.minecraft.world.debug.gizmo;

/**
 * {@code VisibilityConfigurable}.
 */
public interface VisibilityConfigurable {

	VisibilityConfigurable ignoreOcclusion();

	VisibilityConfigurable withLifespan(int lifespan);

	VisibilityConfigurable fadeOut();
}
