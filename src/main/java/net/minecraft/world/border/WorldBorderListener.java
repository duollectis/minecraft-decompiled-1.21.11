package net.minecraft.world.border;

public interface WorldBorderListener {
   void onSizeChange(WorldBorder border, double size);

   void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time, long l);

   void onCenterChanged(WorldBorder border, double centerX, double centerZ);

   void onWarningTimeChanged(WorldBorder border, int warningTime);

   void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance);

   void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock);

   void onSafeZoneChanged(WorldBorder border, double safeZoneRadius);
}
