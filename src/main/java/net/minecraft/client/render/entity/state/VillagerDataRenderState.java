package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.village.VillagerData;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface VillagerDataRenderState {
   @Nullable VillagerData getVillagerData();
}
