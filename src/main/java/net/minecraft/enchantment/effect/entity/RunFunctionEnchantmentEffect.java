package net.minecraft.enchantment.effect.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Эффект зачарования, выполняющий серверную функцию (data pack function) в контексте сущности.
 * Функция запускается с правами уровня GAMEMASTERS, в тихом режиме, с позицией и ротацией сущности.
 * Если функция не найдена — логируется ошибка.
 */
public record RunFunctionEnchantmentEffect(Identifier function) implements EnchantmentEntityEffect {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<RunFunctionEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(Identifier.CODEC.fieldOf("function").forGetter(RunFunctionEnchantmentEffect::function))
					.apply(instance, RunFunctionEnchantmentEffect::new)
	);

	@Override
	public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
		MinecraftServer server = world.getServer();
		CommandFunctionManager functionManager = server.getCommandFunctionManager();
		Optional<CommandFunction<ServerCommandSource>> foundFunction = functionManager.getFunction(function);

		if (foundFunction.isEmpty()) {
			LOGGER.error("Enchantment run_function effect failed for non-existent function {}", function);
			return;
		}

		ServerCommandSource commandSource = server.getCommandSource()
				.withPermissions(LeveledPermissionPredicate.GAMEMASTERS)
				.withSilent()
				.withEntity(user)
				.withWorld(world)
				.withPosition(pos)
				.withRotation(user.getRotationClient());

		functionManager.execute(foundFunction.get(), commandSource);
	}

	@Override
	public MapCodec<RunFunctionEnchantmentEffect> getCodec() {
		return CODEC;
	}
}
