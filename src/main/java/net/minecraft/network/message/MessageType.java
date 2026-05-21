package net.minecraft.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Decoration;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Запись message type.
 */
public record MessageType(Decoration chat, Decoration narration) {

	public static final Codec<MessageType> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Decoration.CODEC.fieldOf("chat").forGetter(MessageType::chat),
					                    Decoration.CODEC.fieldOf("narration").forGetter(MessageType::narration)
			                    )
			                    .apply(instance, MessageType::new)
	);
	public static final PacketCodec<RegistryByteBuf, MessageType> PACKET_CODEC = PacketCodec.tuple(
			Decoration.PACKET_CODEC,
			MessageType::chat,
			Decoration.PACKET_CODEC,
			MessageType::narration,
			MessageType::new
	);
	public static final PacketCodec<RegistryByteBuf, RegistryEntry<MessageType>>
			ENTRY_PACKET_CODEC =
			PacketCodecs.registryEntry(
					RegistryKeys.MESSAGE_TYPE, PACKET_CODEC
			);
	public static final Decoration CHAT_TEXT_DECORATION = Decoration.ofChat("chat.type.text");
	public static final RegistryKey<MessageType> CHAT = register("chat");
	public static final RegistryKey<MessageType> SAY_COMMAND = register("say_command");
	public static final RegistryKey<MessageType> MSG_COMMAND_INCOMING = register("msg_command_incoming");
	public static final RegistryKey<MessageType> MSG_COMMAND_OUTGOING = register("msg_command_outgoing");
	public static final RegistryKey<MessageType> TEAM_MSG_COMMAND_INCOMING = register("team_msg_command_incoming");
	public static final RegistryKey<MessageType> TEAM_MSG_COMMAND_OUTGOING = register("team_msg_command_outgoing");
	public static final RegistryKey<MessageType> EMOTE_COMMAND = register("emote_command");

	private static RegistryKey<MessageType> register(String id) {
		return RegistryKey.of(RegistryKeys.MESSAGE_TYPE, Identifier.ofVanilla(id));
	}

	/**
	 * Bootstrap.
	 *
	 * @param messageTypeRegisterable message type registerable
	 */
	public static void bootstrap(Registerable<MessageType> messageTypeRegisterable) {
		messageTypeRegisterable.register(
				CHAT,
				new MessageType(CHAT_TEXT_DECORATION, Decoration.ofChat("chat.type.text.narrate"))
		);
		messageTypeRegisterable.register(
				SAY_COMMAND,
				new MessageType(
						Decoration.ofChat("chat.type.announcement"),
						Decoration.ofChat("chat.type.text.narrate")
				)
		);
		messageTypeRegisterable.register(
				MSG_COMMAND_INCOMING,
				new MessageType(
						Decoration.ofIncomingMessage("commands.message.display.incoming"),
						Decoration.ofChat("chat.type.text.narrate")
				)
		);
		messageTypeRegisterable.register(
				MSG_COMMAND_OUTGOING,
				new MessageType(
						Decoration.ofOutgoingMessage("commands.message.display.outgoing"),
						Decoration.ofChat("chat.type.text.narrate")
				)
		);
		messageTypeRegisterable.register(
				TEAM_MSG_COMMAND_INCOMING,
				new MessageType(
						Decoration.ofTeamMessage("chat.type.team.text"),
						Decoration.ofChat("chat.type.text.narrate")
				)
		);
		messageTypeRegisterable.register(
				TEAM_MSG_COMMAND_OUTGOING,
				new MessageType(
						Decoration.ofTeamMessage("chat.type.team.sent"),
						Decoration.ofChat("chat.type.text.narrate")
				)
		);
		messageTypeRegisterable.register(
				EMOTE_COMMAND,
				new MessageType(Decoration.ofChat("chat.type.emote"), Decoration.ofChat("chat.type.emote"))
		);
	}

	public static MessageType.Parameters params(RegistryKey<MessageType> typeKey, Entity entity) {
		return params(typeKey, entity.getEntityWorld().getRegistryManager(), entity.getDisplayName());
	}

	public static MessageType.Parameters params(RegistryKey<MessageType> typeKey, ServerCommandSource source) {
		return params(typeKey, source.getRegistryManager(), source.getDisplayName());
	}

	public static MessageType.Parameters params(
			RegistryKey<MessageType> typeKey,
			DynamicRegistryManager registryManager,
			Text name
	) {
		Registry<MessageType> registry = registryManager.getOrThrow(RegistryKeys.MESSAGE_TYPE);
		return new MessageType.Parameters(registry.getOrThrow(typeKey), name);
	}

	/**
	 * Запись parameters.
	 */
	public record Parameters(RegistryEntry<MessageType> type, Text name, Optional<Text> targetName) {

		public static final PacketCodec<RegistryByteBuf, MessageType.Parameters> CODEC = PacketCodec.tuple(
				MessageType.ENTRY_PACKET_CODEC,
				MessageType.Parameters::type,
				TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC,
				MessageType.Parameters::name,
				TextCodecs.OPTIONAL_UNLIMITED_REGISTRY_PACKET_CODEC,
				MessageType.Parameters::targetName,
				MessageType.Parameters::new
		);

		Parameters(RegistryEntry<MessageType> type, Text name) {
			this(type, name, Optional.empty());
		}

		/**
		 * Применяет chat decoration.
		 *
		 * @param content content
		 *
		 * @return Text — результат операции
		 */
		public Text applyChatDecoration(Text content) {
			return this.type.value().chat().apply(content, this);
		}

		/**
		 * Применяет narration decoration.
		 *
		 * @param content content
		 *
		 * @return Text — результат операции
		 */
		public Text applyNarrationDecoration(Text content) {
			return this.type.value().narration().apply(content, this);
		}

		public MessageType.Parameters withTargetName(Text targetName) {
			return new MessageType.Parameters(this.type, this.name, Optional.of(targetName));
		}
	}
}
