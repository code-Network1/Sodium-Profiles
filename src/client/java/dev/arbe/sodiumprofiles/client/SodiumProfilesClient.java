package dev.arbe.sodiumprofiles.client;

import dev.arbe.sodiumprofiles.SodiumProfiles;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SodiumProfilesClient implements ClientModInitializer {

	private static final Queue<Runnable> DEFERRED_ACTIONS = new ConcurrentLinkedQueue<>();

	public static void deferAction(Runnable action) {
		DEFERRED_ACTIONS.add(action);
	}

	@Override
	public void onInitializeClient() {
		String version = FabricLoader.getInstance()
				.getModContainer(SodiumProfiles.MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("?");

		SodiumProfiles.LOGGER.info("  ____            _ _                   ____             __ _ _");
		SodiumProfiles.LOGGER.info(" / ___|  ___   __| (_)_   _ _ __ ___   |  _ \\ _ __ ___  / _(_) | ___  ___");
		SodiumProfiles.LOGGER.info(" \\___ \\ / _ \\ / _` | | | | | '_ ` _ \\  | |_) | '__/ _ \\| |_| | |/ _ \\/ __|");
		SodiumProfiles.LOGGER.info("  ___) | (_) | (_| | | |_| | | | | | | |  __/| | | (_) |  _| | |  __/\\__ \\");
		SodiumProfiles.LOGGER.info(" |____/ \\___/ \\__,_|_|\\__,_|_| |_| |_| |_|   |_|  \\___/|_| |_|_|\\___||___/");
		SodiumProfiles.LOGGER.info(" v{} - Performance profiles for Sodium", version);

		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			Runnable action;
			while ((action = DEFERRED_ACTIONS.poll()) != null) {
				try {
					action.run();
				} catch (Exception e) {
					SodiumProfiles.LOGGER.error("[SodiumProfiles] Error in deferred action", e);
				}
			}
		});
	}
}
