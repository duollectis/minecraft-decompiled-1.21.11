package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.ChatSuggestionsS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Источник команд на стороне клиента.
 * Предоставляет подсказки для автодополнения команд, запрашивая их у сервера,
 * а также локальные подсказки (имена игроков, команды, позиции).
 */
@Environment(EnvType.CLIENT)
public class ClientCommandSource implements CommandSource {

	private static final int NO_PENDING_COMPLETION = -1;

	private final ClientPlayNetworkHandler networkHandler;
	private final MinecraftClient client;
	private int completionId = NO_PENDING_COMPLETION;
	private @Nullable CompletableFuture<Suggestions> pendingCommandCompletion;
	private final Set<String> chatSuggestions = new HashSet<>();
	private final PermissionPredicate permissionPredicate;

	/**
	 * @param networkHandler      сетевой обработчик для отправки пакетов
	 * @param client              клиент Minecraft
	 * @param permissionPredicate предикат проверки прав
	 */
	public ClientCommandSource(
			ClientPlayNetworkHandler networkHandler,
			MinecraftClient client,
			PermissionPredicate permissionPredicate
	) {
		this.networkHandler = networkHandler;
		this.client = client;
		this.permissionPredicate = permissionPredicate;
	}

	@Override
	public Collection<String> getPlayerNames() {
		List<String> names = Lists.newArrayList();

		for (PlayerListEntry entry : networkHandler.getPlayerList()) {
			names.add(entry.getProfile().name());
		}

		return names;
	}

	@Override
	public Collection<String> getChatSuggestions() {
		if (chatSuggestions.isEmpty()) {
			return getPlayerNames();
		}

		Set<String> combined = new HashSet<>(getPlayerNames());
		combined.addAll(chatSuggestions);
		return combined;
	}

	@Override
	public Collection<String> getEntitySuggestions() {
		if (client.crosshairTarget == null
				|| client.crosshairTarget.getType() != HitResult.Type.ENTITY
		) {
			return Collections.emptyList();
		}

		String uuid = ((EntityHitResult) client.crosshairTarget).getEntity().getUuidAsString();
		return Collections.singleton(uuid);
	}

	@Override
	public Collection<String> getTeamNames() {
		return networkHandler.getScoreboard().getTeamNames();
	}

	@Override
	public Stream<Identifier> getSoundIds() {
		return client.getSoundManager().getKeys().stream();
	}

	@Override
	public PermissionPredicate getPermissions() {
		return permissionPredicate;
	}

	@Override
	public CompletableFuture<Suggestions> listIdSuggestions(
			RegistryKey<? extends Registry<?>> registryRef,
			CommandSource.SuggestedIdType suggestedIdType,
			SuggestionsBuilder builder,
			CommandContext<?> context
	) {
		return getRegistryManager()
				.getOptional(registryRef)
				.map(registry -> {
					suggestIdentifiers(registry, suggestedIdType, builder);
					return builder.buildFuture();
				})
				.orElseGet(() -> getCompletions(context));
	}

	@Override
	public CompletableFuture<Suggestions> getCompletions(CommandContext<?> context) {
		if (pendingCommandCompletion != null) {
			pendingCommandCompletion.cancel(false);
		}

		pendingCommandCompletion = new CompletableFuture<>();
		int id = ++completionId;
		networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(id, context.getInput()));
		return pendingCommandCompletion;
	}

	@Override
	public Collection<CommandSource.RelativePosition> getBlockPositionSuggestions() {
		HitResult hit = client.crosshairTarget;

		if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
			return CommandSource.super.getBlockPositionSuggestions();
		}

		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		return Collections.singleton(new CommandSource.RelativePosition(
				formatInt(pos.getX()),
				formatInt(pos.getY()),
				formatInt(pos.getZ())
		));
	}

	@Override
	public Collection<CommandSource.RelativePosition> getPositionSuggestions() {
		HitResult hit = client.crosshairTarget;

		if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
			return CommandSource.super.getPositionSuggestions();
		}

		Vec3d pos = hit.getPos();
		return Collections.singleton(new CommandSource.RelativePosition(
				formatDouble(pos.x),
				formatDouble(pos.y),
				formatDouble(pos.z)
		));
	}

	@Override
	public Set<RegistryKey<World>> getWorldKeys() {
		return networkHandler.getWorldKeys();
	}

	@Override
	public DynamicRegistryManager getRegistryManager() {
		return networkHandler.getRegistryManager();
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return networkHandler.getEnabledFeatures();
	}

	/**
	 * Завершает ожидающий запрос автодополнения, если идентификатор совпадает.
	 *
	 * @param completionId идентификатор запроса
	 * @param suggestions  полученные подсказки
	 */
	public void onCommandSuggestions(int completionId, Suggestions suggestions) {
		if (completionId != this.completionId) {
			return;
		}

		pendingCommandCompletion.complete(suggestions);
		pendingCommandCompletion = null;
		this.completionId = NO_PENDING_COMPLETION;
	}

	/**
	 * Обновляет набор подсказок чата согласно действию из пакета сервера.
	 *
	 * @param action      тип действия (ADD, REMOVE, SET)
	 * @param suggestions список подсказок
	 */
	public void onChatSuggestions(ChatSuggestionsS2CPacket.Action action, List<String> suggestions) {
		switch (action) {
			case ADD -> chatSuggestions.addAll(suggestions);
			case REMOVE -> suggestions.forEach(chatSuggestions::remove);
			case SET -> {
				chatSuggestions.clear();
				chatSuggestions.addAll(suggestions);
			}
		}
	}

	private static String formatDouble(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static String formatInt(int value) {
		return Integer.toString(value);
	}
}
