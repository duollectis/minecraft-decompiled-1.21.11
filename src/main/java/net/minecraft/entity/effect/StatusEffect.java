package net.minecraft.entity.effect;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.entity.event.v1.effect.FabricMobEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureFlag;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.featuretoggle.ToggleableFeature;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Базовый класс статусного эффекта (зелья/эффекта моба).
 *
 * <p>Определяет категорию, цвет, частицы, звук применения и модификаторы атрибутов.
 * Подклассы переопределяют {@link #applyUpdateEffect} и {@link #canApplyUpdateEffect}
 * для реализации периодической логики, либо {@link #applyInstantEffect} для мгновенных эффектов.</p>
 */
public class StatusEffect implements ToggleableFeature, FabricMobEffect {

	public static final Codec<RegistryEntry<StatusEffect>> ENTRY_CODEC = Registries.STATUS_EFFECT.getEntryCodec();
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<StatusEffect>> ENTRY_PACKET_CODEC =
			PacketCodecs.registryEntry(RegistryKeys.STATUS_EFFECT);

	/** Альфа-канал частицы для ambient-режима (≈15% от 255). */
	private static final int AMBIENT_PARTICLE_ALPHA = MathHelper.floor(38.25F);

	private final Map<RegistryEntry<EntityAttribute>, EffectAttributeModifierCreator> attributeModifiers =
			new Object2ObjectOpenHashMap<>();
	private final StatusEffectCategory category;
	private final int color;
	private final Function<StatusEffectInstance, ParticleEffect> particleFactory;
	private @Nullable String translationKey;
	private int fadeInTicks;
	private int fadeOutTicks;
	private int fadeOutThresholdTicks;
	private Optional<SoundEvent> applySound = Optional.empty();
	private FeatureSet requiredFeatures = FeatureFlags.VANILLA_FEATURES;

	protected StatusEffect(StatusEffectCategory category, int color) {
		this.category = category;
		this.color = color;
		particleFactory = effect -> {
			int alpha = effect.isAmbient() ? AMBIENT_PARTICLE_ALPHA : 255;
			return TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, ColorHelper.withAlpha(alpha, color));
		};
	}

	protected StatusEffect(StatusEffectCategory category, int color, ParticleEffect particleEffect) {
		this.category = category;
		this.color = color;
		particleFactory = effect -> particleEffect;
	}

	public int getFadeInTicks() {
		return fadeInTicks;
	}

	public int getFadeOutTicks() {
		return fadeOutTicks;
	}

	public int getFadeOutThresholdTicks() {
		return fadeOutThresholdTicks;
	}

	/**
	 * Применяет периодический эффект к сущности на сервере.
	 *
	 * @param world     серверный мир
	 * @param entity    целевая сущность
	 * @param amplifier уровень усиления (0 = уровень I)
	 * @return {@code true}, если эффект должен продолжаться; {@code false} — для немедленного снятия
	 */
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		return true;
	}

	/**
	 * Применяет мгновенный эффект к цели (например, от брошенного зелья).
	 *
	 * @param world        серверный мир
	 * @param effectEntity сущность-носитель эффекта (снаряд), может быть {@code null}
	 * @param attacker     атакующий, может быть {@code null}
	 * @param target       цель
	 * @param amplifier    уровень усиления
	 * @param proximity    близость к центру взрыва (0.0–1.0), влияет на силу эффекта
	 */
	public void applyInstantEffect(
			ServerWorld world,
			@Nullable Entity effectEntity,
			@Nullable Entity attacker,
			LivingEntity target,
			int amplifier,
			double proximity
	) {
		applyUpdateEffect(world, target, amplifier);
	}

	/**
	 * Определяет, должен ли периодический эффект применяться на данном тике.
	 *
	 * @param duration  оставшаяся длительность в тиках
	 * @param amplifier уровень усиления
	 * @return {@code true}, если эффект нужно применить в этот тик
	 */
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return false;
	}

	public void onApplied(LivingEntity entity, int amplifier) {
	}

	/**
	 * Воспроизводит звук применения эффекта в позиции сущности, если звук задан.
	 */
	public void playApplySound(LivingEntity entity, int amplifier) {
		applySound.ifPresent(sound -> entity
				.getEntityWorld()
				.playSound(
						null,
						entity.getX(),
						entity.getY(),
						entity.getZ(),
						sound,
						entity.getSoundCategory(),
						1.0F,
						1.0F
				));
	}

	public void onEntityRemoval(ServerWorld world, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
	}

	public void onEntityDamage(
			ServerWorld world,
			LivingEntity entity,
			int amplifier,
			DamageSource source,
			float amount
	) {
	}

	public boolean isInstant() {
		return false;
	}

	protected String loadTranslationKey() {
		if (translationKey == null) {
			translationKey = Util.createTranslationKey("effect", Registries.STATUS_EFFECT.getId(this));
		}

		return translationKey;
	}

	public String getTranslationKey() {
		return loadTranslationKey();
	}

	public Text getName() {
		return Text.translatable(getTranslationKey());
	}

	public StatusEffectCategory getCategory() {
		return category;
	}

	public int getColor() {
		return color;
	}

	/**
	 * Регистрирует модификатор атрибута, масштабируемый по уровню усиления.
	 *
	 * @param attribute атрибут, к которому применяется модификатор
	 * @param id        уникальный идентификатор модификатора
	 * @param amount    базовое значение (умножается на {@code amplifier + 1})
	 * @param operation тип операции модификатора
	 * @return {@code this} для цепочки вызовов
	 */
	public StatusEffect addAttributeModifier(
			RegistryEntry<EntityAttribute> attribute,
			Identifier id,
			double amount,
			EntityAttributeModifier.Operation operation
	) {
		attributeModifiers.put(attribute, new EffectAttributeModifierCreator(id, amount, operation));
		return this;
	}

	/**
	 * Устанавливает одинаковое время затухания для входа, выхода и порога.
	 *
	 * @param fadeTicks количество тиков затухания
	 * @return {@code this} для цепочки вызовов
	 */
	public StatusEffect fadeTicks(int fadeTicks) {
		return fadeTicks(fadeTicks, fadeTicks, fadeTicks);
	}

	/**
	 * Устанавливает раздельные параметры затухания эффекта.
	 *
	 * @param fadeInTicks           тики плавного появления
	 * @param fadeOutTicks          тики плавного исчезновения
	 * @param fadeOutThresholdTicks порог оставшейся длительности, при котором начинается затухание
	 * @return {@code this} для цепочки вызовов
	 */
	public StatusEffect fadeTicks(int fadeInTicks, int fadeOutTicks, int fadeOutThresholdTicks) {
		this.fadeInTicks = fadeInTicks;
		this.fadeOutTicks = fadeOutTicks;
		this.fadeOutThresholdTicks = fadeOutThresholdTicks;
		return this;
	}

	/**
	 * Перебирает все зарегистрированные модификаторы атрибутов с учётом уровня усиления.
	 */
	public void forEachAttributeModifier(
			int amplifier,
			BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer
	) {
		attributeModifiers.forEach(
				(attribute, creator) -> consumer.accept(attribute, creator.createAttributeModifier(amplifier))
		);
	}

	/**
	 * Удаляет все модификаторы атрибутов этого эффекта из контейнера атрибутов сущности.
	 */
	public void onRemoved(AttributeContainer attributeContainer) {
		for (Entry<RegistryEntry<EntityAttribute>, EffectAttributeModifierCreator> entry : attributeModifiers.entrySet()) {
			EntityAttributeInstance instance = attributeContainer.getCustomInstance(entry.getKey());
			if (instance != null) {
				instance.removeModifier(entry.getValue().id());
			}
		}
	}

	/**
	 * Применяет все модификаторы атрибутов этого эффекта к контейнеру атрибутов сущности.
	 * Перед добавлением удаляет старые модификаторы во избежание дублирования.
	 */
	public void onApplied(AttributeContainer attributeContainer, int amplifier) {
		for (Entry<RegistryEntry<EntityAttribute>, EffectAttributeModifierCreator> entry : attributeModifiers.entrySet()) {
			EntityAttributeInstance instance = attributeContainer.getCustomInstance(entry.getKey());
			if (instance != null) {
				instance.removeModifier(entry.getValue().id());
				instance.addPersistentModifier(entry.getValue().createAttributeModifier(amplifier));
			}
		}
	}

	public boolean isBeneficial() {
		return category == StatusEffectCategory.BENEFICIAL;
	}

	public ParticleEffect createParticle(StatusEffectInstance effect) {
		return particleFactory.apply(effect);
	}

	/**
	 * Задаёт звук, воспроизводимый при применении эффекта.
	 *
	 * @return {@code this} для цепочки вызовов
	 */
	public StatusEffect applySound(SoundEvent sound) {
		applySound = Optional.of(sound);
		return this;
	}

	/**
	 * Ограничивает эффект набором feature-флагов (экспериментальные функции).
	 *
	 * @return {@code this} для цепочки вызовов
	 */
	public StatusEffect requires(FeatureFlag... requiredFeatures) {
		this.requiredFeatures = FeatureFlags.FEATURE_MANAGER.featureSetOf(requiredFeatures);
		return this;
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return requiredFeatures;
	}

	/**
	 * Фабрика модификатора атрибута, масштабирующая базовое значение на {@code (amplifier + 1)}.
	 */
	record EffectAttributeModifierCreator(
			Identifier id,
			double baseValue,
			EntityAttributeModifier.Operation operation
	) {

		public EntityAttributeModifier createAttributeModifier(int amplifier) {
			return new EntityAttributeModifier(id, baseValue * (amplifier + 1), operation);
		}
	}
}
