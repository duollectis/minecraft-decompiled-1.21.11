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
import net.minecraft.util.ModelAndTexture;
import net.minecraft.util.StringIdentifiable;

import java.util.List;

/**
 * Вариант коровы, определяющий модель, текстуру и условия спавна.
 * Используется для климатических вариантов: обычная, тёплая, холодная.
 */
public record CowVariant(ModelAndTexture<CowVariant.Model> modelAndTexture, SpawnConditionSelectors spawnConditions)
	implements VariantSelectorProvider<SpawnContext, SpawnCondition> {

	public static final Codec<CowVariant> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			ModelAndTexture.createMapCodec(Model.CODEC, Model.NORMAL).forGetter(CowVariant::modelAndTexture),
			SpawnConditionSelectors.CODEC.fieldOf("spawn_conditions").forGetter(CowVariant::spawnConditions)
		).apply(instance, CowVariant::new)
	);

	public static final Codec<CowVariant> NETWORK_CODEC = RecordCodecBuilder.create(
		instance -> instance
			.group(ModelAndTexture.createMapCodec(Model.CODEC, Model.NORMAL).forGetter(CowVariant::modelAndTexture))
			.apply(instance, CowVariant::new)
	);

	public static final Codec<RegistryEntry<CowVariant>> ENTRY_CODEC = RegistryFixedCodec.of(RegistryKeys.COW_VARIANT);

	public static final PacketCodec<RegistryByteBuf, RegistryEntry<CowVariant>> ENTRY_PACKET_CODEC =
		PacketCodecs.registryEntry(RegistryKeys.COW_VARIANT);

	private CowVariant(ModelAndTexture<Model> modelAndTexture) {
		this(modelAndTexture, SpawnConditionSelectors.EMPTY);
	}

	@Override
	public List<VariantSelectorProvider.Selector<SpawnContext, SpawnCondition>> getSelectors() {
		return spawnConditions.selectors();
	}

	/** Модель коровы, соответствующая климатическому варианту. */
	public enum Model implements StringIdentifiable {
		NORMAL("normal"),
		COLD("cold"),
		WARM("warm");

		public static final Codec<Model> CODEC = StringIdentifiable.createCodec(Model::values);

		private final String id;

		Model(String id) {
			this.id = id;
		}

		@Override
		public String asString() {
			return id;
		}
	}
}
