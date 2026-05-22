package net.minecraft.component.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ConsumeEffect;
import net.minecraft.item.consume.PlaySoundConsumeEffect;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.ArrayList;
import java.util.List;

/**
	 * Компонент потребляемого предмета (еда, зелья и т.д.). Управляет анимацией, звуком,
	 * частицами и эффектами при использовании предмета.
	 */
public record ConsumableComponent(
		float consumeSeconds,
		UseAction useAction,
		RegistryEntry<SoundEvent> sound,
		boolean hasConsumeParticles,
		List<ConsumeEffect> onConsumeEffects
) {

	public static final float DEFAULT_CONSUME_SECONDS = 1.6F;
	private static final int PARTICLES_AND_SOUND_TICK_INTERVAL = 4;
	private static final float PARTICLES_AND_SOUND_TICK_THRESHOLD = 0.21875F;
	public static final Codec<ConsumableComponent> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
										Codecs.NON_NEGATIVE_FLOAT
												.optionalFieldOf("consume_seconds", DEFAULT_CONSUME_SECONDS)
												.forGetter(ConsumableComponent::consumeSeconds),
										UseAction.CODEC
												.optionalFieldOf("animation", UseAction.EAT)
												.forGetter(ConsumableComponent::useAction),
										SoundEvent.ENTRY_CODEC
												.optionalFieldOf("sound", SoundEvents.ENTITY_GENERIC_EAT)
												.forGetter(ConsumableComponent::sound),
										Codec.BOOL
												.optionalFieldOf("has_consume_particles", true)
												.forGetter(ConsumableComponent::hasConsumeParticles),
										ConsumeEffect.CODEC
												.listOf()
												.optionalFieldOf("on_consume_effects", List.of())
												.forGetter(ConsumableComponent::onConsumeEffects)
								)
								.apply(instance, ConsumableComponent::new)
	);
	public static final PacketCodec<RegistryByteBuf, ConsumableComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT,
			ConsumableComponent::consumeSeconds,
			UseAction.PACKET_CODEC,
			ConsumableComponent::useAction,
			SoundEvent.ENTRY_PACKET_CODEC,
			ConsumableComponent::sound,
			PacketCodecs.BOOLEAN,
			ConsumableComponent::hasConsumeParticles,
			ConsumeEffect.PACKET_CODEC.collect(PacketCodecs.toList()),
			ConsumableComponent::onConsumeEffects,
			ConsumableComponent::new
	);

	/**
		 * Инициирует процесс потребления предмета. Если предмет потребляется мгновенно
		 * (нулевое время) — завершает потребление сразу; иначе устанавливает активную руку.
		 *
		 * @param user  сущность, использующая предмет
		 * @param stack стек предмета
		 * @param hand  рука, в которой держится предмет
		 * @return результат действия
		 */
	public ActionResult consume(LivingEntity user, ItemStack stack, Hand hand) {
		if (!canConsume(user, stack)) {
			return ActionResult.FAIL;
		}

		if (getConsumeTicks() > 0) {
			user.setCurrentHand(hand);
			return ActionResult.CONSUME;
		}

		ItemStack result = finishConsumption(user.getEntityWorld(), user, stack);
		return ActionResult.CONSUME.withNewHandStack(result);
	}

	/**
		 * Завершает потребление предмета: воспроизводит частицы и звук, начисляет статистику,
		 * применяет эффекты потребления и уменьшает стек на 1.
		 *
		 * @param world мир, в котором происходит потребление
		 * @param user  сущность, потребляющая предмет
		 * @param stack стек потребляемого предмета
		 * @return изменённый стек после потребления
		 */
	public ItemStack finishConsumption(World world, LivingEntity user, ItemStack stack) {
		spawnParticlesAndPlaySound(user.getRandom(), user, stack, 16);

		if (user instanceof ServerPlayerEntity serverPlayer) {
			serverPlayer.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
			Criteria.CONSUME_ITEM.trigger(serverPlayer, stack);
		}

		stack.streamAll(Consumable.class).forEach(consumable -> consumable.onConsume(world, user, stack, this));

		if (!world.isClient()) {
			onConsumeEffects.forEach(effect -> effect.onConsume(world, stack, user));
		}

		user.emitGameEvent(useAction == UseAction.DRINK ? GameEvent.DRINK : GameEvent.EAT);
		stack.decrementUnlessCreative(1, user);
		return stack;
	}

	/**
		 * Проверяет, может ли сущность потребить предмет. Для игроков с компонентом
		 * {@link FoodComponent} учитывается уровень голода и флаг {@code canAlwaysEat}.
		 *
		 * @param user  сущность, пытающаяся потребить предмет
		 * @param stack стек предмета
		 * @return {@code true} если потребление разрешено
		 */
	public boolean canConsume(LivingEntity user, ItemStack stack) {
		FoodComponent food = stack.get(DataComponentTypes.FOOD);
		return food == null || !(user instanceof PlayerEntity playerEntity)
				? true
				: playerEntity.canConsume(food.canAlwaysEat());
	}

	public int getConsumeTicks() {
		return (int) (consumeSeconds * 20.0F);
	}

	/**
		 * Воспроизводит звук поедания/питья и спавнит частицы предмета.
		 * Громкость и высота тона зависят от типа анимации ({@link UseAction#DRINK} или еда).
		 *
		 * @param random        источник случайности
		 * @param user          сущность, потребляющая предмет
		 * @param stack         стек предмета (для частиц)
		 * @param particleCount количество частиц для спавна
		 */
	public void spawnParticlesAndPlaySound(Random random, LivingEntity user, ItemStack stack, int particleCount) {
		float eatVolume = random.nextBoolean() ? 0.5F : 1.0F;
		float eatPitch = random.nextTriangular(1.0F, 0.2F);
		float drinkPitch = MathHelper.nextBetween(random, 0.9F, 1.0F);
		float volume = useAction == UseAction.DRINK ? 0.5F : eatVolume;
		float pitch = useAction == UseAction.DRINK ? drinkPitch : eatPitch;

		if (hasConsumeParticles) {
			user.spawnItemParticles(stack, particleCount);
		}

		SoundEvent soundEvent = user instanceof ConsumableSoundProvider provider
				? provider.getConsumeSound(stack)
				: sound.value();
		user.playSound(soundEvent, volume, pitch);
	}

	/**
		 * Определяет, нужно ли воспроизводить звук и частицы на текущем тике потребления.
		 * Эффекты начинаются после прохождения {@link #PARTICLES_AND_SOUND_TICK_THRESHOLD} от общего времени
		 * и повторяются каждые {@link #PARTICLES_AND_SOUND_TICK_INTERVAL} тиков.
		 *
		 * @param remainingUseTicks оставшееся количество тиков использования
		 * @return {@code true} если нужно воспроизвести звук и частицы
		 */
	public boolean shouldSpawnParticlesAndPlaySounds(int remainingUseTicks) {
		int elapsed = getConsumeTicks() - remainingUseTicks;
		int threshold = (int) (getConsumeTicks() * PARTICLES_AND_SOUND_TICK_THRESHOLD);
		return elapsed > threshold && remainingUseTicks % PARTICLES_AND_SOUND_TICK_INTERVAL == 0;
	}

	public static ConsumableComponent.Builder builder() {
		return new ConsumableComponent.Builder();
	}

	/**
		 * Строитель {@link ConsumableComponent}.
		 */
	public static class Builder {

		private float consumeSeconds = DEFAULT_CONSUME_SECONDS;
		private UseAction useAction = UseAction.EAT;
		private RegistryEntry<SoundEvent> sound = SoundEvents.ENTITY_GENERIC_EAT;
		private boolean consumeParticles = true;
		private final List<ConsumeEffect> consumeEffects = new ArrayList<>();

		Builder() {
		}

		public ConsumableComponent.Builder consumeSeconds(float consumeSeconds) {
			this.consumeSeconds = consumeSeconds;
			return this;
		}

		public ConsumableComponent.Builder useAction(UseAction useAction) {
			this.useAction = useAction;
			return this;
		}

		public ConsumableComponent.Builder sound(RegistryEntry<SoundEvent> sound) {
			this.sound = sound;
			return this;
		}

		public ConsumableComponent.Builder finishSound(RegistryEntry<SoundEvent> finishSound) {
			return this.consumeEffect(new PlaySoundConsumeEffect(finishSound));
		}

		public ConsumableComponent.Builder consumeParticles(boolean consumeParticles) {
			this.consumeParticles = consumeParticles;
			return this;
		}

		public ConsumableComponent.Builder consumeEffect(ConsumeEffect consumeEffect) {
			this.consumeEffects.add(consumeEffect);
			return this;
		}

		public ConsumableComponent build() {
			return new ConsumableComponent(
					this.consumeSeconds,
					this.useAction,
					this.sound,
					this.consumeParticles,
					this.consumeEffects
			);
		}
	}

	/**
		 * Интерфейс для сущностей, переопределяющих звук потребления предмета.
		 */
	public interface ConsumableSoundProvider {

		SoundEvent getConsumeSound(ItemStack stack);
	}
}
