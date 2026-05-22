package net.minecraft.item;

import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Предмет компаса. При использовании на блоке лодочного камня привязывается к нему,
 * превращаясь в компас лодочного камня с особым именем и блеском.
 */
public class CompassItem extends Item {

	private static final Text LODESTONE_COMPASS_NAME = Text.translatable("item.minecraft.lodestone_compass");

	public CompassItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return stack.contains(DataComponentTypes.LODESTONE_TRACKER) || super.hasGlint(stack);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
		LodestoneTrackerComponent tracker = stack.get(DataComponentTypes.LODESTONE_TRACKER);

		if (tracker == null) {
			return;
		}

		LodestoneTrackerComponent updatedTracker = tracker.forWorld(world);

		if (updatedTracker != tracker) {
			stack.set(DataComponentTypes.LODESTONE_TRACKER, updatedTracker);
		}
	}

	/**
	 * При использовании на блоке лодочного камня привязывает компас к его позиции.
	 * Если стек содержит более одного предмета или игрок в режиме творчества,
	 * создаётся новый стек с привязкой, а исходный уменьшается.
	 */
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		BlockPos blockPos = context.getBlockPos();
		World world = context.getWorld();

		if (!world.getBlockState(blockPos).isOf(Blocks.LODESTONE)) {
			return super.useOnBlock(context);
		}

		world.playSound(null, blockPos, SoundEvents.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1.0F, 1.0F);

		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();
		boolean canModifyInPlace = !player.isInCreativeMode() && stack.getCount() == 1;
		LodestoneTrackerComponent tracker = new LodestoneTrackerComponent(
			Optional.of(GlobalPos.create(world.getRegistryKey(), blockPos)),
			true
		);

		if (canModifyInPlace) {
			stack.set(DataComponentTypes.LODESTONE_TRACKER, tracker);
		} else {
			ItemStack linkedCompass = stack.copyComponentsToNewStack(Items.COMPASS, 1);
			stack.decrementUnlessCreative(1, player);
			linkedCompass.set(DataComponentTypes.LODESTONE_TRACKER, tracker);

			if (!player.getInventory().insertStack(linkedCompass)) {
				player.dropItem(linkedCompass, false);
			}
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public Text getName(ItemStack stack) {
		return stack.contains(DataComponentTypes.LODESTONE_TRACKER) ? LODESTONE_COMPASS_NAME : super.getName(stack);
	}
}
