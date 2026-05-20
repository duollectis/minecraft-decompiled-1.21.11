package net.minecraft.test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public abstract class TestFunctionProvider {
   private static final List<TestFunctionProvider> PROVIDERS = new ArrayList<>();

   public static void addProvider(TestFunctionProvider provider) {
      PROVIDERS.add(provider);
   }

   public static void registerAll(Registry<Consumer<TestContext>> registry) {
      for (TestFunctionProvider testFunctionProvider : PROVIDERS) {
         testFunctionProvider.register((key, value) -> Registry.register(registry, key, value));
      }
   }

   public abstract void register(BiConsumer<RegistryKey<Consumer<TestContext>>, Consumer<TestContext>> registry);
}
