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
 * Экземпляр статусного эффекта, применённого к сущности.
 *
 * <p>Хранит тип эффекта, оставшуюся длительность, уровень усиления и флаги отображения.
 * Поддерживает стек скрытых эффектов ({@code hiddenEffect}): когда активный эффект заканчивается,
 * автоматически восстанавливается предыдущий с меньшим уровнем усиления.</p>
 */
public class StatusEffectInstance implements Comparable<StatusEffectInstance> {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final int INFINITE = -1;
	public static final int MIN_AMPLIFIER = 0;
	public static final int MAX_AMPLIFIER = 255;

	/**
	 * Порог длительности, ниже которого эффект считается «коротким» при сортировке.
	 * Используется в {@link #compareTo} для выбора стратегии сравнения.
	 */
	private static final int SHORT_DURATION_THRESHOLD = 32147;

	public static final Codec<StatusEffectInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					StatusEffect.ENTRY_CODEC.fieldOf("id").forGetter(StatusEffectInstance::getEffectType),
					Parameters.CODEC.forGetter(StatusEffectInstance::asParameters)
			).apply(instance, StatusEffectInstance::new)
	);
	public static final PacketCodec<RegistryByteBuf, StatusEffectInstance> PACKET_CODEC = PacketCodec.tuple(
			StatusEffect.ENTRY_PACKET_CODEC,
			StatusEffectInstance::getEffectType,
			Parameters.PACKET_CODEC,
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
	private final Fading fading = new Fading();

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
		type = effect;
		this.duration = duration;
		this.amplifier = MathHelper.clamp(amplifier, 0, MAX_AMPLIFIER);
		this.ambient = ambient;
		this.showParticles = showParticles;
		this.showIcon = showIcon;
		this.hiddenEffect = hiddenEffect;
	}

	/** Конструктор копирования: копирует все поля из переданного экземпляра. */
	public StatusEffectInstance(StatusEffectInstance instance) {
		type = instance.type;
		copyFrom(instance);
	}

	private StatusEffectInstance(RegistryEntry<StatusEffect> effect, Parameters parameters) {
		this(
				effect,
				parameters.duration(),
				parameters.amplifier(),
				parameters.ambient(),
				parameters.showParticles(),
				parameters.showIcon(),
				parameters.hiddenEffect()
						.map(nested -> new StatusEffectInstance(effect, nested))
						.orElse(null)
		);
	}

	private Parameters asParameters() {
		return new Parameters(
				getAmplifier(),
				getDuration(),
				isAmbient(),
				shouldShowParticles(),
				shouldShowIcon(),
				Optional.ofNullable(hiddenEffect).map(StatusEffectInstance::asParameters)
		);
	}

	public float getFadeFactor(LivingEntity entity, float tickProgress) {
		return fading.calculate(entity, tickProgress);
	}

	public ParticleEffect createParticle() {
		return type.value().createParticle(this);
	}

	void copyFrom(StatusEffectInstance that) {
		duration = that.duration;
		amplifier = that.amplifier;
		ambient = that.ambient;
		showParticles = that.showParticles;
		showIcon = that.showIcon;
	}

	/**
	 * Пытается обновить текущий экземпляр данными из нового эффекта того же типа.
	 *
	 * <p>Логика приоритетов:
	 * <ul>
	 *   <li>Если новый эффект сильнее — он становится активным, старый уходит в стек скрытых.</li>
	 *   <li>Если новый эффект того же уровня, но длиннее — обновляется длительность.</li>
	 *   <li>Если новый эффект слабее, но длиннее — он добавляется в стек скрытых.</li>
	 * </ul>
	 * </p>
	 *
	 * @param that новый эффект для слияния
	 * @return {@code true}, если хотя бы одно поле было изменено
	 */
	public boolean upgrade(StatusEffectInstance that) {
		if (type.equals(that.type) == false) {
			LOGGER.warn("This method should only be called for matching effects!");
		}

		boolean upgraded = false;

		if (that.amplifier > amplifier) {
			if (that.lastsShorterThan(this)) {
				StatusEffectInstance previous = hiddenEffect;
				hiddenEffect = new StatusEffectInstance(this);
				hiddenEffect.hiddenEffect = previous;
			}

			amplifier = that.amplifier;
			duration = that.duration;
			upgraded = true;
		} else if (lastsShorterThan(that)) {
			if (that.amplifier == amplifier) {
				duration = that.duration;
				upgraded = true;
			} else if (hiddenEffect == null) {
				hiddenEffect = new StatusEffectInstance(that);
			} else {
				hiddenEffect.upgrade(that);
			}
		}

		if (that.ambient == false && ambient || upgraded) {
			ambient = that.ambient;
			upgraded = true;
		}

		if (that.showParticles != showParticles) {
			showParticles = that.showParticles;
			upgraded = true;
		}

		if (that.showIcon != showIcon) {
			showIcon = that.showIcon;
			upgraded = true;
		}

		return upgraded;
	}

	/**
	 * Проверяет, заканчивается ли этот эффект раньше переданного.
	 * Бесконечный эффект никогда не считается «более коротким».
	 */
	private boolean lastsShorterThan(StatusEffectInstance effect) {
		return !isInfinite() && (duration < effect.duration || effect.isInfinite());
	}

	public boolean isInfinite() {
		return duration == INFINITE;
	}

	public boolean isDurationBelow(int threshold) {
		return !isInfinite() && duration <= threshold;
	}

	/**
	 * Создаёт копию этого экземпляра с длительностью, умноженной на {@code durationMultiplier}.
	 * Результирующая длительность не может быть меньше 1 тика.
	 *
	 * @param durationMultiplier множитель длительности
	 * @return новый экземпляр с масштабированной длительностью
	 */
	public StatusEffectInstance withScaledDuration(float durationMultiplier) {
		StatusEffectInstance scaled = new StatusEffectInstance(this);
		scaled.duration = scaled.mapDuration(
				d -> Math.max(MathHelper.floor(d * durationMultiplier), 1)
		);
		return scaled;
	}

	/**
	 * Применяет функцию-маппер к длительности, если эффект не бесконечный и не нулевой.
	 *
	 * @param mapper функция преобразования длительности
	 * @return преобразованная длительность, либо исходная если эффект бесконечный/нулевой
	 */
	public int mapDuration(Int2IntFunction mapper) {
		return !isInfinite() && duration != 0 ? mapper.applyAsInt(duration) : duration;
	}

	public RegistryEntry<StatusEffect> getEffectType() {
		return type;
	}

	public int getDuration() {
		return duration;
	}

	public int getAmplifier() {
		return amplifier;
	}

	public boolean isAmbient() {
		return ambient;
	}

	public boolean shouldShowParticles() {
		return showParticles;
	}

	public boolean shouldShowIcon() {
		return showIcon;
	}

	/**
	 * Выполняет серверный тик эффекта: применяет логику и уменьшает длительность.
	 *
	 * @param world                серверный мир
	 * @param entity               сущность-носитель
	 * @param hiddenEffectCallback вызывается, когда активный эффект истёк и восстановлен скрытый
	 * @return {@code true}, если эффект ещё активен после тика
	 */
	public boolean update(ServerWorld world, LivingEntity entity, Runnable hiddenEffectCallback) {
		if (!isActive()) {
			return false;
		}

		int ticksForCheck = isInfinite() ? entity.age : duration;

		if (type.value().canApplyUpdateEffect(ticksForCheck, amplifier)
				&& !type.value().applyUpdateEffect(world, entity, amplifier)) {
			return false;
		}

		updateDuration();

		if (tickHiddenEffect()) {
			hiddenEffectCallback.run();
		}

		return isActive();
	}

	/**
	 * Выполняет клиентский тик: обновляет длительность и состояние затухания для анимации.
	 */
	public void tickClient() {
		if (isActive()) {
			updateDuration();
			tickHiddenEffect();
		}

		fading.update(this);
	}

	private boolean isActive() {
		return isInfinite() || duration > 0;
	}

	private void updateDuration() {
		if (hiddenEffect != null) {
			hiddenEffect.updateDuration();
		}

		duration = mapDuration(d -> d - 1);
	}

	/**
	 * Если активный эффект истёк (duration == 0) и есть скрытый — восстанавливает его.
	 *
	 * @return {@code true}, если произошло восстановление скрытого эффекта
	 */
	private boolean tickHiddenEffect() {
		if (duration == 0 && hiddenEffect != null) {
			copyFrom(hiddenEffect);
			hiddenEffect = hiddenEffect.hiddenEffect;
			return true;
		}

		return false;
	}

	public void onApplied(LivingEntity entity) {
		type.value().onApplied(entity, amplifier);
	}

	public void onEntityRemoval(ServerWorld world, LivingEntity entity, Entity.RemovalReason reason) {
		type.value().onEntityRemoval(world, entity, amplifier, reason);
	}

	public void onEntityDamage(ServerWorld world, LivingEntity entity, DamageSource source, float amount) {
		type.value().onEntityDamage(world, entity, amplifier, source, amount);
	}

	public String getTranslationKey() {
		return type.value().getTranslationKey();
	}

	@Override
	public String toString() {
		String base = amplifier > 0
				? getTranslationKey() + " x " + (amplifier + 1) + ", Duration: " + getDurationString()
				: getTranslationKey() + ", Duration: " + getDurationString();

		String result = base;

		if (!showParticles) {
			result = result + ", Particles: false";
		}

		if (!showIcon) {
			result = result + ", Show Icon: false";
		}

		return result;
	}

	private String getDurationString() {
		return isInfinite() ? "infinite" : Integer.toString(duration);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof StatusEffectInstance other)) {
			return false;
		}

		return duration == other.duration
				&& amplifier == other.amplifier
				&& ambient == other.ambient
				&& showParticles == other.showParticles
				&& showIcon == other.showIcon
				&& type.equals(other.type);
	}

	@Override
	public int hashCode() {
		int hash = type.hashCode();
		hash = 31 * hash + duration;
		hash = 31 * hash + amplifier;
		hash = 31 * hash + (ambient ? 1 : 0);
		hash = 31 * hash + (showParticles ? 1 : 0);
		return 31 * hash + (showIcon ? 1 : 0);
	}

	/**
	 * Сравнивает два экземпляра для сортировки в HUD.
	 *
	 * <p>Если хотя бы один из эффектов «короткий» (длительность ≤ {@value #SHORT_DURATION_THRESHOLD})
	 * или оба не ambient — сортировка идёт по приоритету отображения (ambient последними,
	 * бесконечные последними, затем по длительности и цвету).
	 * Иначе (оба ambient и оба длинные) — сортировка только по ambient-флагу и цвету.</p>
	 */
	@Override
	public int compareTo(StatusEffectInstance other) {
		boolean eitherShort = getDuration() <= SHORT_DURATION_THRESHOLD
				|| other.getDuration() <= SHORT_DURATION_THRESHOLD;
		boolean bothAmbient = isAmbient() && other.isAmbient();

		if (eitherShort || !bothAmbient) {
			return ComparisonChain.start()
					.compareFalseFirst(isAmbient(), other.isAmbient())
					.compareFalseFirst(isInfinite(), other.isInfinite())
					.compare(getDuration(), other.getDuration())
					.compare(getEffectType().value().getColor(), other.getEffectType().value().getColor())
					.result();
		}

		return ComparisonChain.start()
				.compare(isAmbient(), other.isAmbient())
				.compare(getEffectType().value().getColor(), other.getEffectType().value().getColor())
				.result();
	}

	public void playApplySound(LivingEntity entity) {
		type.value().playApplySound(entity, amplifier);
	}

	public boolean equals(RegistryEntry<StatusEffect> effect) {
		return type.equals(effect);
	}

	public void copyFadingFrom(StatusEffectInstance effect) {
		fading.copyFrom(effect.fading);
	}

	public void skipFading() {
		fading.skipFading(this);
	}

	// -------------------------------------------------------------------------
	// Вложенные классы
	// -------------------------------------------------------------------------

	/**
	 * Управляет плавным появлением и исчезновением эффекта (fade in/out).
	 *
	 * <p>Хранит текущий и предыдущий коэффициент затухания для интерполяции между тиками.</p>
	 */
	static class Fading {

		private float factor;
		private float lastFactor;

		/**
		 * Мгновенно устанавливает коэффициент затухания без анимации.
		 * Используется при первом применении эффекта.
		 */
		public void skipFading(StatusEffectInstance effect) {
			factor = shouldFadeIn(effect) ? 1.0F : 0.0F;
			lastFactor = factor;
		}

		public void copyFrom(Fading other) {
			factor = other.factor;
			lastFactor = other.lastFactor;
		}

		/**
		 * Обновляет коэффициент затухания на один тик.
		 * Скорость изменения ограничена {@code 1 / fadeInTicks} или {@code 1 / fadeOutTicks}.
		 */
		public void update(StatusEffectInstance effect) {
			lastFactor = factor;
			boolean fadingIn = shouldFadeIn(effect);
			float target = fadingIn ? 1.0F : 0.0F;

			if (factor == target) {
				return;
			}

			StatusEffect statusEffect = effect.getEffectType().value();
			int fadeTicks = fadingIn ? statusEffect.getFadeInTicks() : statusEffect.getFadeOutTicks();

			if (fadeTicks == 0) {
				factor = target;
			} else {
				float step = 1.0F / fadeTicks;
				factor = factor + MathHelper.clamp(target - factor, -step, step);
			}
		}

		private static boolean shouldFadeIn(StatusEffectInstance effect) {
			return !effect.isDurationBelow(effect.getEffectType().value().getFadeOutThresholdTicks());
		}

		/**
		 * Вычисляет интерполированный коэффициент затухания для рендера.
		 *
		 * @param entity       сущность-носитель (если удалена — используется последнее значение)
		 * @param tickProgress прогресс между тиками (0.0–1.0) для плавной анимации
		 * @return интерполированный коэффициент [0.0, 1.0]
		 */
		public float calculate(LivingEntity entity, float tickProgress) {
			if (entity.isRemoved()) {
				lastFactor = factor;
			}

			return MathHelper.lerp(tickProgress, lastFactor, factor);
		}
	}

	/**
	 * Сериализуемые параметры экземпляра эффекта (без типа).
	 * Используется в кодеках для разделения типа и параметров.
	 */
	record Parameters(
			int amplifier,
			int duration,
			boolean ambient,
			boolean showParticles,
			boolean showIcon,
			Optional<Parameters> hiddenEffect
	) {

		public static final MapCodec<Parameters> CODEC = MapCodec.recursive(
				"MobEffectInstance.Details",
				codec -> RecordCodecBuilder.mapCodec(
						instance -> instance.group(
								Codecs.UNSIGNED_BYTE
										.optionalFieldOf("amplifier", 0)
										.forGetter(Parameters::amplifier),
								Codec.INT
										.optionalFieldOf("duration", 0)
										.forGetter(Parameters::duration),
								Codec.BOOL
										.optionalFieldOf("ambient", false)
										.forGetter(Parameters::ambient),
								Codec.BOOL
										.optionalFieldOf("show_particles", true)
										.forGetter(Parameters::showParticles),
								Codec.BOOL
										.optionalFieldOf("show_icon")
										.forGetter(parameters -> Optional.of(parameters.showIcon())),
								codec
										.optionalFieldOf("hidden_effect")
										.forGetter(Parameters::hiddenEffect)
						).apply(instance, Parameters::create)
				)
		);
		public static final PacketCodec<ByteBuf, Parameters> PACKET_CODEC = PacketCodec.recursive(
				packetCodec -> PacketCodec.tuple(
						PacketCodecs.VAR_INT,
						Parameters::amplifier,
						PacketCodecs.VAR_INT,
						Parameters::duration,
						PacketCodecs.BOOLEAN,
						Parameters::ambient,
						PacketCodecs.BOOLEAN,
						Parameters::showParticles,
						PacketCodecs.BOOLEAN,
						Parameters::showIcon,
						packetCodec.collect(PacketCodecs::optional),
						Parameters::hiddenEffect,
						Parameters::new
				)
		);

		/**
		 * Фабричный метод для десериализации: если {@code showIcon} отсутствует в данных,
		 * значение берётся из {@code showParticles} (обратная совместимость).
		 */
		private static Parameters create(
				int amplifier,
				int duration,
				boolean ambient,
				boolean showParticles,
				Optional<Boolean> showIcon,
				Optional<Parameters> hiddenEffect
		) {
			return new Parameters(
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
