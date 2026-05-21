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
 * {@code ServerCommandSource}.
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

	/**
	 * With output.
	 *
	 * @param output output
	 *
	 * @return ServerCommandSource — результат операции
	 */
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

	/**
	 * With entity.
	 *
	 * @param entity entity
	 *
	 * @return ServerCommandSource — результат операции
	 */
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

	/**
	 * With position.
	 *
	 * @param position position
	 *
	 * @return ServerCommandSource — результат операции
	 */
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

	/**
	 * With rotation.
	 *
	 * @param rotation rotation
	 *
	 * @return ServerCommandSource — результат операции
	 */
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

	/**
	 * With return value consumer.
	 *
	 * @param returnValueConsumer return value consumer
	 *
	 * @return ServerCommandSource — результат операции
	 */
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
		ReturnValueConsumer returnValueConsumer2 = merger.apply(this.returnValueConsumer, returnValueConsumer);
		return this.withReturnValueConsumer(returnValueConsumer2);
	}

	/**
	 * With silent.
	 *
	 * @return ServerCommandSource — результат операции
	 */
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

	/**
	 * With permissions.
	 *
	 * @param permissions permissions
	 *
	 * @return ServerCommandSource — результат операции
	 */
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

	/**
	 * With additional permissions.
	 *
	 * @param permissions permissions
	 *
	 * @return ServerCommandSource — результат операции
	 */
	public ServerCommandSource withAdditionalPermissions(PermissionPredicate permissions) {
		return this.withPermissions(this.permissions.or(permissions));
	}

	/**
	 * With entity anchor.
	 *
	 * @param anchor anchor
	 *
	 * @return ServerCommandSource — результат операции
	 */
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

	/**
	 * With world.
	 *
	 * @param world world
	 *
	 * @return ServerCommandSource — результат операции
	 */
	public ServerCommandSource withWorld(ServerWorld world) {
		if (world == this.world) {
			return this;
		}
		else {
			double d = DimensionType.getCoordinateScaleFactor(this.world.getDimension(), world.getDimension());
			Vec3d vec3d = new Vec3d(this.position.x * d, this.position.y, this.position.z * d);
			return new ServerCommandSource(
					this.output,
					vec3d,
					this.rotation,
					world,
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
	}

	/**
	 * With looking at.
	 *
	 * @param entity entity
	 * @param anchor anchor
	 *
	 * @return ServerCommandSource — результат операции
	 */
	public ServerCommandSource withLookingAt(Entity entity, EntityAnchorArgumentType.EntityAnchor anchor) {
		return this.withLookingAt(anchor.positionAt(entity));
	}

	/**
	 * With looking at.
	 *
	 * @param position position
	 *
	 * @return ServerCommandSource — результат операции
	 */
	public ServerCommandSource withLookingAt(Vec3d position) {
		Vec3d vec3d = this.entityAnchor.positionAt(this);
		double d = position.x - vec3d.x;
		double e = position.y - vec3d.y;
		double f = position.z - vec3d.z;
		double g = Math.sqrt(d * d + f * f);
		float h = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 180.0F / (float) Math.PI)));
		float i = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 180.0F / (float) Math.PI) - 90.0F);
		return this.withRotation(new Vec2f(h, i));
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
		if (this.entity == null) {
			throw REQUIRES_ENTITY_EXCEPTION.create();
		}
		else {
			return this.entity;
		}
	}

	public ServerPlayerEntity getPlayerOrThrow() throws CommandSyntaxException {
		if (this.entity instanceof ServerPlayerEntity serverPlayerEntity) {
			return serverPlayerEntity;
		}
		else {
			throw REQUIRES_PLAYER_EXCEPTION.create();
		}
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

	/**
	 * Определяет, следует ли filter text.
	 *
	 * @param recipient recipient
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldFilterText(ServerPlayerEntity recipient) {
		ServerPlayerEntity serverPlayerEntity = this.getPlayer();
		return recipient == serverPlayerEntity ? false
		                                       : serverPlayerEntity != null && serverPlayerEntity.shouldFilterText()
		                                         || recipient.shouldFilterText();
	}

	/**
	 * Отправляет chat message.
	 *
	 * @param message message
	 * @param filterMaskEnabled filter mask enabled
	 * @param params params
	 */
	public void sendChatMessage(SentMessage message, boolean filterMaskEnabled, MessageType.Parameters params) {
		if (!this.silent) {
			ServerPlayerEntity serverPlayerEntity = this.getPlayer();
			if (serverPlayerEntity != null) {
				serverPlayerEntity.sendChatMessage(message, filterMaskEnabled, params);
			}
			else {
				this.output.sendMessage(params.applyChatDecoration(message.content()));
			}
		}
	}

	/**
	 * Отправляет message.
	 *
	 * @param message message
	 */
	public void sendMessage(Text message) {
		if (!this.silent) {
			ServerPlayerEntity serverPlayerEntity = this.getPlayer();
			if (serverPlayerEntity != null) {
				serverPlayerEntity.sendMessage(message);
			}
			else {
				this.output.sendMessage(message);
			}
		}
	}

	/**
	 * Отправляет feedback.
	 *
	 * @param feedbackSupplier feedback supplier
	 * @param broadcastToOps broadcast to ops
	 */
	public void sendFeedback(Supplier<Text> feedbackSupplier, boolean broadcastToOps) {
		boolean bl = this.output.shouldReceiveFeedback() && !this.silent;
		boolean bl2 = broadcastToOps && this.output.shouldBroadcastConsoleToOps() && !this.silent;
		if (bl || bl2) {
			Text text = feedbackSupplier.get();
			if (bl) {
				this.output.sendMessage(text);
			}

			if (bl2) {
				this.sendToOps(text);
			}
		}
	}

	private void sendToOps(Text message) {
		Text
				text =
				Text
						.translatable("chat.type.admin", this.getDisplayName(), message)
						.formatted(Formatting.GRAY, Formatting.ITALIC);
		GameRules gameRules = this.world.getGameRules();
		if (gameRules.getValue(GameRules.SEND_COMMAND_FEEDBACK)) {
			for (ServerPlayerEntity serverPlayerEntity : this.server.getPlayerManager().getPlayerList()) {
				if (serverPlayerEntity.getCommandOutput() != this.output && this.server
						.getPlayerManager()
						.isOperator(serverPlayerEntity.getPlayerConfigEntry())) {
					serverPlayerEntity.sendMessage(text);
				}
			}
		}

		if (this.output != this.server && gameRules.getValue(GameRules.LOG_ADMIN_COMMANDS)) {
			this.server.sendMessage(text);
		}
	}

	/**
	 * Отправляет error.
	 *
	 * @param message message
	 */
	public void sendError(Text message) {
		if (this.output.shouldTrackOutput() && !this.silent) {
			this.output.sendMessage(Text.empty().append(message).formatted(Formatting.RED));
		}
	}

	@Override
	public ReturnValueConsumer getReturnValueConsumer() {
		return this.returnValueConsumer;
	}

	@Override
	public Collection<String> getPlayerNames() {
		return Lists.newArrayList(this.server.getPlayerNames());
	}

	@Override
	public Collection<String> getTeamNames() {
		return this.server.getScoreboard().getTeamNames();
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
					this.server
							.getRecipeManager()
							.values()
							.stream()
							.map(recipe -> recipe.id().getValue()), builder
			);
		}
		else if (registryRef == RegistryKeys.ADVANCEMENT) {
			Collection<AdvancementEntry> collection = this.server.getAdvancementLoader().getAdvancements();
			return CommandSource.suggestIdentifiers(collection.stream().map(AdvancementEntry::id), builder);
		}
		else {
			return this.getRegistry(registryRef).map(registry -> {
				this.suggestIdentifiers((RegistryWrapper<?>) registry, suggestedIdType, builder);
				return builder.buildFuture();
			}).orElseGet(Suggestions::empty);
		}
	}

	private Optional<? extends RegistryWrapper<?>> getRegistry(RegistryKey<? extends Registry<?>> registryRef) {
		Optional<? extends Registry<?>> optional = this.getRegistryManager().getOptional(registryRef);
		return optional.isPresent() ? optional : this.server
		                                         .getReloadableRegistries()
		                                         .createRegistryLookup()
		                                         .getOptional(registryRef);
	}

	@Override
	public Set<RegistryKey<World>> getWorldKeys() {
		return this.server.getWorldRegistryKeys();
	}

	@Override
	public DynamicRegistryManager getRegistryManager() {
		return this.server.getRegistryManager();
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return this.world.getEnabledFeatures();
	}

	@Override
	public CommandDispatcher<ServerCommandSource> getDispatcher() {
		return this.getServer().getCommandFunctionManager().getDispatcher();
	}

	@Override
	public void handleException(CommandExceptionType type, Message message, boolean silent, @Nullable Tracer tracer) {
		if (tracer != null) {
			tracer.traceError(message.getString());
		}

		if (!silent) {
			this.sendError(Texts.toText(message));
		}
	}

	@Override
	public boolean isSilent() {
		return this.silent;
	}
}
