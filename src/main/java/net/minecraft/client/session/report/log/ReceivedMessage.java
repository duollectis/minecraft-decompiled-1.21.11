package net.minecraft.client.session.report.log;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.message.MessageTrustStatus;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
public interface ReceivedMessage extends ChatLogEntry {
   static ReceivedMessage.ChatMessage of(GameProfile gameProfile, SignedMessage message, MessageTrustStatus trustStatus) {
      return new ReceivedMessage.ChatMessage(gameProfile, message, trustStatus);
   }

   static ReceivedMessage.GameMessage of(Text message, Instant timestamp) {
      return new ReceivedMessage.GameMessage(message, timestamp);
   }

   Text getContent();

   default Text getNarration() {
      return this.getContent();
   }

   boolean isSentFrom(UUID uuid);

   @Environment(EnvType.CLIENT)
   public record ChatMessage(GameProfile profile, SignedMessage message, MessageTrustStatus trustStatus) implements ReceivedMessage {
      public static final MapCodec<ReceivedMessage.ChatMessage> CHAT_MESSAGE_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Codecs.GAME_PROFILE_CODEC.fieldOf("profile").forGetter(ReceivedMessage.ChatMessage::profile),
               SignedMessage.CODEC.forGetter(ReceivedMessage.ChatMessage::message),
               MessageTrustStatus.CODEC.optionalFieldOf("trust_level", MessageTrustStatus.SECURE).forGetter(ReceivedMessage.ChatMessage::trustStatus)
            )
            .apply(instance, ReceivedMessage.ChatMessage::new)
      );
      private static final DateTimeFormatter DATE_TIME_FORMATTER = Util.getDefaultLocaleFormatter(FormatStyle.SHORT);

      @Override
      public Text getContent() {
         if (!this.message.filterMask().isPassThrough()) {
            Text text = this.message.filterMask().getFilteredText(this.message.getSignedContent());
            return (Text)(text != null ? text : Text.empty());
         } else {
            return this.message.getContent();
         }
      }

      @Override
      public Text getNarration() {
         Text text = this.getContent();
         Text text2 = this.getFormattedTimestamp();
         return Text.translatable("gui.chatSelection.message.narrate", this.profile.name(), text, text2);
      }

      public Text getHeadingText() {
         Text text = this.getFormattedTimestamp();
         return Text.translatable("gui.chatSelection.heading", this.profile.name(), text);
      }

      private Text getFormattedTimestamp() {
         ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(this.message.getTimestamp(), ZoneId.systemDefault());
         return Text.literal(zonedDateTime.format(DATE_TIME_FORMATTER)).formatted(Formatting.ITALIC, Formatting.GRAY);
      }

      @Override
      public boolean isSentFrom(UUID uuid) {
         return this.message.canVerifyFrom(uuid);
      }

      public UUID getSenderUuid() {
         return this.profile.id();
      }

      @Override
      public ChatLogEntry.Type getType() {
         return ChatLogEntry.Type.PLAYER;
      }
   }

   @Environment(EnvType.CLIENT)
   public record GameMessage(Text message, Instant timestamp) implements ReceivedMessage {
      public static final MapCodec<ReceivedMessage.GameMessage> GAME_MESSAGE_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               TextCodecs.CODEC.fieldOf("message").forGetter(ReceivedMessage.GameMessage::message),
               Codecs.INSTANT.fieldOf("time_stamp").forGetter(ReceivedMessage.GameMessage::timestamp)
            )
            .apply(instance, ReceivedMessage.GameMessage::new)
      );

      @Override
      public Text getContent() {
         return this.message;
      }

      @Override
      public boolean isSentFrom(UUID uuid) {
         return false;
      }

      @Override
      public ChatLogEntry.Type getType() {
         return ChatLogEntry.Type.SYSTEM;
      }
   }
}
