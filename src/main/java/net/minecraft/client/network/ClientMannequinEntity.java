package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.MannequinEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Клиентская реализация сущности-манекена с поддержкой скинов игроков.
 * <p>Асинхронно загружает скин через {@link PlayerSkinCache} при изменении профиля
 * манекена. До загрузки скина используется {@link #DEFAULT_TEXTURES}.
 */
@Environment(EnvType.CLIENT)
public class ClientMannequinEntity extends MannequinEntity implements ClientPlayerLikeEntity {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Текстуры по умолчанию для манекена без загруженного скина.
	 */
	public static final SkinTextures DEFAULT_TEXTURES = DefaultSkinHelper.getSkinTextures(
			MannequinEntity.DEFAULT_INFO.getGameProfile()
	);

	private final ClientPlayerLikeState state = new ClientPlayerLikeState();
	private final PlayerSkinCache skinCache;
	private @Nullable CompletableFuture<Optional<SkinTextures>> skinLookup;
	private SkinTextures skin = DEFAULT_TEXTURES;

	/**
	 * Устанавливает фабрику манекенов, создающую {@link ClientMannequinEntity} на клиенте.
	 *
	 * @param cache кэш скинов игроков
	 */
	public static void setFactory(PlayerSkinCache cache) {
		MannequinEntity.factory = (type, world) -> world instanceof ClientWorld
		                                           ? new ClientMannequinEntity(world, cache)
		                                           : new MannequinEntity(type, world);
	}

	/**
	 * Создаёт клиентского манекена.
	 *
	 * @param world     мир, в котором находится манекен
	 * @param skinCache кэш скинов для загрузки текстур
	 */
	public ClientMannequinEntity(World world, PlayerSkinCache skinCache) {
		super(world);
		this.skinCache = skinCache;
	}

	@Override
	public void tick() {
		super.tick();
		state.tick(getEntityPos(), getVelocity());

		if (skinLookup == null || skinLookup.isDone() == false) {
			return;
		}

		try {
			skinLookup.get().ifPresent(loaded -> skin = loaded);
			skinLookup = null;
		}
		catch (Exception e) {
			LOGGER.error("Error when trying to look up skin", e);
		}
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);

		if (data.equals(PROFILE)) {
			refreshSkin();
		}
	}

	@Override
	public ClientPlayerLikeState getState() {
		return state;
	}

	@Override
	public SkinTextures getSkin() {
		return skin;
	}

	@Override
	public @Nullable Text getMannequinName() {
		return getDescription();
	}

	@Override
	public ParrotEntity.@Nullable Variant getShoulderParrotVariant(boolean leftShoulder) {
		return null;
	}

	@Override
	public boolean hasExtraEars() {
		return false;
	}

	private void refreshSkin() {
		if (skinLookup != null) {
			CompletableFuture<Optional<SkinTextures>> pending = skinLookup;
			skinLookup = null;
			pending.cancel(false);
		}

		skinLookup = skinCache
				.getFuture(getMannequinProfile())
				.thenApply(entry -> entry.map(PlayerSkinCache.Entry::getTextures));
	}
}
