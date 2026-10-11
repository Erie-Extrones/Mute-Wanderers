package dev.erie.rankmute;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import dev.erie.rankmute.filter.MatchResult;

/**
 * Hooks Fabric API's receive events and decides whether a message reaches the chat HUD.
 * Nothing here talks to the server.
 */
public final class ChatFilter {
	private static int hiddenThisSession = 0;

	private ChatFilter() {
	}

	public static void register() {
		// Signed player chat.
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
				allow(message, "chat"));

		// System/game messages. Servers with rank plugins usually send player chat this way.
		// Overlay (action bar) text is never chat, so it is always allowed through.
		ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
				overlay || allow(message, "game"));
	}

	private static boolean allow(Component message, String source) {
		if (!ConfigManager.get().enabled) {
			return true;
		}

		String text = message.getString();
		MatchResult result = ConfigManager.matcher().test(text, localUsername());
		if (!result.hidden()) {
			return true;
		}

		hiddenThisSession++;
		if (ConfigManager.get().debugLogEvents) {
			RankMuteClient.LOGGER.info("[{}] hidden by {}: {}", source, result.matchedRule(), text);
		}
		if (ConfigManager.get().showCounterInActionBar) {
			showCounter();
		}
		return false;
	}

	/** The local player's username, for the mention exemption. */
	public static String localUsername() {
		Minecraft client = Minecraft.getInstance();
		return client == null ? null : client.getUser().getName();
	}

	private static void showCounter() {
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.player != null) {
			// Player-level API rather than Gui/Hud: it is identical across the Minecraft versions we target.
			client.player.sendOverlayMessage(
					Component.literal("RankMute: hid " + hiddenThisSession).withStyle(ChatFormatting.GRAY));
		}
	}

	public static int hiddenThisSession() {
		return hiddenThisSession;
	}
}
