package dev.erie.rankmute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class RankMuteClient implements ClientModInitializer {
	public static final String MOD_ID = "rankmute";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ConfigManager.load();
		ChatFilter.register();
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> RankMuteCommands.register(dispatcher));
		LOGGER.info("RankMute loaded with {} rule(s)", ConfigManager.matcher().ruleCount());
	}
}
