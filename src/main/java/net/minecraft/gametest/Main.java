package net.minecraft.gametest;

import net.minecraft.SharedConstants;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.test.TestBootstrap;

/**
 * Точка входа для запуска игровых тестов (GameTest) в headless-режиме.
 * Инициализирует версию игры и передаёт управление {@link TestBootstrap}.
 */
public class Main {

	@DontObfuscate
	public static void main(String[] args) throws Exception {
		SharedConstants.createGameVersion();
		TestBootstrap.run(args, string -> {});
	}
}
