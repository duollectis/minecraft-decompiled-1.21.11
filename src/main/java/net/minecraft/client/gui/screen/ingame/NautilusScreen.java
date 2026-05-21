package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.AbstractNautilusEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NautilusScreenHandler;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code NautilusScreen}.
 */
public class NautilusScreen extends MountScreen<NautilusScreenHandler> {

	private static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/nautilus.png");

	public NautilusScreen(
			NautilusScreenHandler handler,
			PlayerInventory inventory,
			AbstractNautilusEntity nautilus,
			int slotColumnCount
	) {
		super(handler, inventory, nautilus.getDisplayName(), slotColumnCount, nautilus);
	}

	@Override
	protected Identifier getTexture() {
		return TEXTURE;
	}

	@Override
	protected Identifier getSlotTexture() {
		return SLOT_TEXTURE;
	}

	@Override
	protected @Nullable Identifier getChestSlotsTexture() {
		return null;
	}

	@Override
	protected boolean canEquipSaddle() {
		return this.mount.canUseSlot(EquipmentSlot.SADDLE);
	}

	@Override
	protected boolean canEquipArmor() {
		return this.mount.canUseSlot(EquipmentSlot.BODY);
	}
}
