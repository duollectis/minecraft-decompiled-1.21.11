package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.encryption.PublicPlayerSession;
import net.minecraft.network.message.MessageVerifier;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PlayerListEntry {
   private final GameProfile profile;
   private @Nullable Supplier<SkinTextures> texturesSupplier;
   private GameMode gameMode = GameMode.DEFAULT;
   private int latency;
   private @Nullable Text displayName;
   private boolean showHat = true;
   private @Nullable PublicPlayerSession session;
   private MessageVerifier messageVerifier;
   private int listOrder;

   public PlayerListEntry(GameProfile profile, boolean secureChatEnforced) {
      this.profile = profile;
      this.messageVerifier = getInitialVerifier(secureChatEnforced);
   }

   private static Supplier<SkinTextures> texturesSupplier(GameProfile profile) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      boolean bl = !minecraftClient.uuidEquals(profile.id());
      return minecraftClient.getSkinProvider().supplySkinTextures(profile, bl);
   }

   public GameProfile getProfile() {
      return this.profile;
   }

   public @Nullable PublicPlayerSession getSession() {
      return this.session;
   }

   public MessageVerifier getMessageVerifier() {
      return this.messageVerifier;
   }

   public boolean hasPublicKey() {
      return this.session != null;
   }

   protected void setSession(PublicPlayerSession session) {
      this.session = session;
      this.messageVerifier = session.createVerifier(PlayerPublicKey.EXPIRATION_GRACE_PERIOD);
   }

   protected void resetSession(boolean secureChatEnforced) {
      this.session = null;
      this.messageVerifier = getInitialVerifier(secureChatEnforced);
   }

   private static MessageVerifier getInitialVerifier(boolean secureChatEnforced) {
      return secureChatEnforced ? MessageVerifier.UNVERIFIED : MessageVerifier.NO_SIGNATURE;
   }

   public GameMode getGameMode() {
      return this.gameMode;
   }

   protected void setGameMode(GameMode gameMode) {
      this.gameMode = gameMode;
   }

   public int getLatency() {
      return this.latency;
   }

   protected void setLatency(int latency) {
      this.latency = latency;
   }

   public SkinTextures getSkinTextures() {
      if (this.texturesSupplier == null) {
         this.texturesSupplier = texturesSupplier(this.profile);
      }

      return this.texturesSupplier.get();
   }

   public @Nullable Team getScoreboardTeam() {
      return MinecraftClient.getInstance().world.getScoreboard().getScoreHolderTeam(this.getProfile().name());
   }

   public void setDisplayName(@Nullable Text displayName) {
      this.displayName = displayName;
   }

   public @Nullable Text getDisplayName() {
      return this.displayName;
   }

   public void setShowHat(boolean showHat) {
      this.showHat = showHat;
   }

   public boolean shouldShowHat() {
      return this.showHat;
   }

   public void setListOrder(int listOrder) {
      this.listOrder = listOrder;
   }

   public int getListOrder() {
      return this.listOrder;
   }
}
