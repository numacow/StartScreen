package com.numa.startscreen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.numa.startscreen.config.SimpleConfig;
import com.numa.startscreen.fancymenu.CustomGUIResolver;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StartScreen implements ModInitializer {


	public static final String MODNAME = "Start Screen";
	public static final String MODID = "startscreen";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
	SimpleConfig CONFIG = SimpleConfig.of( MODID + "config" ).provider( this::provider ).request();
	private String provider( String filename ) {
		return "seconds=300";
	}
	public final int TIMER_IN_SECONDS = CONFIG.getOrDefault( "seconds", 300 );
	public final int TIMER_IN_MILLIS = TIMER_IN_SECONDS * 1000;
	private ScheduledExecutorService executorService;
	private static final HashMap<String, Long> playerTimeTracker = new HashMap<>();
	private static long serverUpTime = 0;

	synchronized public static HashMap<String, Long> getPlayerTimeTracker(){
		return playerTimeTracker;
	}

	@Override
	public void onInitialize() {
		CustomGUIResolver.load();

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			onShutdown();
		});

		// Use Fabric to bootstrap the Common mod.
		LOGGER.info("Hello Fabric world!");
	}

	private void onServerTick(MinecraftServer server) {
		long currentTimeMillis = System.currentTimeMillis();
		long elapsed = currentTimeMillis - serverUpTime;

		if (elapsed >= 1000 ) {
			synchronized (playerTimeTracker) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					String uuid = player.getUUID().toString();
					CommandSourceStack commandStack = player.createCommandSourceStack().withSuppressedOutput().withPermission(4);
					CommandDispatcher<CommandSourceStack> commanddispatcher = Objects.requireNonNull(player.getServer()).getCommands().getDispatcher();

					if (playerTimeTracker.containsKey(uuid)) {
						long lastPlayed = playerTimeTracker.getOrDefault(uuid, Long.MIN_VALUE);

						if (lastPlayed > serverUpTime) {
							LOGGER.info("Player {} had time greater than server uptime {} did the server restart since last join?", player.getUUID(), serverUpTime);
							playerTimeTracker.put(uuid, Long.MIN_VALUE);
							lastPlayed = 0;
						}

						if ((serverUpTime - lastPlayed) >= TIMER_IN_MILLIS) {
							LOGGER.debug("Player {} was offline for {} seconds", player.getUUID(), (serverUpTime - lastPlayed)/1000);
							String command = "openguiscreen startscreen_start";

							ParseResults<CommandSourceStack> results = commanddispatcher.parse(command, commandStack);
							player.getServer().getCommands().performCommand(results, command);
						}
					}
					else {
						LOGGER.debug("New Player {}", player.getUUID());
						playerTimeTracker.put(uuid, currentTimeMillis);
						String command = "openguiscreen startscreen_start";
						ParseResults<CommandSourceStack> results = commanddispatcher.parse(command, commandStack);
						player.getServer().getCommands().performCommand(results, command);
					}
				}
			}
			serverUpTime = currentTimeMillis;
		}
	}

	private void onShutdown() {
		shutdownExecutorService();
	}

	private void shutdownExecutorService() {
		executorService.shutdown();

		try {
			// Wait a while for existing tasks to terminate
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				executorService.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Executor service did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executorService.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
}
