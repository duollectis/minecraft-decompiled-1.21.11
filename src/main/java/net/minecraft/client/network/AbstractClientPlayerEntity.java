package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;

/**
 * Базовый класс для всех клиентских игроков.
 * Связывает сущность игрока с записью в списке игроков, управляет скином,
 * состоянием движения и полем зрения.
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractClientPlayerEntity extends PlayerEntity implements ClientPlayerLikeEntity {

	private @Nullable PlayerListEntry playerListEntry;
	private final boolean deadmau5;
	private final ClientPlayerLikeState state = new ClientPlayerLikeState();

	/**
	 * @param world   клиентский мир
	 * @param profile профиль игрока
	 */
	public AbstractClientPlayerEntity(ClientWorld world, GameProfile profile) {
		super(world, profile);
		deadmau5 = "deadmau5".equals(getGameProfile().name());
	}

	@Override
	public @Nullable GameMode getGameMode() {
		PlayerListEntry entry = getPlayerListEntry();
		return entry != null ? entry.getGameMode() : null;
	}

	/**
	 * Возвращает запись игрока из списка игроков, кешируя результат.
	 *
	 * @return запись или {@code null} если игрок не найден
	 */
	protected @Nullable PlayerListEntry getPlayerListEntry() {
		if (playerListEntry == null) {
			playerListEntry = MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(getUuid());
		}

		return playerListEntry;
	}

	@Override
	public void tick() {
		state.tick(getEntityPos(), getVelocity());
		super.tick();
	}

	/**
	 * Добавляет пройденное расстояние к накопителю состояния движения.
	 *
	 * @param distanceMoved пройденное расстояние за тик
	 */
	protected void addDistanceMoved(float distanceMoved) {
		state.addDistanceMoved(distanceMoved);
	}

	@Override
	public ClientPlayerLikeState getState() {
		return state;
	}

	@Override
	public @Nullable Text getMannequinName() {
		Scoreboard scoreboard = getEntityWorld().getScoreboard();
		ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);

		if (objective == null) {
			return null;
		}

		ReadableScoreboardScore score = scoreboard.getScore(this, objective);
		Text scoreText = ReadableScoreboardScore.getFormattedScore(
				score,
				objective.getNumberFormatOr(StyledNumberFormat.EMPTY)
		);
		return Text.empty().append(scoreText).append(ScreenTexts.SPACE).append(objective.getDisplayName());
	}

	@Override
	public SkinTextures getSkin() {
		PlayerListEntry entry = getPlayerListEntry();
		return entry == null
		       ? DefaultSkinHelper.getSkinTextures(getUuid())
		       : entry.getSkinTextures();
	}

	@Override
	public ParrotEntity.@Nullable Variant getShoulderParrotVariant(boolean leftShoulder) {
		return leftShoulder
		       ? getLeftShoulderParrotVariant().orElse(null)
		       : getRightShoulderParrotVariant().orElse(null);
	}

	@Override
	public void tickRiding() {
		super.tickRiding();
		getState().tickRiding();
	}

	@Override
	public void tickMovement() {
		tickPlayerMovement();
		super.tickMovement();
	}

	/**
	 * Обновляет накопитель движения на основе горизонтальной скорости.
	 * Движение учитывается только на земле, когда игрок жив и не плывёт.
	 */
	protected void tickPlayerMovement() {
		boolean onGroundAlive = isOnGround() && isDead() == false && isSwimming() == false;
		float movement = onGroundAlive
		                 ? Math.min(0.1F, (float) getVelocity().horizontalLength())
		                 : 0.0F;
		getState().tickMovement(movement);
	}

	/**
	 * Вычисляет множитель поля зрения с учётом полёта, скорости и используемого предмета.
	 *
	 * @param firstPerson    {@code true} если вид от первого лица
	 * @param fovEffectScale масштаб эффекта FOV из настроек
	 * @return итоговый множитель FOV
	 */
	public float getFovMultiplier(boolean firstPerson, float fovEffectScale) {
		float multiplier = 1.0F;

		if (getAbilities().flying) {
			multiplier *= 1.1F;
		}

		float walkSpeed = getAbilities().getWalkSpeed();
		if (walkSpeed != 0.0F) {
			float speedRatio = (float) getAttributeValue(EntityAttributes.MOVEMENT_SPEED) / walkSpeed;
			multiplier *= (speedRatio + 1.0F) / 2.0F;
		}

		if (isUsingItem()) {
			if (getActiveItem().isOf(Items.BOW)) {
				float chargeProgress = Math.min(getItemUseTime() / 20.0F, 1.0F);
				multiplier *= 1.0F - MathHelper.square(chargeProgress) * 0.15F;
			}
			else if (firstPerson && isUsingSpyglass()) {
				return 0.1F;
			}
		}

		return MathHelper.lerp(fovEffectScale, 1.0F, multiplier);
	}

	@Override
	public boolean hasExtraEars() {
		return deadmau5;
	}
}
