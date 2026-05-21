package net.minecraft.server.dedicated.management.schema;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.dedicated.management.RpcDiscover;
import net.minecraft.server.dedicated.management.RpcKickReason;
import net.minecraft.server.dedicated.management.RpcPlayer;
import net.minecraft.server.dedicated.management.UriUtil;
import net.minecraft.server.dedicated.management.dispatch.*;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Uuids;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.rule.GameRuleType;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code RpcSchema}.
 */
public record RpcSchema<T>(
		Optional<URI> reference,
		List<String> type,
		Optional<RpcSchema<?>> items,
		Map<String, RpcSchema<?>> properties,
		List<String> enumValues,
		Codec<T> codec
) {

	@SuppressWarnings("unchecked")
	public static final Codec<RpcSchema<?>> CODEC = (Codec<RpcSchema<?>>) (Codec<?>) Codec.<RpcSchema<?>>recursive(
			                                                                                      "Schema",
			                                                                                      codec -> RecordCodecBuilder.<RpcSchema<?>>create(
					                                                                                      instance -> instance.group(
							                                                                                                          UriUtil.URI_CODEC.optionalFieldOf("$ref").forGetter(schema -> schema.reference),
							                                                                                                          Codecs
									                                                                                                          .listOrSingle(Codec.STRING)
									                                                                                                          .optionalFieldOf("type", List.of())
									                                                                                                          .forGetter(schema -> schema.type),
							                                                                                                          codec.optionalFieldOf("items").forGetter(schema -> (Optional) schema.items),
							                                                                                                          Codec
									                                                                                                          .unboundedMap(Codec.STRING, codec)
									                                                                                                          .optionalFieldOf("properties", Map.of())
									                                                                                                          .forGetter(schema -> (Map) schema.properties),
							                                                                                                          Codec.STRING
									                                                                                                          .listOf()
									                                                                                                          .optionalFieldOf("enum", List.of())
									                                                                                                          .forGetter(schema -> schema.enumValues)
					                                                                                                          )
					                                                                                                          .apply(instance, (ref, type, items, properties, enumValues) -> null)
			                                                                                      )
	                                                                                      )
	                                                                                      .validate(schema ->
			                                                                                      schema == null
			                                                                                      ? DataResult.error(() -> "Should not deserialize schema")
			                                                                                      : DataResult.success(
					                                                                                      schema)
	                                                                                      );
	private static final List<RpcSchemaEntry<?>> REGISTERED_SCHEMAS = new ArrayList<>();
	public static final RpcSchema<Boolean> BOOLEAN = ofLiteral("boolean", Codec.BOOL);
	public static final RpcSchema<Integer> INTEGER = ofLiteral("integer", Codec.INT);
	public static final RpcSchema<Either<Boolean, Integer>>
			GAME_RULE_VALUE =
			ofLiterals(List.of("boolean", "integer"), Codec.either(Codec.BOOL, Codec.INT));
	public static final RpcSchema<Float> NUMBER = ofLiteral("number", Codec.FLOAT);
	public static final RpcSchema<String> STRING = ofLiteral("string", Codec.STRING);
	public static final RpcSchema<UUID> PLAYER_ID = ofLiteral("string", Uuids.INT_STREAM_CODEC);
	public static final RpcSchema<RpcDiscover.Document>
			DOCUMENT =
			ofLiteral("string", RpcDiscover.Document.CODEC.codec());
	public static final RpcSchemaEntry<Difficulty>
			DIFFICULTY =
			registerEntry("difficulty", ofEnum(Difficulty::values, Difficulty.CODEC));
	public static final RpcSchemaEntry<GameMode>
			GAME_MODE =
			registerEntry("game_type", ofEnum(GameMode::values, GameMode.CODEC));
	public static final RpcSchema<PermissionLevel>
			PERMISSION_LEVEL =
			ofLiteral("integer", PermissionLevel.NUMERIC_CODEC);
	public static final RpcSchemaEntry<RpcPlayer> PLAYER = registerEntry(
			"player", ofObject(RpcPlayer.CODEC.codec()).withProperty("id", PLAYER_ID).withProperty("name", STRING)
	);
	public static final RpcSchemaEntry<RpcDiscover.Info> VERSION = registerEntry(
			"version",
			ofObject(RpcDiscover.Info.CODEC.codec()).withProperty("name", STRING).withProperty("protocol", INTEGER)
	);
	public static final RpcSchemaEntry<ServerRpcDispatcher.RpcStatus> SERVER_STATE = registerEntry(
			"server_state",
			ofObject(ServerRpcDispatcher.RpcStatus.CODEC)
					.withProperty("started", BOOLEAN)
					.withProperty("players", PLAYER.ref().asArray())
					.withProperty("version", VERSION.ref())
	);
	public static final RpcSchema<GameRuleType> GAME_RULE_TYPE = ofEnum(GameRuleType::values);
	public static final RpcSchemaEntry<GameRuleRpcDispatcher.RuleEntry<?>> TYPED_GAME_RULE = registerEntry(
			"typed_game_rule",
			ofObject(GameRuleRpcDispatcher.RuleEntry.TYPED_CODEC)
					.withProperty("key", STRING)
					.withProperty("value", GAME_RULE_VALUE)
					.withProperty("type", GAME_RULE_TYPE)
	);
	public static final RpcSchemaEntry<GameRuleRpcDispatcher.RuleEntry<?>> UNTYPED_GAME_RULE = registerEntry(
			"untyped_game_rule",
			ofObject(GameRuleRpcDispatcher.RuleEntry.UNTYPED_CODEC)
					.withProperty("key", STRING)
					.withProperty("value", GAME_RULE_VALUE)
	);
	public static final RpcSchemaEntry<RpcKickReason> MESSAGE = registerEntry(
			"message",
			ofObject(RpcKickReason.CODEC)
					.withProperty("literal", STRING)
					.withProperty("translatable", STRING)
					.withProperty("translatableParams", STRING.asArray())
	);
	public static final RpcSchemaEntry<ServerRpcDispatcher.RpcSystemMessage> SYSTEM_MESSAGE = registerEntry(
			"system_message",
			ofObject(ServerRpcDispatcher.RpcSystemMessage.CODEC)
					.withProperty("message", MESSAGE.ref())
					.withProperty("overlay", BOOLEAN)
					.withProperty("receivingPlayers", PLAYER.ref().asArray())
	);
	public static final RpcSchemaEntry<PlayersRpcDispatcher.RpcEntry> KICK_PLAYER = registerEntry(
			"kick_player",
			ofObject(PlayersRpcDispatcher.RpcEntry.CODEC.codec())
					.withProperty("message", MESSAGE.ref())
					.withProperty("player", PLAYER.ref())
	);
	public static final RpcSchemaEntry<OperatorsRpcDispatcher.RpcEntry> OPERATOR = registerEntry(
			"operator",
			ofObject(OperatorsRpcDispatcher.RpcEntry.CODEC.codec())
					.withProperty("player", PLAYER.ref())
					.withProperty("bypassesPlayerLimit", BOOLEAN)
					.withProperty("permissionLevel", INTEGER)
	);
	public static final RpcSchemaEntry<IpBansRpcDispatcher.IncomingRpcIpBanData> INCOMING_IP_BAN = registerEntry(
			"incoming_ip_ban",
			ofObject(IpBansRpcDispatcher.IncomingRpcIpBanData.CODEC.codec())
					.withProperty("player", PLAYER.ref())
					.withProperty("ip", STRING)
					.withProperty("reason", STRING)
					.withProperty("source", STRING)
					.withProperty("expires", STRING)
	);
	public static final RpcSchemaEntry<IpBansRpcDispatcher.IpBanData> IP_BAN = registerEntry(
			"ip_ban",
			ofObject(IpBansRpcDispatcher.IpBanData.CODEC.codec())
					.withProperty("ip", STRING)
					.withProperty("reason", STRING)
					.withProperty("source", STRING)
					.withProperty("expires", STRING)
	);
	public static final RpcSchemaEntry<PlayerBansRpcDispatcher.RpcEntry> USER_BAN = registerEntry(
			"user_ban",
			ofObject(PlayerBansRpcDispatcher.RpcEntry.CODEC.codec())
					.withProperty("player", PLAYER.ref())
					.withProperty("reason", STRING)
					.withProperty("source", STRING)
					.withProperty("expires", STRING)
	);

	@SuppressWarnings("unchecked")
	public static <T> Codec<RpcSchema<T>> getCodec() {
		return (Codec<RpcSchema<T>>) (Codec<?>) CODEC;
	}

	/**
	 * Copy.
	 *
	 * @return RpcSchema — результат операции
	 */
	public RpcSchema<T> copy() {
		return new RpcSchema<>(
				this.reference,
				this.type,
				this.items.map(RpcSchema::copy),
				this.properties
						.entrySet()
						.stream()
						.collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().copy())),
				this.enumValues,
				this.codec
		);
	}

	private static <T> RpcSchemaEntry<T> registerEntry(String reference, RpcSchema<T> schema) {
		RpcSchemaEntry<T> rpcSchemaEntry = new RpcSchemaEntry<>(reference, UriUtil.createSchemasUri(reference), schema);
		REGISTERED_SCHEMAS.add(rpcSchemaEntry);
		return rpcSchemaEntry;
	}

	public static List<RpcSchemaEntry<?>> getRegisteredSchemas() {
		return REGISTERED_SCHEMAS;
	}

	/**
	 * Of reference.
	 *
	 * @param reference reference
	 * @param codec codec
	 *
	 * @return RpcSchema — результат операции
	 */
	public static <T> RpcSchema<T> ofReference(URI reference, Codec<T> codec) {
		return new RpcSchema<>(Optional.of(reference), List.of(), Optional.empty(), Map.of(), List.of(), codec);
	}

	/**
	 * Of literal.
	 *
	 * @param literal literal
	 * @param codec codec
	 *
	 * @return RpcSchema — результат операции
	 */
	public static <T> RpcSchema<T> ofLiteral(String literal, Codec<T> codec) {
		return ofLiterals(List.of(literal), codec);
	}

	/**
	 * Of literals.
	 *
	 * @param literals literals
	 * @param codec codec
	 *
	 * @return RpcSchema — результат операции
	 */
	public static <T> RpcSchema<T> ofLiterals(List<String> literals, Codec<T> codec) {
		return new RpcSchema<>(Optional.empty(), literals, Optional.empty(), Map.of(), List.of(), codec);
	}

	public static <E extends Enum<E> & StringIdentifiable> RpcSchema<E> ofEnum(Supplier<E[]> enumValues) {
		return ofEnum(enumValues, StringIdentifiable.createCodec(enumValues));
	}

	public static <E extends Enum<E> & StringIdentifiable> RpcSchema<E> ofEnum(Supplier<E[]> values, Codec<E> codec) {
		List<String>
				list =
				Stream.<Enum>of((Enum[]) values.get()).map(value -> ((StringIdentifiable) value).asString()).toList();
		return ofList(list, codec);
	}

	/**
	 * Of list.
	 *
	 * @param values values
	 * @param codec codec
	 *
	 * @return RpcSchema — результат операции
	 */
	public static <T> RpcSchema<T> ofList(List<String> values, Codec<T> codec) {
		return new RpcSchema<>(Optional.empty(), List.of("string"), Optional.empty(), Map.of(), values, codec);
	}

	/**
	 * Of array.
	 *
	 * @param itemSchema item schema
	 * @param codec codec
	 *
	 * @return RpcSchema> — результат операции
	 */
	public static <T> RpcSchema<List<T>> ofArray(RpcSchema<?> itemSchema, Codec<T> codec) {
		return new RpcSchema<>(
				Optional.empty(),
				List.of("array"),
				Optional.of(itemSchema),
				Map.of(),
				List.of(),
				codec.listOf()
		);
	}

	/**
	 * Of object.
	 *
	 * @param codec codec
	 *
	 * @return RpcSchema — результат операции
	 */
	public static <T> RpcSchema<T> ofObject(Codec<T> codec) {
		return new RpcSchema<>(Optional.empty(), List.of("object"), Optional.empty(), Map.of(), List.of(), codec);
	}

	private static <T> RpcSchema<T> ofObjectWithProperties(Map<String, RpcSchema<?>> itemSchemaMap, Codec<T> codec) {
		return new RpcSchema<>(Optional.empty(), List.of("object"), Optional.empty(), itemSchemaMap, List.of(), codec);
	}

	/**
	 * With property.
	 *
	 * @param reference reference
	 * @param schema schema
	 *
	 * @return RpcSchema — результат операции
	 */
	public RpcSchema<T> withProperty(String reference, RpcSchema<?> schema) {
		HashMap<String, RpcSchema<?>> hashMap = new HashMap<>(this.properties);
		hashMap.put(reference, schema);
		return ofObjectWithProperties(hashMap, this.codec);
	}

	/**
	 * As array.
	 *
	 * @return RpcSchema> — результат операции
	 */
	public RpcSchema<List<T>> asArray() {
		return ofArray(this, this.codec);
	}
}
