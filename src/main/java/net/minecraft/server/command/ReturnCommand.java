package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.ContextChain;
import net.minecraft.command.*;

import java.util.List;

/**
 * {@code ReturnCommand}.
 */
public class ReturnCommand {

	/**
	 * Register.
	 *
	 * @param dispatcher dispatcher
	 *
	 * @return > void — результат операции
	 */
	public static <T extends AbstractServerCommandSource<T>> void register(CommandDispatcher<T> dispatcher) {
		dispatcher.register(
				LiteralArgumentBuilder.<T>literal("return")
				                      .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
				                      .then(RequiredArgumentBuilder
						                      .<T, Integer>argument("value", IntegerArgumentType.integer())
						                      .executes(new ReturnCommand.ValueCommand<>()))
				                      .then(LiteralArgumentBuilder
						                      .<T>literal("fail")
						                      .executes(new ReturnCommand.FailCommand<>()))
				                      .then(LiteralArgumentBuilder
						                      .<T>literal("run")
						                      .forward(
								                      dispatcher.getRoot(),
								                      new ReturnCommand.ReturnRunRedirector<>(),
								                      false
						                      ))
		);
	}

	/**
	 * {@code FailCommand}.
	 */
	static class FailCommand<T extends AbstractServerCommandSource<T>> implements ControlFlowAware.Command<T> {

		public void execute(
				T abstractServerCommandSource,
				ContextChain<T> contextChain,
				ExecutionFlags executionFlags,
				ExecutionControl<T> executionControl
		) {
			abstractServerCommandSource.getReturnValueConsumer().onFailure();
			Frame frame = executionControl.getFrame();
			frame.fail();
			frame.doReturn();
		}
	}

	/**
	 * {@code ReturnRunRedirector}.
	 */
	static class ReturnRunRedirector<T extends AbstractServerCommandSource<T>> implements Forkable.RedirectModifier<T> {

		public void execute(
				T abstractServerCommandSource,
				List<T> list,
				ContextChain<T> contextChain,
				ExecutionFlags executionFlags,
				ExecutionControl<T> executionControl
		) {
			if (list.isEmpty()) {
				if (executionFlags.isInsideReturnRun()) {
					executionControl.enqueueAction(FallthroughCommandAction.getInstance());
				}
			}
			else {
				executionControl.getFrame().doReturn();
				ContextChain<T> contextChain2 = contextChain.nextStage();
				String string = contextChain2.getTopContext().getInput();
				executionControl.enqueueAction(
						new SingleCommandAction.MultiSource<>(
								string,
								contextChain2,
								executionFlags.setInsideReturnRun(),
								abstractServerCommandSource,
								list
						)
				);
			}
		}
	}

	/**
	 * {@code ValueCommand}.
	 */
	static class ValueCommand<T extends AbstractServerCommandSource<T>> implements ControlFlowAware.Command<T> {

		public void execute(
				T abstractServerCommandSource,
				ContextChain<T> contextChain,
				ExecutionFlags executionFlags,
				ExecutionControl<T> executionControl
		) {
			int i = IntegerArgumentType.getInteger(contextChain.getTopContext(), "value");
			abstractServerCommandSource.getReturnValueConsumer().onSuccess(i);
			Frame frame = executionControl.getFrame();
			frame.succeed(i);
			frame.doReturn();
		}
	}
}
