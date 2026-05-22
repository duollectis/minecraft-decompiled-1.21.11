package net.minecraft.data.report;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.ArgumentHelper;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Генерирует JSON-отчёт о синтаксисе всех серверных команд.
 * Создаёт полный граф дерева команд через {@link CommandManager} и сериализует его
 * в {@code reports/commands.json} с помощью {@link ArgumentHelper#toJson}.
 */
public class CommandSyntaxProvider implements DataProvider {

	private final DataOutput output;
	private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

	public CommandSyntaxProvider(DataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
		this.output = output;
		this.registriesFuture = registriesFuture;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		Path path = output.resolvePath(DataOutput.OutputType.REPORTS).resolve("commands.json");

		return registriesFuture.thenCompose(registries -> {
			CommandDispatcher<ServerCommandSource> dispatcher = new CommandManager(
				CommandManager.RegistrationEnvironment.ALL,
				CommandManager.createRegistryAccess(registries)
			).getDispatcher();

			return DataProvider.writeToPath(
				writer,
				ArgumentHelper.toJson(dispatcher, dispatcher.getRoot()),
				path
			);
		});
	}

	@Override
	public String getName() {
		return "Command Syntax";
	}
}
