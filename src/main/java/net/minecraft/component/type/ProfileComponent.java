package net.minecraft.component.type;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.GameProfileResolver;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * {@code ProfileComponent}.
 */
public abstract sealed class ProfileComponent implements TooltipAppender permits ProfileComponent.Static, ProfileComponent.Dynamic {

	private static final Codec<ProfileComponent> COMPONENT_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codec
							                    .mapEither(Codecs.INT_STREAM_UUID_GAME_PROFILE_CODEC, ProfileComponent.Data.CODEC)
							                    .forGetter(ProfileComponent::get),
					                    SkinTextures.SkinOverride.CODEC.forGetter(ProfileComponent::getOverride)
			                    )
			                    .apply(instance, ProfileComponent::ofDispatched)
	);
	public static final Codec<ProfileComponent>
			CODEC =
			Codec.withAlternative(COMPONENT_CODEC, Codecs.PLAYER_NAME, ProfileComponent::ofDynamic);
	public static final PacketCodec<ByteBuf, ProfileComponent> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.either(PacketCodecs.GAME_PROFILE, ProfileComponent.Data.PACKET_CODEC),
			ProfileComponent::get,
			SkinTextures.SkinOverride.PACKET_CODEC,
			ProfileComponent::getOverride,
			ProfileComponent::ofDispatched
	);
	protected final GameProfile profile;
	protected final SkinTextures.SkinOverride override;

	private static ProfileComponent ofDispatched(
			Either<GameProfile, ProfileComponent.Data> profileOrData,
			SkinTextures.SkinOverride override
	) {
		return (ProfileComponent) profileOrData.map(
				profile -> new ProfileComponent.Static(Either.left(profile), override),
				data -> (ProfileComponent) (data.properties.isEmpty() && data.id.isPresent() != data.name.isPresent()
				                            ? data.name
				                              .<ProfileComponent>map(name -> new ProfileComponent.Dynamic(
						                              Either.left(name), override))
				                              .orElseGet(() -> new ProfileComponent.Dynamic(
						                              Either.right(data.id.get()),
						                              override
				                              ))
				                            : new ProfileComponent.Static(Either.right(data), override)
				)
		);
	}

	/**
	 * Of static.
	 *
	 * @param profile profile
	 *
	 * @return ProfileComponent — результат операции
	 */
	public static ProfileComponent ofStatic(GameProfile profile) {
		return new ProfileComponent.Static(Either.left(profile), SkinTextures.SkinOverride.EMPTY);
	}

	/**
	 * Of dynamic.
	 *
	 * @param name name
	 *
	 * @return ProfileComponent — результат операции
	 */
	public static ProfileComponent ofDynamic(String name) {
		return new ProfileComponent.Dynamic(Either.left(name), SkinTextures.SkinOverride.EMPTY);
	}

	/**
	 * Of dynamic.
	 *
	 * @param id id
	 *
	 * @return ProfileComponent — результат операции
	 */
	public static ProfileComponent ofDynamic(UUID id) {
		return new ProfileComponent.Dynamic(Either.right(id), SkinTextures.SkinOverride.EMPTY);
	}

	protected abstract Either<GameProfile, ProfileComponent.Data> get();

	protected ProfileComponent(GameProfile profile, SkinTextures.SkinOverride override) {
		this.profile = profile;
		this.override = override;
	}

	/**
	 * Resolve.
	 *
	 * @param resolver resolver
	 *
	 * @return CompletableFuture — результат операции
	 */
	public abstract CompletableFuture<GameProfile> resolve(GameProfileResolver resolver);

	public GameProfile getGameProfile() {
		return this.profile;
	}

	public SkinTextures.SkinOverride getOverride() {
		return this.override;
	}

	static GameProfile createGameProfile(Optional<String> name, Optional<UUID> id, PropertyMap properties) {
		String string = name.orElse("");
		UUID uUID = id.orElseGet(() -> name.map(Uuids::getOfflinePlayerUuid).orElse(Util.NIL_UUID));
		return new GameProfile(uUID, string, properties);
	}

	public abstract Optional<String> getName();

	/**
	 * {@code Data}.
	 */
	protected record Data(Optional<String> name, Optional<UUID> id, PropertyMap properties) {

		public static final ProfileComponent.Data
				EMPTY =
				new ProfileComponent.Data(Optional.empty(), Optional.empty(), PropertyMap.EMPTY);
		static final MapCodec<ProfileComponent.Data> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    Codecs.PLAYER_NAME.optionalFieldOf("name").forGetter(ProfileComponent.Data::name),
						                    Uuids.INT_STREAM_CODEC.optionalFieldOf("id").forGetter(ProfileComponent.Data::id),
						                    Codecs.GAME_PROFILE_PROPERTY_MAP
								                    .optionalFieldOf("properties", PropertyMap.EMPTY)
								                    .forGetter(ProfileComponent.Data::properties)
				                    )
				                    .apply(instance, ProfileComponent.Data::new)
		);
		public static final PacketCodec<ByteBuf, ProfileComponent.Data> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.PLAYER_NAME.collect(PacketCodecs::optional),
				ProfileComponent.Data::name,
				Uuids.PACKET_CODEC.collect(PacketCodecs::optional),
				ProfileComponent.Data::id,
				PacketCodecs.PROPERTY_MAP,
				ProfileComponent.Data::properties,
				ProfileComponent.Data::new
		);

		private GameProfile createGameProfile() {
			return ProfileComponent.createGameProfile(this.name, this.id, this.properties);
		}
	}

	/**
	 * {@code Dynamic}.
	 */
	public static final class Dynamic extends ProfileComponent {

		private static final Text TEXT = Text.translatable("component.profile.dynamic").formatted(Formatting.GRAY);
		private final Either<String, UUID> nameOrId;

		Dynamic(Either<String, UUID> nameOrId, SkinTextures.SkinOverride override) {
			super(ProfileComponent.createGameProfile(nameOrId.left(), nameOrId.right(), PropertyMap.EMPTY), override);
			this.nameOrId = nameOrId;
		}

		@Override
		public Optional<String> getName() {
			return this.nameOrId.left();
		}

		@Override
		public boolean equals(Object o) {
			return this == o || o instanceof ProfileComponent.Dynamic dynamic && this.nameOrId.equals(dynamic.nameOrId)
					&& this.override.equals(dynamic.override);
		}

		@Override
		public int hashCode() {
			int i = 31 + this.nameOrId.hashCode();
			return 31 * i + this.override.hashCode();
		}

		@Override
		protected Either<GameProfile, ProfileComponent.Data> get() {
			return Either.right(new ProfileComponent.Data(
					this.nameOrId.left(),
					this.nameOrId.right(),
					PropertyMap.EMPTY
			));
		}

		@Override
		public CompletableFuture<GameProfile> resolve(GameProfileResolver resolver) {
			return CompletableFuture.supplyAsync(
					() -> resolver.getProfile(this.nameOrId).orElse(this.profile),
					Util.getDownloadWorkerExecutor()
			);
		}

		@Override
		public void appendTooltip(
				Item.TooltipContext context,
				Consumer<Text> textConsumer,
				TooltipType type,
				ComponentsAccess components
		) {
			textConsumer.accept(TEXT);
		}
	}

	/**
	 * {@code Static}.
	 */
	public static final class Static extends ProfileComponent {

		public static final ProfileComponent.Static EMPTY = new ProfileComponent.Static(
				Either.right(ProfileComponent.Data.EMPTY), SkinTextures.SkinOverride.EMPTY
		);
		private final Either<GameProfile, ProfileComponent.Data> profileOrData;

		Static(Either<GameProfile, ProfileComponent.Data> profileOrData, SkinTextures.SkinOverride override) {
			super(
					(GameProfile) profileOrData.map(profile -> profile, ProfileComponent.Data::createGameProfile),
					override
			);
			this.profileOrData = profileOrData;
		}

		@Override
		public CompletableFuture<GameProfile> resolve(GameProfileResolver resolver) {
			return CompletableFuture.completedFuture(this.profile);
		}

		@Override
		protected Either<GameProfile, ProfileComponent.Data> get() {
			return this.profileOrData;
		}

		@Override
		public Optional<String> getName() {
			return (Optional<String>) this.profileOrData.map(profile -> Optional.of(profile.name()), data -> data.name);
		}

		@Override
		public boolean equals(Object o) {
			return this == o
					|| o instanceof ProfileComponent.Static static_ && this.profileOrData.equals(static_.profileOrData)
					&& this.override.equals(static_.override);
		}

		@Override
		public int hashCode() {
			int i = 31 + this.profileOrData.hashCode();
			return 31 * i + this.override.hashCode();
		}

		@Override
		public void appendTooltip(
				Item.TooltipContext context,
				Consumer<Text> textConsumer,
				TooltipType type,
				ComponentsAccess components
		) {
		}
	}
}
