package net.minecraft.item;

import com.google.common.collect.Maps;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Map;

/**
 * Предмет-краситель. Позволяет перекрашивать овец и текст на табличках.
 * Реестр всех красителей по цвету хранится в статической карте {@code DYES}.
 */
public class DyeItem extends Item implements SignChangingItem {

	private static final Map<DyeColor, DyeItem> DYES = Maps.newEnumMap(DyeColor.class);

	private final DyeColor color;

	public DyeItem(DyeColor color, Item.Settings settings) {
		super(settings);
		this.color = color;
		DYES.put(color, this);
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		if (entity instanceof SheepEntity sheep
				&& sheep.isAlive()
				&& !sheep.isSheared()
				&& sheep.getColor() != color
		) {
			sheep.getEntityWorld()
					.playSoundFromEntity(
							user,
							sheep,
							SoundEvents.ITEM_DYE_USE,
							SoundCategory.PLAYERS,
							1.0F,
							1.0F
					);

			if (!user.getEntityWorld().isClient()) {
				sheep.setColor(color);
				stack.decrement(1);
			}

			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}

	public DyeColor getColor() {
		return color;
	}

	/**
	 * Возвращает экземпляр {@link DyeItem} по заданному цвету.
	 * Все красители регистрируются в {@code DYES} при создании.
	 *
	 * @param color цвет красителя
	 * @return соответствующий {@link DyeItem}
	 */
	public static DyeItem byColor(DyeColor color) {
		return DYES.get(color);
	}

	@Override
	public boolean useOnSign(World world, SignBlockEntity signBlockEntity, boolean front, PlayerEntity player) {
		if (signBlockEntity.changeText(text -> text.withColor(getColor()), front)) {
			world.playSound(null, signBlockEntity.getPos(), SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
			return true;
		}

		return false;
	}
}
