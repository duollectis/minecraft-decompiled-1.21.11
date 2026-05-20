package net.minecraft.world.debug.gizmo;

public interface VisibilityConfigurable {
   VisibilityConfigurable ignoreOcclusion();

   VisibilityConfigurable withLifespan(int lifespan);

   VisibilityConfigurable fadeOut();
}
