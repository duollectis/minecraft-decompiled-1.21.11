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
	 * Компонент профиля игрока для предметов (голова игрока). Существует в двух вариантах:
	 * {@link Static} — с уже разрешённым {@link GameProfile}, и {@link Dynamic} — с именем или UUID,
	 * требующим асинхронного разрешения через {@link GameProfileResolver}.
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
		return profileOrData.map(
				profile -> new Static(Either.left(profile), override),
				data -> {
					boolean isDynamic = data.properties.isEmpty()
							&& data.id.isPresent() != data.name.isPresent();
					if (isDynamic) {
						return data.name
								.<ProfileComponent>map(name -> new Dynamic(Either.left(name), override))
								.orElseGet(() -> new Dynamic(Either.right(data.id.get()), override));
					}

					return new Static(Either.right(data), override);
				}
		);
	}

	/**
		 * Создаёт статический компонент профиля из уже разрешённого {@link GameProfile}.
		 *
		 * @param profile разрешённый профиль игрока
		 * @return {@link Static}-вариант компонента
		 */
	public static ProfileComponent ofStatic(GameProfile profile) {
		return new ProfileComponent.Static(Either.left(profile), SkinTextures.SkinOverride.EMPTY);
	}

	/**
		 * Создаёт динамический компонент профиля по имени игрока.
		 * Разрешение профиля происходит асинхронно через {@link GameProfileResolver}.
		 *
		 * @param name имя игрока
		 * @return {@link Dynamic}-вариант компонента
		 */
	public static ProfileComponent ofDynamic(String name) {
		return new ProfileComponent.Dynamic(Either.left(name), SkinTextures.SkinOverride.EMPTY);
	}

	/**
		 * Создаёт динамический компонент профиля по UUID игрока.
		 * Разрешение профиля происходит асинхронно через {@link GameProfileResolver}.
		 *
		 * @param id UUID игрока
		 * @return {@link Dynamic}-вариант компонента
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
		 * Асинхронно разрешает профиль игрока через {@link GameProfileResolver}.
		 * Для {@link Static} возвращает уже готовый профиль, для {@link Dynamic} — запрашивает его.
		 *
		 * @param resolver резолвер профилей
		 * @return {@link CompletableFuture} с разрешённым {@link GameProfile}
		 */
	public abstract CompletableFuture<GameProfile> resolve(GameProfileResolver resolver);

	public GameProfile getGameProfile() {
		return profile;
	}

	public SkinTextures.SkinOverride getOverride() {
		return override;
	}

	static GameProfile createGameProfile(Optional<String> name, Optional<UUID> id, PropertyMap properties) {
		String string = name.orElse("");
		UUID uUID = id.orElseGet(() -> name.map(Uuids::getOfflinePlayerUuid).orElse(Util.NIL_UUID));
		return new GameProfile(uUID, string, properties);
	}

	public abstract Optional<String> getName();

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
			return ProfileComponent.createGameProfile(name, id, properties);
		}
	}

	public static final class Dynamic extends ProfileComponent {

		private static final Text TEXT = Text.translatable("component.profile.dynamic").formatted(Formatting.GRAY);
		private final Either<String, UUID> nameOrId;

		Dynamic(Either<String, UUID> nameOrId, SkinTextures.SkinOverride override) {
			super(ProfileComponent.createGameProfile(nameOrId.left(), nameOrId.right(), PropertyMap.EMPTY), override);
			this.nameOrId = nameOrId;
		}

		@Override
		public Optional<String> getName() {
			return nameOrId.left();
		}

		@Override
		public boolean equals(Object o) {
			return this == o
					|| o instanceof Dynamic dynamic
					&& nameOrId.equals(dynamic.nameOrId)
					&& override.equals(dynamic.override);
		}

		@Override
		public int hashCode() {
			return 31 * (31 + nameOrId.hashCode()) + override.hashCode();
		}

		@Override
		protected Either<GameProfile, Data> get() {
			return Either.right(new Data(nameOrId.left(), nameOrId.right(), PropertyMap.EMPTY));
		}

		@Override
		public CompletableFuture<GameProfile> resolve(GameProfileResolver resolver) {
			return CompletableFuture.supplyAsync(
					() -> resolver.getProfile(nameOrId).orElse(profile),
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

	public static final class Static extends ProfileComponent {

		public static final ProfileComponent.Static EMPTY = new ProfileComponent.Static(
				Either.right(ProfileComponent.Data.EMPTY), SkinTextures.SkinOverride.EMPTY
		);
		private final Either<GameProfile, ProfileComponent.Data> profileOrData;

		Static(Either<GameProfile, Data> profileOrData, SkinTextures.SkinOverride override) {
			super(profileOrData.map(p -> p, Data::createGameProfile), override);
			this.profileOrData = profileOrData;
		}

		@Override
		public CompletableFuture<GameProfile> resolve(GameProfileResolver resolver) {
			return CompletableFuture.completedFuture(profile);
		}

		@Override
		protected Either<GameProfile, Data> get() {
			return profileOrData;
		}

		@Override
		public Optional<String> getName() {
			return profileOrData.map(p -> Optional.of(p.name()), data -> data.name);
		}

		@Override
		public boolean equals(Object o) {
			return this == o
					|| o instanceof Static other
					&& profileOrData.equals(other.profileOrData)
					&& override.equals(other.override);
		}

		@Override
		public int hashCode() {
			return 31 * (31 + profileOrData.hashCode()) + override.hashCode();
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
