package net.minecraft.potion;

import com.mojang.serialization.Codec;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureFlag;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.featuretoggle.ToggleableFeature;

import java.util.List;

/**
 * Зелье — именованный набор эффектов статуса, применяемых при употреблении.
 * Базовое имя ({@code baseName}) используется для формирования ключа перевода.
 * Несколько зелий могут разделять одно базовое имя (например, обычное, длинное и усиленное).
 */
public class Potion implements ToggleableFeature {

	public static final Codec<RegistryEntry<Potion>> CODEC = Registries.POTION.getEntryCodec();
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<Potion>> PACKET_CODEC =
			PacketCodecs.registryEntry(RegistryKeys.POTION);

	private final String baseName;
	private final List<StatusEffectInstance> effects;
	private FeatureSet requiredFeatures = FeatureFlags.VANILLA_FEATURES;

	public Potion(String baseName, StatusEffectInstance... effects) {
		this.baseName = baseName;
		this.effects = List.of(effects);
	}

	/**
	 * Помечает зелье как требующее включённых флагов функций.
	 * Используется для зелий, доступных только в экспериментальных режимах.
	 *
	 * @param requiredFeatures флаги функций, необходимые для активации зелья
	 * @return {@code this} для цепочки вызовов
	 */
	public Potion requires(FeatureFlag... requiredFeatures) {
		this.requiredFeatures = FeatureFlags.FEATURE_MANAGER.featureSetOf(requiredFeatures);
		return this;
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return requiredFeatures;
	}

	public List<StatusEffectInstance> getEffects() {
		return effects;
	}

	public String getBaseName() {
		return baseName;
	}

	/**
	 * Проверяет, содержит ли зелье хотя бы один мгновенный эффект.
	 * Мгновенные зелья (лечение, урон) применяются сразу, без длительности.
	 *
	 * @return {@code true}, если среди эффектов есть мгновенный
	 */
	public boolean hasInstantEffect() {
		for (StatusEffectInstance effect : effects) {
			if (effect.getEffectType().value().isInstant()) {
				return true;
			}
		}

		return false;
	}
}
