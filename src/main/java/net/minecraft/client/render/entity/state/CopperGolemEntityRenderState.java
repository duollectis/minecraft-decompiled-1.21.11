package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.passive.CopperGolemState;

import java.util.Optional;

@Environment(EnvType.CLIENT)
/**
 * {@code CopperGolemEntityRenderState}.
 */
public class CopperGolemEntityRenderState extends ArmedEntityRenderState {

	public Oxidizable.OxidationLevel oxidationLevel = Oxidizable.OxidationLevel.UNAFFECTED;
	public CopperGolemState copperGolemState = CopperGolemState.IDLE;
	public final AnimationState spinHeadAnimationState = new AnimationState();
	public final AnimationState gettingItemAnimationState = new AnimationState();
	public final AnimationState gettingNoItemAnimationState = new AnimationState();
	public final AnimationState droppingItemAnimationState = new AnimationState();
	public final AnimationState droppingNoItemAnimationState = new AnimationState();
	public Optional<BlockState> headBlockItemStack = Optional.empty();
}
