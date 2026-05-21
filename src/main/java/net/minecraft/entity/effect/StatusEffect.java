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
 * {@code StatusEffect}.
 */
public class StatusEffect implements ToggleableFeature, FabricMobEffect {

	public static final Codec<RegistryEntry<StatusEffect>> ENTRY_CODEC = Registries.STATUS_EFFECT.getEntryCodec();
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<StatusEffect>>
			ENTRY_PACKET_CODEC =
			PacketCodecs.registryEntry(RegistryKeys.STATUS_EFFECT);
	private static final int AMBIENT_PARTICLE_ALPHA = MathHelper.floor(38.25F);
	private final Map<RegistryEntry<EntityAttribute>, StatusEffect.EffectAttributeModifierCreator>
			attributeModifiers =
			new Object2ObjectOpenHashMap();
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
		this.particleFactory = effect -> {
			int j = effect.isAmbient() ? AMBIENT_PARTICLE_ALPHA : 255;
			return TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, ColorHelper.withAlpha(j, color));
		};
	}

	protected StatusEffect(StatusEffectCategory category, int color, ParticleEffect particleEffect) {
		this.category = category;
		this.color = color;
		this.particleFactory = effect -> particleEffect;
	}

	public int getFadeInTicks() {
		return this.fadeInTicks;
	}

	public int getFadeOutTicks() {
		return this.fadeOutTicks;
	}

	public int getFadeOutThresholdTicks() {
		return this.fadeOutThresholdTicks;
	}

	/**
	 * Применяет update effect.
	 *
	 * @param world world
	 * @param entity entity
	 * @param amplifier amplifier
	 *
	 * @return boolean — результат операции
	 */
	public boolean applyUpdateEffect(ServerWorld world, LivingEntity entity, int amplifier) {
		return true;
	}

	public void applyInstantEffect(
			ServerWorld world,
			@Nullable Entity effectEntity,
			@Nullable Entity attacker,
			LivingEntity target,
			int amplifier,
			double proximity
	) {
		this.applyUpdateEffect(world, target, amplifier);
	}

	/**
	 * Проверяет возможность apply update effect.
	 *
	 * @param duration duration
	 * @param amplifier amplifier
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canApplyUpdateEffect(int duration, int amplifier) {
		return false;
	}

	/**
	 * Обрабатывает событие applied.
	 *
	 * @param entity entity
	 * @param amplifier amplifier
	 */
	public void onApplied(LivingEntity entity, int amplifier) {
	}

	/**
	 * Play apply sound.
	 *
	 * @param entity entity
	 * @param amplifier amplifier
	 */
	public void playApplySound(LivingEntity entity, int amplifier) {
		this.applySound
				.ifPresent(sound -> entity
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

	/**
	 * Обрабатывает событие entity removal.
	 *
	 * @param world world
	 * @param entity entity
	 * @param amplifier amplifier
	 * @param reason reason
	 */
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

	/**
	 * Загружает translation key.
	 *
	 * @return String — результат операции
	 */
	protected String loadTranslationKey() {
		if (this.translationKey == null) {
			this.translationKey = Util.createTranslationKey("effect", Registries.STATUS_EFFECT.getId(this));
		}

		return this.translationKey;
	}

	public String getTranslationKey() {
		return this.loadTranslationKey();
	}

	public Text getName() {
		return Text.translatable(this.getTranslationKey());
	}

	public StatusEffectCategory getCategory() {
		return this.category;
	}

	public int getColor() {
		return this.color;
	}

	public StatusEffect addAttributeModifier(
			RegistryEntry<EntityAttribute> attribute,
			Identifier id,
			double amount,
			EntityAttributeModifier.Operation operation
	) {
		this.attributeModifiers.put(attribute, new StatusEffect.EffectAttributeModifierCreator(id, amount, operation));
		return this;
	}

	/**
	 * Fade ticks.
	 *
	 * @param fadeTicks fade ticks
	 *
	 * @return StatusEffect — результат операции
	 */
	public StatusEffect fadeTicks(int fadeTicks) {
		return this.fadeTicks(fadeTicks, fadeTicks, fadeTicks);
	}

	/**
	 * Fade ticks.
	 *
	 * @param fadeInTicks fade in ticks
	 * @param fadeOutTicks fade out ticks
	 * @param fadeOutThresholdTicks fade out threshold ticks
	 *
	 * @return StatusEffect — результат операции
	 */
	public StatusEffect fadeTicks(int fadeInTicks, int fadeOutTicks, int fadeOutThresholdTicks) {
		this.fadeInTicks = fadeInTicks;
		this.fadeOutTicks = fadeOutTicks;
		this.fadeOutThresholdTicks = fadeOutThresholdTicks;
		return this;
	}

	public void forEachAttributeModifier(
			int amplifier,
			BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer
	) {
		this.attributeModifiers
				.forEach(
						(attribute, attributeModifierCreator) -> consumer.accept(
								(RegistryEntry<EntityAttribute>) attribute,
								attributeModifierCreator.createAttributeModifier(amplifier)
						)
				);
	}

	/**
	 * Обрабатывает событие removed.
	 *
	 * @param attributeContainer attribute container
	 */
	public void onRemoved(AttributeContainer attributeContainer) {
		for (Entry<RegistryEntry<EntityAttribute>, StatusEffect.EffectAttributeModifierCreator> entry : this.attributeModifiers.entrySet()) {
			EntityAttributeInstance entityAttributeInstance = attributeContainer.getCustomInstance(entry.getKey());
			if (entityAttributeInstance != null) {
				entityAttributeInstance.removeModifier(entry.getValue().id());
			}
		}
	}

	/**
	 * Обрабатывает событие applied.
	 *
	 * @param attributeContainer attribute container
	 * @param amplifier amplifier
	 */
	public void onApplied(AttributeContainer attributeContainer, int amplifier) {
		for (Entry<RegistryEntry<EntityAttribute>, StatusEffect.EffectAttributeModifierCreator> entry : this.attributeModifiers.entrySet()) {
			EntityAttributeInstance entityAttributeInstance = attributeContainer.getCustomInstance(entry.getKey());
			if (entityAttributeInstance != null) {
				entityAttributeInstance.removeModifier(entry.getValue().id());
				entityAttributeInstance.addPersistentModifier(entry.getValue().createAttributeModifier(amplifier));
			}
		}
	}

	public boolean isBeneficial() {
		return this.category == StatusEffectCategory.BENEFICIAL;
	}

	/**
	 * Создаёт particle.
	 *
	 * @param effect effect
	 *
	 * @return ParticleEffect — результат операции
	 */
	public ParticleEffect createParticle(StatusEffectInstance effect) {
		return this.particleFactory.apply(effect);
	}

	/**
	 * Применяет sound.
	 *
	 * @param sound sound
	 *
	 * @return StatusEffect — результат операции
	 */
	public StatusEffect applySound(SoundEvent sound) {
		this.applySound = Optional.of(sound);
		return this;
	}

	/**
	 * Requires.
	 *
	 * @param requiredFeatures required features
	 *
	 * @return StatusEffect — результат операции
	 */
	public StatusEffect requires(FeatureFlag... requiredFeatures) {
		this.requiredFeatures = FeatureFlags.FEATURE_MANAGER.featureSetOf(requiredFeatures);
		return this;
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return this.requiredFeatures;
	}

	/**
	 * {@code EffectAttributeModifierCreator}.
	 */
	record EffectAttributeModifierCreator(
			Identifier id,
			double baseValue,
			EntityAttributeModifier.Operation operation
	) {

		/**
		 * Создаёт attribute modifier.
		 *
		 * @param amplifier amplifier
		 *
		 * @return EntityAttributeModifier — результат операции
		 */
		public EntityAttributeModifier createAttributeModifier(int amplifier) {
			return new EntityAttributeModifier(this.id, this.baseValue * (amplifier + 1), this.operation);
		}
	}
}
