package net.minecraft.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
/**
 * {@code BipedEntityRenderState}.
 */
public class BipedEntityRenderState extends ArmedEntityRenderState {

	public float leaningPitch;
	public float limbAmplitudeInverse = 1.0F;
	public float crossbowPullTime;
	public float itemUseTime;
	public Arm preferredArm = Arm.RIGHT;
	public Hand activeHand = Hand.MAIN_HAND;
	public boolean isInSneakingPose;
	public boolean isGliding;
	public boolean isSwimming;
	public boolean hasVehicle;
	public boolean isUsingItem;
	public float leftWingPitch;
	public float leftWingYaw;
	public float leftWingRoll;
	public ItemStack equippedHeadStack = ItemStack.EMPTY;
	public ItemStack equippedChestStack = ItemStack.EMPTY;
	public ItemStack equippedLegsStack = ItemStack.EMPTY;
	public ItemStack equippedFeetStack = ItemStack.EMPTY;

	@Override
	public float getItemUseTime(Arm arm) {
		return this.isUsingItem && this.activeHand == Hand.MAIN_HAND == (arm == this.mainArm) ? this.itemUseTime : 0.0F;
	}
}
