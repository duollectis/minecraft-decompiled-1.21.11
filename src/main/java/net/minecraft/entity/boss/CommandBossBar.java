package net.minecraft.entity.boss;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.MathHelper;

public class CommandBossBar extends ServerBossBar {
   private static final int DEFAULT_MAX_VALUE = 100;
   private final Identifier id;
   private final Set<UUID> playerUuids = Sets.newHashSet();
   private int value;
   private int maxValue = 100;

   public CommandBossBar(Identifier id, Text displayName) {
      super(displayName, BossBar.Color.WHITE, BossBar.Style.PROGRESS);
      this.id = id;
      this.setPercent(0.0F);
   }

   public Identifier getId() {
      return this.id;
   }

   @Override
   public void addPlayer(ServerPlayerEntity player) {
      super.addPlayer(player);
      this.playerUuids.add(player.getUuid());
   }

   public void addPlayer(UUID uuid) {
      this.playerUuids.add(uuid);
   }

   @Override
   public void removePlayer(ServerPlayerEntity player) {
      super.removePlayer(player);
      this.playerUuids.remove(player.getUuid());
   }

   @Override
   public void clearPlayers() {
      super.clearPlayers();
      this.playerUuids.clear();
   }

   public int getValue() {
      return this.value;
   }

   public int getMaxValue() {
      return this.maxValue;
   }

   public void setValue(int value) {
      this.value = value;
      this.setPercent(MathHelper.clamp((float)value / this.maxValue, 0.0F, 1.0F));
   }

   public void setMaxValue(int maxValue) {
      this.maxValue = maxValue;
      this.setPercent(MathHelper.clamp((float)this.value / maxValue, 0.0F, 1.0F));
   }

   public final Text toHoverableText() {
      return Texts.bracketed(this.getName())
         .styled(
            style -> style.withColor(this.getColor().getTextFormat())
               .withHoverEvent(new HoverEvent.ShowText(Text.literal(this.getId().toString())))
               .withInsertion(this.getId().toString())
         );
   }

   public boolean addPlayers(Collection<ServerPlayerEntity> players) {
      Set<UUID> set = Sets.newHashSet();
      Set<ServerPlayerEntity> set2 = Sets.newHashSet();

      for (UUID uUID : this.playerUuids) {
         boolean bl = false;

         for (ServerPlayerEntity serverPlayerEntity : players) {
            if (serverPlayerEntity.getUuid().equals(uUID)) {
               bl = true;
               break;
            }
         }

         if (!bl) {
            set.add(uUID);
         }
      }

      for (ServerPlayerEntity serverPlayerEntity2 : players) {
         boolean bl = false;

         for (UUID uUID2 : this.playerUuids) {
            if (serverPlayerEntity2.getUuid().equals(uUID2)) {
               bl = true;
               break;
            }
         }

         if (!bl) {
            set2.add(serverPlayerEntity2);
         }
      }

      for (UUID uUID : set) {
         for (ServerPlayerEntity serverPlayerEntity3 : this.getPlayers()) {
            if (serverPlayerEntity3.getUuid().equals(uUID)) {
               this.removePlayer(serverPlayerEntity3);
               break;
            }
         }

         this.playerUuids.remove(uUID);
      }

      for (ServerPlayerEntity serverPlayerEntity2 : set2) {
         this.addPlayer(serverPlayerEntity2);
      }

      return !set.isEmpty() || !set2.isEmpty();
   }

   public static CommandBossBar fromSerialized(Identifier id, CommandBossBar.Serialized serialized) {
      CommandBossBar commandBossBar = new CommandBossBar(id, serialized.name);
      commandBossBar.setVisible(serialized.visible);
      commandBossBar.setValue(serialized.value);
      commandBossBar.setMaxValue(serialized.max);
      commandBossBar.setColor(serialized.color);
      commandBossBar.setStyle(serialized.overlay);
      commandBossBar.setDarkenSky(serialized.darkenScreen);
      commandBossBar.setDragonMusic(serialized.playBossMusic);
      commandBossBar.setThickenFog(serialized.createWorldFog);
      serialized.players.forEach(commandBossBar::addPlayer);
      return commandBossBar;
   }

   public CommandBossBar.Serialized toSerialized() {
      return new CommandBossBar.Serialized(
         this.getName(),
         this.isVisible(),
         this.getValue(),
         this.getMaxValue(),
         this.getColor(),
         this.getStyle(),
         this.shouldDarkenSky(),
         this.hasDragonMusic(),
         this.shouldThickenFog(),
         Set.copyOf(this.playerUuids)
      );
   }

   public void onPlayerConnect(ServerPlayerEntity player) {
      if (this.playerUuids.contains(player.getUuid())) {
         this.addPlayer(player);
      }
   }

   public void onPlayerDisconnect(ServerPlayerEntity player) {
      super.removePlayer(player);
   }

   public record Serialized(
      Text name,
      boolean visible,
      int value,
      int max,
      BossBar.Color color,
      BossBar.Style overlay,
      boolean darkenScreen,
      boolean playBossMusic,
      boolean createWorldFog,
      Set<UUID> players
   ) {
      public static final Codec<CommandBossBar.Serialized> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               TextCodecs.CODEC.fieldOf("Name").forGetter(CommandBossBar.Serialized::name),
               Codec.BOOL.optionalFieldOf("Visible", false).forGetter(CommandBossBar.Serialized::visible),
               Codec.INT.optionalFieldOf("Value", 0).forGetter(CommandBossBar.Serialized::value),
               Codec.INT.optionalFieldOf("Max", 100).forGetter(CommandBossBar.Serialized::max),
               BossBar.Color.CODEC.optionalFieldOf("Color", BossBar.Color.WHITE).forGetter(CommandBossBar.Serialized::color),
               BossBar.Style.CODEC.optionalFieldOf("Overlay", BossBar.Style.PROGRESS).forGetter(CommandBossBar.Serialized::overlay),
               Codec.BOOL.optionalFieldOf("DarkenScreen", false).forGetter(CommandBossBar.Serialized::darkenScreen),
               Codec.BOOL.optionalFieldOf("PlayBossMusic", false).forGetter(CommandBossBar.Serialized::playBossMusic),
               Codec.BOOL.optionalFieldOf("CreateWorldFog", false).forGetter(CommandBossBar.Serialized::createWorldFog),
               Uuids.SET_CODEC.optionalFieldOf("Players", Set.of()).forGetter(CommandBossBar.Serialized::players)
            )
            .apply(instance, CommandBossBar.Serialized::new)
      );
   }
}
