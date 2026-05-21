package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;

@Environment(EnvType.CLIENT)
/**
 * {@code GiantEntityModel}.
 */
public class GiantEntityModel extends AbstractZombieModel<ZombieEntityRenderState> {

	public GiantEntityModel(ModelPart modelPart) {
		super(modelPart);
	}
}
