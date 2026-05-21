package net.minecraft.client.render.block.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;

@Environment(EnvType.CLIENT)
/**
 * {@code BannerBlockEntityRenderState}.
 */
public class BannerBlockEntityRenderState extends BlockEntityRenderState {

	public DyeColor dyeColor;
	public BannerPatternsComponent bannerPatterns;
	public float pitch;
	public float yaw;
	public boolean standing;
}
