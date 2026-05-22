package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.VariantSelectorProvider;
import net.minecraft.entity.spawn.SpawnCondition;
import net.minecraft.entity.spawn.SpawnConditionSelectors;
import net.minecraft.entity.spawn.SpawnContext;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.util.AssetInfo;

import java.util.List;

/**
 * Вариант волка: определяет текстуру и биом спавна.
 */
public record WolfVariant(WolfVariant.WolfAssetInfo assetInfo, SpawnConditionSelectors spawnConditions)
		implements VariantSelectorProvider<SpawnContext, SpawnCondition> {

	public static final Codec<WolfVariant> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    WolfVariant.WolfAssetInfo.CODEC.fieldOf("assets").forGetter(WolfVariant::assetInfo),
					                    SpawnConditionSelectors.CODEC.fieldOf("spawn_conditions").forGetter(WolfVariant::spawnConditions)
			                    )
			                    .apply(instance, WolfVariant::new)
	);
	public static final Codec<WolfVariant> NETWORK_CODEC = RecordCodecBuilder.create(
			instance -> instance
					.group(WolfVariant.WolfAssetInfo.CODEC.fieldOf("assets").forGetter(WolfVariant::assetInfo))
					.apply(instance, WolfVariant::new)
	);
	public static final Codec<RegistryEntry<WolfVariant>>
			ENTRY_CODEC =
			RegistryFixedCodec.of(RegistryKeys.WOLF_VARIANT);
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<WolfVariant>>
			ENTRY_PACKET_CODEC =
			PacketCodecs.registryEntry(RegistryKeys.WOLF_VARIANT);

	private WolfVariant(WolfVariant.WolfAssetInfo assetInfo) {
		this(assetInfo, SpawnConditionSelectors.EMPTY);
	}

	@Override
	public List<VariantSelectorProvider.Selector<SpawnContext, SpawnCondition>> getSelectors() {
		return this.spawnConditions.selectors();
	}

	/**
 * Данные варианта волка для сериализации.
 */
	public record WolfAssetInfo(
			AssetInfo.TextureAssetInfo wild,
			AssetInfo.TextureAssetInfo tame,
			AssetInfo.TextureAssetInfo angry
	) {

		public static final Codec<WolfVariant.WolfAssetInfo> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    AssetInfo.TextureAssetInfo.CODEC.fieldOf("wild").forGetter(WolfVariant.WolfAssetInfo::wild),
						                    AssetInfo.TextureAssetInfo.CODEC.fieldOf("tame").forGetter(WolfVariant.WolfAssetInfo::tame),
						                    AssetInfo.TextureAssetInfo.CODEC.fieldOf("angry").forGetter(WolfVariant.WolfAssetInfo::angry)
				                    )
				                    .apply(instance, WolfVariant.WolfAssetInfo::new)
		);
	}
}
