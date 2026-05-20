package net.minecraft.client.render.fog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class FogData {
   public float environmentalStart;
   public float renderDistanceStart;
   public float environmentalEnd;
   public float renderDistanceEnd;
   public float skyEnd;
   public float cloudEnd;
}
