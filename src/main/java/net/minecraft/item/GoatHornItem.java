package net.minecraft.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.InstrumentComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;

/**
 * Предмет «Рог козла». Воспроизводит звук инструмента из компонента {@link InstrumentComponent}.
 * Длительность использования и кулдаун определяются параметрами инструмента.
 */
public class GoatHornItem extends Item {

	/** Количество тиков в секунде для перевода длительности инструмента. */
	private static final float TICKS_PER_SECOND = 20.0F;

	public GoatHornItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Создаёт стек рога с привязанным инструментом.
	 *
	 * @param item       предмет рога
	 * @param instrument запись реестра инструмента
	 * @return стек с установленным компонентом инструмента
	 */
	public static ItemStack getStackForInstrument(Item item, RegistryEntry<Instrument> instrument) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponentTypes.INSTRUMENT, new InstrumentComponent(instrument));
		return stack;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		Optional<? extends RegistryEntry<Instrument>> instrumentEntry = getInstrument(stack, user.getRegistryManager());

		if (instrumentEntry.isEmpty()) {
			return ActionResult.FAIL;
		}

		Instrument instrument = instrumentEntry.get().value();
		user.setCurrentHand(hand);
		playSound(world, user, instrument);
		user.getItemCooldownManager().set(stack, MathHelper.floor(instrument.useDuration() * TICKS_PER_SECOND));
		user.incrementStat(Stats.USED.getOrCreateStat(this));

		return ActionResult.CONSUME;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		Optional<RegistryEntry<Instrument>> instrumentEntry = getInstrument(stack, user.getRegistryManager());
		return instrumentEntry
				.<Integer>map(entry -> MathHelper.floor(entry.value().useDuration() * TICKS_PER_SECOND))
				.orElse(0);
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.TOOT_HORN;
	}

	private Optional<RegistryEntry<Instrument>> getInstrument(ItemStack stack, RegistryWrapper.WrapperLookup registries) {
		InstrumentComponent component = stack.get(DataComponentTypes.INSTRUMENT);
		return component != null ? component.getInstrument(registries) : Optional.empty();
	}

	private static void playSound(World world, PlayerEntity player, Instrument instrument) {
		SoundEvent sound = instrument.soundEvent().value();
		float volume = instrument.range() / 16.0F;
		world.playSoundFromEntity(player, player, sound, SoundCategory.RECORDS, volume, 1.0F);
		world.emitGameEvent(GameEvent.INSTRUMENT_PLAY, player.getEntityPos(), GameEvent.Emitter.of(player));
	}
}
