package net.minecraft.test;

public interface TestListener {
   void onStarted(GameTestState test);

   void onPassed(GameTestState test, TestRunContext context);

   void onFailed(GameTestState test, TestRunContext context);

   void onRetry(GameTestState lastState, GameTestState nextState, TestRunContext context);
}
