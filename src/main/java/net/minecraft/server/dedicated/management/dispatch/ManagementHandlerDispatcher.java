package net.minecraft.server.dedicated.management.dispatch;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.management.DedicatedServerSubmitter;
import net.minecraft.server.dedicated.management.ManagementLogger;
import net.minecraft.server.dedicated.management.Submitter;
import net.minecraft.server.dedicated.management.handler.*;
import net.minecraft.server.dedicated.management.listener.CompositeManagementListener;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Класс Management Handler Dispatcher.
 */
public class ManagementHandlerDispatcher {

	private final CompositeManagementListener listener;
	private final AllowlistManagementHandler allowlistHandler;
	private final BanManagementHandler banHandler;
	private final PlayerListManagementHandler playerListHandler;
	private final GameRuleManagementHandler gameRuleHandler;
	private final OperatorManagementHandler operatorHandler;
	private final PropertiesManagementHandler propertiesHandler;
	private final ServerManagementHandler serverHandler;
	private final Submitter submitter;

	public ManagementHandlerDispatcher(
			CompositeManagementListener listener,
			AllowlistManagementHandler allowlistHandler,
			BanManagementHandler banHandler,
			PlayerListManagementHandler playerListHandler,
			GameRuleManagementHandler gameRuleHandler,
			OperatorManagementHandler operatorHandler,
			PropertiesManagementHandler propertiesHandler,
			ServerManagementHandler serverHandler,
			Submitter submitter
	) {
		this.listener = listener;
		this.allowlistHandler = allowlistHandler;
		this.banHandler = banHandler;
		this.playerListHandler = playerListHandler;
		this.gameRuleHandler = gameRuleHandler;
		this.operatorHandler = operatorHandler;
		this.propertiesHandler = propertiesHandler;
		this.serverHandler = serverHandler;
		this.submitter = submitter;
	}

	/**
	 * Submit.
	 *
	 * @param task task
	 *
	 * @return CompletableFuture — результат операции
	 */
	public <V> CompletableFuture<V> submit(Supplier<V> task) {
		return this.submitter.submit(task);
	}

	/**
	 * Submit.
	 *
	 * @param task task
	 *
	 * @return CompletableFuture — результат операции
	 */
	public CompletableFuture<Void> submit(Runnable task) {
		return this.submitter.submit(task);
	}

	public AllowlistManagementHandler getAllowlistHandler() {
		return this.allowlistHandler;
	}

	public BanManagementHandler getBanHandler() {
		return this.banHandler;
	}

	public PlayerListManagementHandler getPlayerListHandler() {
		return this.playerListHandler;
	}

	public GameRuleManagementHandler getGameRuleHandler() {
		return this.gameRuleHandler;
	}

	public OperatorManagementHandler getOperatorHandler() {
		return this.operatorHandler;
	}

	public PropertiesManagementHandler getPropertiesHandler() {
		return this.propertiesHandler;
	}

	public ServerManagementHandler getServerHandler() {
		return this.serverHandler;
	}

	public CompositeManagementListener getListener() {
		return this.listener;
	}

	/**
	 * Create.
	 *
	 * @param server server
	 *
	 * @return ManagementHandlerDispatcher — результат операции
	 */
	public static ManagementHandlerDispatcher create(MinecraftDedicatedServer server) {
		ManagementLogger managementLogger = new ManagementLogger();
		AllowlistManagementHandlerImpl
				allowlistManagementHandlerImpl =
				new AllowlistManagementHandlerImpl(server, managementLogger);
		BanManagementHandlerImpl banManagementHandlerImpl = new BanManagementHandlerImpl(server, managementLogger);
		PlayerListManagementHandlerImpl
				playerListManagementHandlerImpl =
				new PlayerListManagementHandlerImpl(server, managementLogger);
		GameRuleManagementHandlerImpl
				gameRuleManagementHandlerImpl =
				new GameRuleManagementHandlerImpl(server, managementLogger);
		OperatorManagementHandlerImpl
				operatorManagementHandlerImpl =
				new OperatorManagementHandlerImpl(server, managementLogger);
		PropertiesManagementHandlerImpl
				propertiesManagementHandlerImpl =
				new PropertiesManagementHandlerImpl(server, managementLogger);
		ServerManagementHandlerImpl
				serverManagementHandlerImpl =
				new ServerManagementHandlerImpl(server, managementLogger);
		Submitter submitter = new DedicatedServerSubmitter(server);
		return new ManagementHandlerDispatcher(
				server.getManagementListener(),
				allowlistManagementHandlerImpl,
				banManagementHandlerImpl,
				playerListManagementHandlerImpl,
				gameRuleManagementHandlerImpl,
				operatorManagementHandlerImpl,
				propertiesManagementHandlerImpl,
				serverManagementHandlerImpl,
				submitter
		);
	}
}
