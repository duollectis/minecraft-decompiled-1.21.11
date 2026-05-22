package net.minecraft.server.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ReturnValueConsumer;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.entity.Entity;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedCommandArguments;
import net.minecraft.registry.*;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.function.Tracer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.thread.FutureQueue;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Источник выполнения серверной команды: инкапсулирует контекст (позиция, мир, права,
 * сущность-исполнитель) и предоставляет методы отправки обратной связи игроку и операторам.
 * Все {@code with*}-методы возвращают новый иммутабельный экземпляр с изменённым полем.
 */
public class ServerCommandSource implements AbstractServerCommandSource<ServerCommandSource>, CommandSource {

	public static final SimpleCommandExceptionType
			REQUIRES_PLAYER_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("permissions.requires.player"));
	public static final SimpleCommandExceptionType
			REQUIRES_ENTITY_EXCEPTION =
			new SimpleCommandExceptionType(Text.translatable("permissions.requires.entity"));
	private final CommandOutput output;
	private final Vec3d position;
	private final ServerWorld world;
	private final PermissionPredicate permissions;
	private final String name;
	private final Text displayName;
	private final MinecraftServer server;
	private final boolean silent;
	private final @Nullable Entity entity;
	private final ReturnValueConsumer returnValueConsumer;
	private final EntityAnchorArgumentType.EntityAnchor entityAnchor;
	private final Vec2f rotation;
	private final SignedCommandArguments signedArguments;
	private final FutureQueue messageChainTaskQueue;

	public ServerCommandSource(
			CommandOutput output,
			Vec3d pos,
			Vec2f rot,
			ServerWorld world,
			PermissionPredicate permissions,
			String name,
			Text displayName,
			MinecraftServer server,
			@Nullable Entity entity
	) {
		this(
				output,
				pos,
				rot,
				world,
				permissions,
				name,
				displayName,
				server,
				entity,
				false,
				ReturnValueConsumer.EMPTY,
				EntityAnchorArgumentType.EntityAnchor.FEET,
				SignedCommandArguments.EMPTY,
				FutureQueue.immediate(server)
		);
	}

	private ServerCommandSource(
			CommandOutput output,
			Vec3d pos,
			Vec2f rot,
			ServerWorld world,
			PermissionPredicate permissions,
			String name,
			Text displayName,
			MinecraftServer server,
			@Nullable Entity entity,
			boolean silent,
			ReturnValueConsumer resultStorer,
			EntityAnchorArgumentType.EntityAnchor entityAnchor,
			SignedCommandArguments signedArguments,
			FutureQueue messageChainTaskQueue
	) {
		this.output = output;
		this.position = pos;
		this.world = world;
		this.silent = silent;
		this.entity = entity;
		this.permissions = permissions;
		this.name = name;
		this.displayName = displayName;
		this.server = server;
		this.returnValueConsumer = resultStorer;
		this.entityAnchor = entityAnchor;
		this.rotation = rot;
		this.signedArguments = signedArguments;
		this.messageChainTaskQueue = messageChainTaskQueue;
	}

	public ServerCommandSource withOutput(CommandOutput output) {
		return this.output == output
		       ? this
		       : new ServerCommandSource(
				       output,
				       this.position,
				       this.rotation,
				       this.world,
				       this.permissions,
				       this.name,
				       this.displayName,
				       this.server,
				       this.entity,
				       this.silent,
				       this.returnValueConsumer,
				       this.entityAnchor,
				       this.signedArguments,
				       this.messageChainTaskQueue
		       );
	}

	public ServerCommandSource withEntity(Entity entity) {
		return this.entity == entity
		       ? this
		       : new ServerCommandSource(
				       this.output,
				       this.position,
				       this.rotation,
				       this.world,
				       this.permissions,
				       entity.getStringifiedName(),
				       entity.getDisplayName(),
				       this.server,
				       entity,
				       this.silent,
				       this.returnValueConsumer,
				       this.entityAnchor,
				       this.signedArguments,
				       this.messageChainTaskQueue
		       );
	}

	public ServerCommandSource withPosition(Vec3d position) {
		return this.position.equals(position)
		       ? this
		       : new ServerCommandSource(
				       this.output,
				       position,
				       this.rotation,
				       this.world,
				       this.permissions,
				       this.name,
				       this.displayName,
				       this.server,
				       this.entity,
				       this.silent,
				       this.returnValueConsumer,
				       this.entityAnchor,
				       this.signedArguments,
				       this.messageChainTaskQueue
		       );
	}

	public ServerCommandSource withRotation(Vec2f rotation) {
		return this.rotation.equals(rotation)
		       ? this
		       : new ServerCommandSource(
				       this.output,
				       this.position,
				       rotation,
				       this.world,
				       this.permissions,
				       this.name,
				       this.displayName,
				       this.server,
				       this.entity,
				       this.silent,
				       this.returnValueConsumer,
				       this.entityAnchor,
				       this.signedArguments,
				       this.messageChainTaskQueue
		       );
	}

	public ServerCommandSource withReturnValueConsumer(ReturnValueConsumer returnValueConsumer) {
		return Objects.equals(this.returnValueConsumer, returnValueConsumer)
		       ? this
		       : new ServerCommandSource(
				       this.output,
				       this.position,
				       this.rotation,
				       this.world,
				       this.permissions,
				       this.name,
				       this.displayName,
				       this.server,
				       this.entity,
				       this.silent,
				       returnValueConsumer,
				       this.entityAnchor,
				       this.signedArguments,
				       this.messageChainTaskQueue
		       );
	}

	public ServerCommandSource mergeReturnValueConsumers(
			ReturnValueConsumer returnValueConsumer,
			BinaryOperator<ReturnValueConsumer> merger
	) {
		ReturnValueConsumer merged = merger.apply(this.returnValueConsumer, returnValueConsumer);
		return withReturnValueConsumer(merged);
	}

	public ServerCommandSource withSilent() {
		return !this.silent && !this.output.cannotBeSilenced()
		       ? new ServerCommandSource(
				this.output,
				this.position,
				this.rotation,
				this.world,
				this.permissions,
				this.name,
				this.displayName,
				this.server,
				this.entity,
				true,
				this.returnValueConsumer,
				this.entityAnchor,
				this.signedArguments,
				this.messageChainTaskQueue
		)
		       : this;
	}

	public ServerCommandSource withPermissions(PermissionPredicate permissions) {
		return permissions == this.permissions
		       ? this
		       : new ServerCommandSource(
				       this.output,
				       this.position,
				       this.rotation,
				       this.world,
				       permissions,
				       this.name,
				       this.displayName,
				       this.server,
				       this.entity,
				       this.silent,
				       this.returnValueConsumer,
				       this.entityAnchor,
				       this.signedArguments,
				       this.messageChainTaskQueue
		       );
	}

	public ServerCommandSource withAdditionalPermissions(PermissionPredicate permissions) {
		return this.withPermissions(this.permissions.or(permissions));
	}

	public ServerCommandSource withEntityAnchor(EntityAnchorArgumentType.EntityAnchor anchor) {
		return anchor == this.entityAnchor
		       ? this
		       : new ServerCommandSource(
				       this.output,
				       this.position,
				       this.rotation,
				       this.world,
				       this.permissions,
				       this.name,
				       this.displayName,
				       this.server,
				       this.entity,
				       this.silent,
				       this.returnValueConsumer,
				       anchor,
				       this.signedArguments,
				       this.messageChainTaskQueue
		       );
	}

	public ServerCommandSource withWorld(ServerWorld world) {
		if (world == this.world) {
			return this;
		}

		double scale = DimensionType.getCoordinateScaleFactor(this.world.getDimension(), world.getDimension());
		Vec3d scaledPos = new Vec3d(position.x * scale, position.y, position.z * scale);
		return new ServerCommandSource(
				output,
				scaledPos,
				rotation,
				world,
				permissions,
				name,
				displayName,
				server,
				entity,
				silent,
				returnValueConsumer,
				entityAnchor,
				signedArguments,
				messageChainTaskQueue
		);
	}

	public ServerCommandSource withLookingAt(Entity entity, EntityAnchorArgumentType.EntityAnchor anchor) {
		return withLookingAt(anchor.positionAt(entity));
	}

	/**
	 * Вычисляет углы поворота (pitch/yaw) для взгляда в заданную точку пространства
	 * относительно якоря сущности (ноги или глаза) и возвращает новый источник с этим поворотом.
	 */
	public ServerCommandSource withLookingAt(Vec3d position) {
		Vec3d anchorPos = entityAnchor.positionAt(this);
		double dx = position.x - anchorPos.x;
		double dy = position.y - anchorPos.y;
		double dz = position.z - anchorPos.z;
		double horizontalDist = Math.sqrt(dx * dx + dz * dz);
		float pitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(dy, horizontalDist) * 180.0F / (float) Math.PI)));
		float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F);
		return withRotation(new Vec2f(pitch, yaw));
	}

	public ServerCommandSource withSignedArguments(
			SignedCommandArguments signedArguments,
			FutureQueue messageChainTaskQueue
	) {
		return signedArguments == this.signedArguments && messageChainTaskQueue == this.messageChainTaskQueue
		       ? this
		       : new ServerCommandSource(
				       this.output,
				       this.position,
				       this.rotation,
				       this.world,
				       this.permissions,
				       this.name,
				       this.displayName,
				       this.server,
				       this.entity,
				       this.silent,
				       this.returnValueConsumer,
				       this.entityAnchor,
				       signedArguments,
				       messageChainTaskQueue
		       );
	}

	public Text getDisplayName() {
		return this.displayName;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public PermissionPredicate getPermissions() {
		return this.permissions;
	}

	public Vec3d getPosition() {
		return this.position;
	}

	public ServerWorld getWorld() {
		return this.world;
	}

	public @Nullable Entity getEntity() {
		return this.entity;
	}

	public Entity getEntityOrThrow() throws CommandSyntaxException {
		if (entity == null) {
			throw REQUIRES_ENTITY_EXCEPTION.create();
		}

		return entity;
	}

	public ServerPlayerEntity getPlayerOrThrow() throws CommandSyntaxException {
		if (entity instanceof ServerPlayerEntity player) {
			return player;
		}

		throw REQUIRES_PLAYER_EXCEPTION.create();
	}

	public @Nullable ServerPlayerEntity getPlayer() {
		return this.entity instanceof ServerPlayerEntity serverPlayerEntity ? serverPlayerEntity : null;
	}

	public boolean isExecutedByPlayer() {
		return this.entity instanceof ServerPlayerEntity;
	}

	public Vec2f getRotation() {
		return this.rotation;
	}

	public MinecraftServer getServer() {
		return this.server;
	}

	public EntityAnchorArgumentType.EntityAnchor getEntityAnchor() {
		return this.entityAnchor;
	}

	public SignedCommandArguments getSignedArguments() {
		return this.signedArguments;
	}

	public FutureQueue getMessageChainTaskQueue() {
		return this.messageChainTaskQueue;
	}

	public boolean shouldFilterText(ServerPlayerEntity recipient) {
		ServerPlayerEntity player = getPlayer();
		if (recipient == player) {
			return false;
		}

		return (player != null && player.shouldFilterText()) || recipient.shouldFilterText();
	}

	public void sendChatMessage(SentMessage message, boolean filterMaskEnabled, MessageType.Parameters params) {
		if (silent) {
			return;
		}

		ServerPlayerEntity player = getPlayer();
		if (player != null) {
			player.sendChatMessage(message, filterMaskEnabled, params);
		}
		else {
			output.sendMessage(params.applyChatDecoration(message.content()));
		}
	}

	public void sendMessage(Text message) {
		if (silent) {
			return;
		}

		ServerPlayerEntity player = getPlayer();
		if (player != null) {
			player.sendMessage(message);
		}
		else {
			output.sendMessage(message);
		}
	}

	public void sendFeedback(Supplier<Text> feedbackSupplier, boolean broadcastToOps) {
		boolean shouldSendToOutput = output.shouldReceiveFeedback() && !silent;
		boolean shouldBroadcast = broadcastToOps && output.shouldBroadcastConsoleToOps() && !silent;

		if (shouldSendToOutput || shouldBroadcast) {
			Text text = feedbackSupplier.get();

			if (shouldSendToOutput) {
				output.sendMessage(text);
			}

			if (shouldBroadcast) {
				sendToOps(text);
			}
		}
	}

	private void sendToOps(Text message) {
		Text adminText = Text.translatable("chat.type.admin", getDisplayName(), message)
		                     .formatted(Formatting.GRAY, Formatting.ITALIC);
		GameRules gameRules = world.getGameRules();

		if (gameRules.getValue(GameRules.SEND_COMMAND_FEEDBACK)) {
			for (ServerPlayerEntity op : server.getPlayerManager().getPlayerList()) {
				if (op.getCommandOutput() != output && server.getPlayerManager().isOperator(op.getPlayerConfigEntry())) {
					op.sendMessage(adminText);
				}
			}
		}

		if (output != server && gameRules.getValue(GameRules.LOG_ADMIN_COMMANDS)) {
			server.sendMessage(adminText);
		}
	}

	public void sendError(Text message) {
		if (output.shouldTrackOutput() && !silent) {
			output.sendMessage(Text.empty().append(message).formatted(Formatting.RED));
		}
	}

	@Override
	public ReturnValueConsumer getReturnValueConsumer() {
		return returnValueConsumer;
	}

	@Override
	public Collection<String> getPlayerNames() {
		return Lists.newArrayList(server.getPlayerNames());
	}

	@Override
	public Collection<String> getTeamNames() {
		return server.getScoreboard().getTeamNames();
	}

	@Override
	public Stream<Identifier> getSoundIds() {
		return Registries.SOUND_EVENT.stream().map(SoundEvent::id);
	}

	@Override
	public CompletableFuture<Suggestions> getCompletions(CommandContext<?> context) {
		return Suggestions.empty();
	}

	@Override
	public CompletableFuture<Suggestions> listIdSuggestions(
			RegistryKey<? extends Registry<?>> registryRef,
			CommandSource.SuggestedIdType suggestedIdType,
			SuggestionsBuilder builder,
			CommandContext<?> context
	) {
		if (registryRef == RegistryKeys.RECIPE) {
			return CommandSource.suggestIdentifiers(
					server.getRecipeManager().values().stream().map(recipe -> recipe.id().getValue()),
					builder
			);
		}

		if (registryRef == RegistryKeys.ADVANCEMENT) {
			Collection<AdvancementEntry> advancements = server.getAdvancementLoader().getAdvancements();
			return CommandSource.suggestIdentifiers(advancements.stream().map(AdvancementEntry::id), builder);
		}

		return getRegistry(registryRef).map(registry -> {
			suggestIdentifiers((RegistryWrapper<?>) registry, suggestedIdType, builder);
			return builder.buildFuture();
		}).orElseGet(Suggestions::empty);
	}

	private Optional<? extends RegistryWrapper<?>> getRegistry(RegistryKey<? extends Registry<?>> registryRef) {
		Optional<? extends Registry<?>> staticRegistry = getRegistryManager().getOptional(registryRef);
		return staticRegistry.isPresent()
				? staticRegistry
				: server.getReloadableRegistries().createRegistryLookup().getOptional(registryRef);
	}

	@Override
	public Set<RegistryKey<World>> getWorldKeys() {
		return server.getWorldRegistryKeys();
	}

	@Override
	public DynamicRegistryManager getRegistryManager() {
		return server.getRegistryManager();
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return world.getEnabledFeatures();
	}

	@Override
	public CommandDispatcher<ServerCommandSource> getDispatcher() {
		return getServer().getCommandFunctionManager().getDispatcher();
	}

	@Override
	public void handleException(CommandExceptionType type, Message message, boolean silent, @Nullable Tracer tracer) {
		if (tracer != null) {
			tracer.traceError(message.getString());
		}

		if (!silent) {
			sendError(Texts.toText(message));
		}
	}

	@Override
	public boolean isSilent() {
		return silent;
	}
}
