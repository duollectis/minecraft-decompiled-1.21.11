package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.village.VillagerData;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ZombieVillagerRenderState}.
 */
public class ZombieVillagerRenderState extends ZombieEntityRenderState implements VillagerDataRenderState {

	public @Nullable VillagerData villagerData;

	@Override
	public @Nullable VillagerData getVillagerData() {
		return this.villagerData;
	}
}
