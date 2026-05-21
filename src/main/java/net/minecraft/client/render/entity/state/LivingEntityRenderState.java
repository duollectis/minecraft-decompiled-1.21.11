package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.Direction;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code LivingEntityRenderState}.
 */
public class LivingEntityRenderState extends EntityRenderState {

	public float bodyYaw;
	public float relativeHeadYaw;
	public float pitch;
	public float deathTime;
	public float limbSwingAnimationProgress;
	public float limbSwingAmplitude;
	public float baseScale = 1.0F;
	public float ageScale = 1.0F;
	public float timeSinceLastKineticAttack;
	public boolean flipUpsideDown;
	public boolean shaking;
	public boolean baby;
	public boolean touchingWater;
	public boolean usingRiptide;
	public boolean hurt;
	public boolean invisibleToPlayer;
	public @Nullable Direction sleepingDirection;
	public EntityPose pose = EntityPose.STANDING;
	public final ItemRenderState headItemRenderState = new ItemRenderState();
	public float headItemAnimationProgress;
	public SkullBlock.@Nullable SkullType wearingSkullType;
	public @Nullable ProfileComponent wearingSkullProfile;

	public boolean isInPose(EntityPose pose) {
		return this.pose == pose;
	}
}
