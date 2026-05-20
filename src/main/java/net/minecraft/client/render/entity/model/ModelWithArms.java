package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
public interface ModelWithArms<T extends EntityRenderState> {
   void setArmAngle(T state, Arm arm, MatrixStack matrices);
}
