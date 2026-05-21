package net.minecraft.entity.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * {@code StatusEffectInstance}.
 */
public class StatusEffectInstance implements Comparable<StatusEffectInstance> {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int INFINITE = -1;
	public static final int MIN_AMPLIFIER = 0;
	public static final int MAX_AMPLIFIER = 255;
	public static final Codec<StatusEffectInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    StatusEffect.ENTRY_CODEC.fieldOf("id").forGetter(StatusEffectInstance::getEffectType),
					                    StatusEffectInstance.Parameters.CODEC.forGetter(StatusEffectInstance::asParameters)
			                    )
			                    .apply(instance, StatusEffectInstance::new)
	);
	public static final PacketCodec<RegistryByteBuf, StatusEffectInstance> PACKET_CODEC = PacketCodec.tuple(
			StatusEffect.ENTRY_PACKET_CODEC,
			StatusEffectInstance::getEffectType,
			StatusEffectInstance.Parameters.PACKET_CODEC,
			StatusEffectInstance::asParameters,
			StatusEffectInstance::new
	);
	private final RegistryEntry<StatusEffect> type;
	private int duration;
	private int amplifier;
	private boolean ambient;
	private boolean showParticles;
	private boolean showIcon;
	private @Nullable StatusEffectInstance hiddenEffect;
	private final StatusEffectInstance.Fading fading = new StatusEffectInstance.Fading();

	public StatusEffectInstance(RegistryEntry<StatusEffect> effect) {
		this(effect, 0, 0);
	}

	public StatusEffectInstance(RegistryEntry<StatusEffect> effect, int duration) {
		this(effect, duration, 0);
	}

	public StatusEffectInstance(RegistryEntry<StatusEffect> effect, int duration, int amplifier) {
		this(effect, duration, amplifier, false, true);
	}

	public StatusEffectInstance(
			RegistryEntry<StatusEffect> effect,
			int duration,
			int amplifier,
			boolean ambient,
			boolean visible
	) {
		this(effect, duration, amplifier, ambient, visible, visible);
	}

	public StatusEffectInstance(
			RegistryEntry<StatusEffect> effect,
			int duration,
			int amplifier,
			boolean ambient,
			boolean showParticles,
			boolean showIcon
	) {
		this(effect, duration, amplifier, ambient, showParticles, showIcon, null);
	}

	public StatusEffectInstance(
			RegistryEntry<StatusEffect> effect,
			int duration,
			int amplifier,
			boolean ambient,
			boolean showParticles,
			boolean showIcon,
			@Nullable StatusEffectInstance hiddenEffect
	) {
		this.type = effect;
		this.duration = duration;
		this.amplifier = MathHelper.clamp(amplifier, 0, 255);
		this.ambient = ambient;
		this.showParticles = showParticles;
		this.showIcon = showIcon;
		this.hiddenEffect = hiddenEffect;
	}

	public StatusEffectInstance(StatusEffectInstance instance) {
		this.type = instance.type;
		this.copyFrom(instance);
	}

	private StatusEffectInstance(RegistryEntry<StatusEffect> effect, StatusEffectInstance.Parameters parameters) {
		this(
				effect,
				parameters.duration(),
				parameters.amplifier(),
				parameters.ambient(),
				parameters.showParticles(),
				parameters.showIcon(),
				parameters.hiddenEffect().map(parametersx -> new StatusEffectInstance(effect, parametersx)).orElse(null)
		);
	}

	private StatusEffectInstance.Parameters asParameters() {
		return new StatusEffectInstance.Parameters(
				this.getAmplifier(),
				this.getDuration(),
				this.isAmbient(),
				this.shouldShowParticles(),
				this.shouldShowIcon(),
				Optional.ofNullable(this.hiddenEffect).map(StatusEffectInstance::asParameters)
		);
	}

	public float getFadeFactor(LivingEntity entity, float tickProgress) {
		return this.fading.calculate(entity, tickProgress);
	}

	/**
	 * Создаёт particle.
	 *
	 * @return ParticleEffect — результат операции
	 */
	public ParticleEffect createParticle() {
		return this.type.value().createParticle(this);
	}

	void copyFrom(StatusEffectInstance that) {
		this.duration = that.duration;
		this.amplifier = that.amplifier;
		this.ambient = that.ambient;
		this.showParticles = that.showParticles;
		this.showIcon = that.showIcon;
	}

	/**
	 * Upgrade.
	 *
	 * @param that that
	 *
	 * @return boolean — результат операции
	 */
	public boolean upgrade(StatusEffectInstance that) {
		if (!this.type.equals(that.type)) {
			LOGGER.warn("This method should only be called for matching effects!");
		}

		boolean bl = false;
		if (that.amplifier > this.amplifier) {
			if (that.lastsShorterThan(this)) {
				StatusEffectInstance statusEffectInstance = this.hiddenEffect;
				this.hiddenEffect = new StatusEffectInstance(this);
				this.hiddenEffect.hiddenEffect = statusEffectInstance;
			}

			this.amplifier = that.amplifier;
			this.duration = that.duration;
			bl = true;
		}
		else if (this.lastsShorterThan(that)) {
			if (that.amplifier == this.amplifier) {
				this.duration = that.duration;
				bl = true;
			}
			else if (this.hiddenEffect == null) {
				this.hiddenEffect = new StatusEffectInstance(that);
			}
			else {
				this.hiddenEffect.upgrade(that);
			}
		}

		if (!that.ambient && this.ambient || bl) {
			this.ambient = that.ambient;
			bl = true;
		}

		if (that.showParticles != this.showParticles) {
			this.showParticles = that.showParticles;
			bl = true;
		}

		if (that.showIcon != this.showIcon) {
			this.showIcon = that.showIcon;
			bl = true;
		}

		return bl;
	}

	private boolean lastsShorterThan(StatusEffectInstance effect) {
		return !this.isInfinite() && (this.duration < effect.duration || effect.isInfinite());
	}

	public boolean isInfinite() {
		return this.duration == -1;
	}

	public boolean isDurationBelow(int duration) {
		return !this.isInfinite() && this.duration <= duration;
	}

	/**
	 * With scaled duration.
	 *
	 * @param durationMultiplier duration multiplier
	 *
	 * @return StatusEffectInstance — результат операции
	 */
	public StatusEffectInstance withScaledDuration(float durationMultiplier) {
		StatusEffectInstance statusEffectInstance = new StatusEffectInstance(this);
		statusEffectInstance.duration =
				statusEffectInstance.mapDuration(duration -> Math.max(
						MathHelper.floor(duration * durationMultiplier),
						1
				));
		return statusEffectInstance;
	}

	/**
	 * Map duration.
	 *
	 * @param mapper mapper
	 *
	 * @return int — результат операции
	 */
	public int mapDuration(Int2IntFunction mapper) {
		return !this.isInfinite() && this.duration != 0 ? mapper.applyAsInt(this.duration) : this.duration;
	}

	public RegistryEntry<StatusEffect> getEffectType() {
		return this.type;
	}

	public int getDuration() {
		return this.duration;
	}

	public int getAmplifier() {
		return this.amplifier;
	}

	public boolean isAmbient() {
		return this.ambient;
	}

	/**
	 * Определяет, следует ли show particles.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldShowParticles() {
		return this.showParticles;
	}

	/**
	 * Определяет, следует ли show icon.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldShowIcon() {
		return this.showIcon;
	}

	/**
	 * Update.
	 *
	 * @param world world
	 * @param entity entity
	 * @param hiddenEffectCallback hidden effect callback
	 *
	 * @return boolean — результат операции
	 */
	public boolean update(ServerWorld world, LivingEntity entity, Runnable hiddenEffectCallback) {
		if (!this.isActive()) {
			return false;
		}
		else {
			int i = this.isInfinite() ? entity.age : this.duration;
			if (this.type.value().canApplyUpdateEffect(i, this.amplifier) && !this.type
					.value()
					.applyUpdateEffect(world, entity, this.amplifier)) {
				return false;
			}
			else {
				this.updateDuration();
				if (this.tickHiddenEffect()) {
					hiddenEffectCallback.run();
				}

				return this.isActive();
			}
		}
	}

	/**
	 * Выполняет тик обновления для client.
	 */
	public void tickClient() {
		if (this.isActive()) {
			this.updateDuration();
			this.tickHiddenEffect();
		}

		this.fading.update(this);
	}

	private boolean isActive() {
		return this.isInfinite() || this.duration > 0;
	}

	private void updateDuration() {
		if (this.hiddenEffect != null) {
			this.hiddenEffect.updateDuration();
		}

		this.duration = this.mapDuration(duration -> duration - 1);
	}

	private boolean tickHiddenEffect() {
		if (this.duration == 0 && this.hiddenEffect != null) {
			this.copyFrom(this.hiddenEffect);
			this.hiddenEffect = this.hiddenEffect.hiddenEffect;
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Обрабатывает событие applied.
	 *
	 * @param entity entity
	 */
	public void onApplied(LivingEntity entity) {
		this.type.value().onApplied(entity, this.amplifier);
	}

	/**
	 * Обрабатывает событие entity removal.
	 *
	 * @param world world
	 * @param entity entity
	 * @param reason reason
	 */
	public void onEntityRemoval(ServerWorld world, LivingEntity entity, Entity.RemovalReason reason) {
		this.type.value().onEntityRemoval(world, entity, this.amplifier, reason);
	}

	/**
	 * Обрабатывает событие entity damage.
	 *
	 * @param world world
	 * @param entity entity
	 * @param source source
	 * @param amount amount
	 */
	public void onEntityDamage(ServerWorld world, LivingEntity entity, DamageSource source, float amount) {
		this.type.value().onEntityDamage(world, entity, this.amplifier, source, amount);
	}

	public String getTranslationKey() {
		return this.type.value().getTranslationKey();
	}

	@Override
	public String toString() {
		String string;
		if (this.amplifier > 0) {
			string =
					this.getTranslationKey() + " x " + (this.amplifier + 1) + ", Duration: " + this.getDurationString();
		}
		else {
			string = this.getTranslationKey() + ", Duration: " + this.getDurationString();
		}

		if (!this.showParticles) {
			string = string + ", Particles: false";
		}

		if (!this.showIcon) {
			string = string + ", Show Icon: false";
		}

		return string;
	}

	private String getDurationString() {
		return this.isInfinite() ? "infinite" : Integer.toString(this.duration);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else {
			return !(o instanceof StatusEffectInstance statusEffectInstance)
			       ? false
			       : this.duration == statusEffectInstance.duration
			         && this.amplifier == statusEffectInstance.amplifier
			         && this.ambient == statusEffectInstance.ambient
			         && this.showParticles == statusEffectInstance.showParticles
			         && this.showIcon == statusEffectInstance.showIcon
			         && this.type.equals(statusEffectInstance.type);
		}
	}

	@Override
	public int hashCode() {
		int i = this.type.hashCode();
		i = 31 * i + this.duration;
		i = 31 * i + this.amplifier;
		i = 31 * i + (this.ambient ? 1 : 0);
		i = 31 * i + (this.showParticles ? 1 : 0);
		return 31 * i + (this.showIcon ? 1 : 0);
	}

	/**
	 * Compare to.
	 *
	 * @param statusEffectInstance status effect instance
	 *
	 * @return int — результат операции
	 */
	public int compareTo(StatusEffectInstance statusEffectInstance) {
		int i = 32147;
		return (this.getDuration() <= 32147 || statusEffectInstance.getDuration() <= 32147) && (!this.isAmbient()
				|| !statusEffectInstance.isAmbient()
		)
		       ? ComparisonChain.start()
		                        .compareFalseFirst(this.isAmbient(), statusEffectInstance.isAmbient())
		                        .compareFalseFirst(this.isInfinite(), statusEffectInstance.isInfinite())
		                        .compare(this.getDuration(), statusEffectInstance.getDuration())
		                        .compare(
				                        this.getEffectType().value().getColor(),
				                        statusEffectInstance.getEffectType().value().getColor()
		                        )
		                        .result()
		       : ComparisonChain.start()
		                        .compare(this.isAmbient(), statusEffectInstance.isAmbient())
		                        .compare(
				                        this.getEffectType().value().getColor(),
				                        statusEffectInstance.getEffectType().value().getColor()
		                        )
		                        .result();
	}

	/**
	 * Play apply sound.
	 *
	 * @param entity entity
	 */
	public void playApplySound(LivingEntity entity) {
		this.type.value().playApplySound(entity, this.amplifier);
	}

	/**
	 * Equals.
	 *
	 * @param effect effect
	 *
	 * @return boolean — результат операции
	 */
	public boolean equals(RegistryEntry<StatusEffect> effect) {
		return this.type.equals(effect);
	}

	/**
	 * Создаёт копию fading from.
	 *
	 * @param effect effect
	 */
	public void copyFadingFrom(StatusEffectInstance effect) {
		this.fading.copyFrom(effect.fading);
	}

	/**
	 * Skip fading.
	 */
	public void skipFading() {
		this.fading.skipFading(this);
	}

	/**
	 * {@code Fading}.
	 */
	static class Fading {

		private float factor;
		private float lastFactor;

		/**
		 * Skip fading.
		 *
		 * @param effect effect
		 */
		public void skipFading(StatusEffectInstance effect) {
			this.factor = shouldFadeIn(effect) ? 1.0F : 0.0F;
			this.lastFactor = this.factor;
		}

		/**
		 * Создаёт копию from.
		 *
		 * @param fading fading
		 */
		public void copyFrom(StatusEffectInstance.Fading fading) {
			this.factor = fading.factor;
			this.lastFactor = fading.lastFactor;
		}

		/**
		 * Update.
		 *
		 * @param effect effect
		 */
		public void update(StatusEffectInstance effect) {
			this.lastFactor = this.factor;
			boolean bl = shouldFadeIn(effect);
			float f = bl ? 1.0F : 0.0F;
			if (this.factor != f) {
				StatusEffect statusEffect = effect.getEffectType().value();
				int i = bl ? statusEffect.getFadeInTicks() : statusEffect.getFadeOutTicks();
				if (i == 0) {
					this.factor = f;
				}
				else {
					float g = 1.0F / i;
					this.factor = this.factor + MathHelper.clamp(f - this.factor, -g, g);
				}
			}
		}

		private static boolean shouldFadeIn(StatusEffectInstance effect) {
			return !effect.isDurationBelow(effect.getEffectType().value().getFadeOutThresholdTicks());
		}

		/**
		 * Calculate.
		 *
		 * @param entity entity
		 * @param tickProgress tick progress
		 *
		 * @return float — результат операции
		 */
		public float calculate(LivingEntity entity, float tickProgress) {
			if (entity.isRemoved()) {
				this.lastFactor = this.factor;
			}

			return MathHelper.lerp(tickProgress, this.lastFactor, this.factor);
		}
	}

	/**
	 * {@code Parameters}.
	 */
	record Parameters(
			int amplifier,
			int duration,
			boolean ambient,
			boolean showParticles,
			boolean showIcon,
			Optional<StatusEffectInstance.Parameters> hiddenEffect
	) {

		public static final MapCodec<StatusEffectInstance.Parameters> CODEC = MapCodec.recursive(
				"MobEffectInstance.Details",
				codec -> RecordCodecBuilder.mapCodec(
						instance -> instance.group(
								                    Codecs.UNSIGNED_BYTE
										                    .optionalFieldOf("amplifier", 0)
										                    .forGetter(StatusEffectInstance.Parameters::amplifier),
								                    Codec.INT
										                    .optionalFieldOf("duration", 0)
										                    .forGetter(StatusEffectInstance.Parameters::duration),
								                    Codec.BOOL
										                    .optionalFieldOf("ambient", false)
										                    .forGetter(StatusEffectInstance.Parameters::ambient),
								                    Codec.BOOL
										                    .optionalFieldOf("show_particles", true)
										                    .forGetter(StatusEffectInstance.Parameters::showParticles),
								                    Codec.BOOL
										                    .optionalFieldOf("show_icon")
										                    .forGetter(parameters -> Optional.of(parameters.showIcon())),
								                    codec
										                    .optionalFieldOf("hidden_effect")
										                    .forGetter(StatusEffectInstance.Parameters::hiddenEffect)
						                    )
						                    .apply(instance, StatusEffectInstance.Parameters::create)
				)
		);
		public static final PacketCodec<ByteBuf, StatusEffectInstance.Parameters> PACKET_CODEC = PacketCodec.recursive(
				packetCodec -> PacketCodec.tuple(
						PacketCodecs.VAR_INT,
						StatusEffectInstance.Parameters::amplifier,
						PacketCodecs.VAR_INT,
						StatusEffectInstance.Parameters::duration,
						PacketCodecs.BOOLEAN,
						StatusEffectInstance.Parameters::ambient,
						PacketCodecs.BOOLEAN,
						StatusEffectInstance.Parameters::showParticles,
						PacketCodecs.BOOLEAN,
						StatusEffectInstance.Parameters::showIcon,
						packetCodec.collect(PacketCodecs::optional),
						StatusEffectInstance.Parameters::hiddenEffect,
						StatusEffectInstance.Parameters::new
				)
		);

		private static StatusEffectInstance.Parameters create(
				int amplifier,
				int duration,
				boolean ambient,
				boolean showParticles,
				Optional<Boolean> showIcon,
				Optional<StatusEffectInstance.Parameters> hiddenEffect
		) {
			return new StatusEffectInstance.Parameters(
					amplifier,
					duration,
					ambient,
					showParticles,
					showIcon.orElse(showParticles),
					hiddenEffect
			);
		}
	}
}
