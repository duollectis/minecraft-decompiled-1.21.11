package net.minecraft.entity.mob;

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
import net.minecraft.util.ModelAndTexture;
import net.minecraft.util.StringIdentifiable;

import java.util.List;

/**
 * {@code ZombieNautilusVariant}.
 */
public record ZombieNautilusVariant(
		ModelAndTexture<ZombieNautilusVariant.Model> modelAndTexture,
		SpawnConditionSelectors spawnConditions
)
		implements VariantSelectorProvider<SpawnContext, SpawnCondition> {

	public static final Codec<ZombieNautilusVariant> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    ModelAndTexture
							                    .createMapCodec(ZombieNautilusVariant.Model.CODEC, ZombieNautilusVariant.Model.NORMAL)
							                    .forGetter(ZombieNautilusVariant::modelAndTexture),
					                    SpawnConditionSelectors.CODEC
							                    .fieldOf("spawn_conditions")
							                    .forGetter(ZombieNautilusVariant::spawnConditions)
			                    )
			                    .apply(instance, ZombieNautilusVariant::new)
	);
	public static final Codec<ZombieNautilusVariant> NETWORK_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    ModelAndTexture
							                    .createMapCodec(ZombieNautilusVariant.Model.CODEC, ZombieNautilusVariant.Model.NORMAL)
							                    .forGetter(ZombieNautilusVariant::modelAndTexture)
			                    )
			                    .apply(instance, ZombieNautilusVariant::new)
	);
	public static final Codec<RegistryEntry<ZombieNautilusVariant>>
			ENTRY_CODEC =
			RegistryFixedCodec.of(RegistryKeys.ZOMBIE_NAUTILUS_VARIANT);
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<ZombieNautilusVariant>>
			ENTRY_PACKET_CODEC =
			PacketCodecs.registryEntry(
					RegistryKeys.ZOMBIE_NAUTILUS_VARIANT
			);

	private ZombieNautilusVariant(ModelAndTexture<ZombieNautilusVariant.Model> modelAndTexture) {
		this(modelAndTexture, SpawnConditionSelectors.EMPTY);
	}

	@Override
	public List<VariantSelectorProvider.Selector<SpawnContext, SpawnCondition>> getSelectors() {
		return this.spawnConditions.selectors();
	}

	/**
	 * {@code Model}.
	 */
	public static enum Model implements StringIdentifiable {
		NORMAL("normal"),
		WARM("warm");

		public static final Codec<ZombieNautilusVariant.Model>
				CODEC =
				StringIdentifiable.createCodec(ZombieNautilusVariant.Model::values);
		private final String id;

		private Model(final String id) {
			this.id = id;
		}

		@Override
		public String asString() {
			return this.id;
		}
	}
}
