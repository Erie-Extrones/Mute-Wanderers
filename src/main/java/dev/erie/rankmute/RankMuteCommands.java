package dev.erie.rankmute;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import dev.erie.rankmute.filter.MatchResult;
import dev.erie.rankmute.filter.RankMatcher;

/**
 * {@code /rankmute ...} client-only commands. None of these reach the server.
 */
public final class RankMuteCommands {
	private static final int OK = 1;

	private RankMuteCommands() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(literal("rankmute")
				.executes(ctx -> status(ctx.getSource()))
				.then(literal("toggle").executes(ctx -> toggle(ctx.getSource())))
				.then(literal("add").then(argument("rank", StringArgumentType.greedyString())
						.executes(ctx -> addRank(ctx.getSource(), StringArgumentType.getString(ctx, "rank")))))
				.then(literal("remove").then(argument("rank", StringArgumentType.greedyString())
						.executes(ctx -> removeRank(ctx.getSource(), StringArgumentType.getString(ctx, "rank")))))
				.then(literal("list").executes(ctx -> list(ctx.getSource())))
				.then(literal("regex")
						.then(literal("add").then(argument("pattern", StringArgumentType.greedyString())
								.executes(ctx -> addRegex(ctx.getSource(), StringArgumentType.getString(ctx, "pattern")))))
						.then(literal("remove").then(argument("index", IntegerArgumentType.integer(1))
								.executes(ctx -> removeRegex(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"))))))
				.then(literal("template").then(argument("pattern", StringArgumentType.greedyString())
						.executes(ctx -> setTemplate(ctx.getSource(), StringArgumentType.getString(ctx, "pattern")))))
				.then(literal("reload").executes(ctx -> reload(ctx.getSource())))
				.then(literal("test").then(argument("text", StringArgumentType.greedyString())
						.executes(ctx -> test(ctx, StringArgumentType.getString(ctx, "text"))))));
	}

	private static int status(FabricClientCommandSource source) {
		RankMuteConfig cfg = ConfigManager.get();
		info(source, "RankMute is " + (cfg.enabled ? "enabled" : "disabled")
				+ " | " + cfg.hiddenRanks.size() + " hidden rank(s), " + cfg.regexRules.size() + " regex rule(s)"
				+ " | hid " + ChatFilter.hiddenThisSession() + " message(s) this session");
		info(source, "Template: " + cfg.prefixTemplate
				+ " | show if mentioned: " + cfg.showIfMentioned
				+ " | action-bar counter: " + cfg.showCounterInActionBar);
		List<String> errors = ConfigManager.matcher().errors();
		if (!errors.isEmpty()) {
			error(source, errors.size() + " invalid pattern(s) are being skipped; see the log.");
		}
		return OK;
	}

	private static int toggle(FabricClientCommandSource source) {
		RankMuteConfig cfg = ConfigManager.get();
		cfg.enabled = !cfg.enabled;
		ConfigManager.changed();
		ok(source, "RankMute " + (cfg.enabled ? "enabled" : "disabled") + ".");
		return OK;
	}

	private static int addRank(FabricClientCommandSource source, String rank) {
		rank = rank.trim();
		RankMuteConfig cfg = ConfigManager.get();
		if (rank.isEmpty()) {
			error(source, "Rank name is empty.");
			return 0;
		}
		String existing = cfg.findRank(rank);
		if (existing != null) {
			error(source, "'" + existing + "' is already hidden.");
			return 0;
		}
		cfg.hiddenRanks.add(rank);
		ConfigManager.changed();
		ok(source, "Now hiding rank '" + rank + "' (" + cfg.hiddenRanks.size() + " total).");
		return OK;
	}

	private static int removeRank(FabricClientCommandSource source, String rank) {
		RankMuteConfig cfg = ConfigManager.get();
		String existing = cfg.findRank(rank.trim());
		if (existing == null) {
			error(source, "'" + rank.trim() + "' is not in the hidden list.");
			return 0;
		}
		cfg.hiddenRanks.remove(existing);
		ConfigManager.changed();
		ok(source, "No longer hiding rank '" + existing + "' (" + cfg.hiddenRanks.size() + " left).");
		return OK;
	}

	private static int list(FabricClientCommandSource source) {
		RankMuteConfig cfg = ConfigManager.get();
		if (cfg.hiddenRanks.isEmpty()) {
			info(source, "No hidden ranks.");
		} else {
			info(source, "Hidden ranks (" + cfg.hiddenRanks.size() + "): " + String.join(", ", cfg.hiddenRanks));
		}
		if (cfg.regexRules.isEmpty()) {
			info(source, "No regex rules.");
		} else {
			info(source, "Regex rules (" + cfg.regexRules.size() + "):");
			for (int i = 0; i < cfg.regexRules.size(); i++) {
				info(source, "  " + (i + 1) + ". " + cfg.regexRules.get(i));
			}
		}
		return OK;
	}

	private static int addRegex(FabricClientCommandSource source, String pattern) {
		String problem = RankMatcher.validateRegex(pattern);
		if (problem != null) {
			error(source, "Invalid regex: " + problem);
			return 0;
		}
		RankMuteConfig cfg = ConfigManager.get();
		cfg.regexRules.add(pattern);
		ConfigManager.changed();
		ok(source, "Added regex rule #" + cfg.regexRules.size() + ": " + pattern);
		return OK;
	}

	private static int removeRegex(FabricClientCommandSource source, int index) {
		RankMuteConfig cfg = ConfigManager.get();
		if (index > cfg.regexRules.size()) {
			error(source, "No regex rule #" + index + " (there are " + cfg.regexRules.size() + "). Use /rankmute list.");
			return 0;
		}
		String removed = cfg.regexRules.remove(index - 1);
		ConfigManager.changed();
		ok(source, "Removed regex rule #" + index + ": " + removed);
		return OK;
	}

	private static int setTemplate(FabricClientCommandSource source, String template) {
		String problem = RankMatcher.validateTemplate(template);
		if (problem != null) {
			error(source, "Invalid template: " + problem);
			return 0;
		}
		ConfigManager.get().prefixTemplate = template;
		ConfigManager.changed();
		ok(source, "Prefix template set to: " + template);
		return OK;
	}

	private static int reload(FabricClientCommandSource source) {
		boolean clean = ConfigManager.load();
		if (clean) {
			ok(source, "Config reloaded: " + ConfigManager.matcher().ruleCount() + " rule(s) active.");
		} else {
			error(source, "Config could not be parsed; using defaults. See the log for details.");
		}
		return OK;
	}

	private static int test(CommandContext<FabricClientCommandSource> ctx, String text) {
		FabricClientCommandSource source = ctx.getSource();
		RankMuteConfig cfg = ConfigManager.get();
		MatchResult result = ConfigManager.matcher().test(text, ChatFilter.localUsername());

		if (result.hidden()) {
			info(source, "Would be HIDDEN by " + result.matchedRule() + (cfg.enabled ? "" : " (but RankMute is disabled)"));
		} else if (result.exemptedByMention()) {
			info(source, "Would be SHOWN: matched " + result.matchedRule() + " but it mentions you");
		} else {
			info(source, "Would be SHOWN: no rule matched");
		}
		return OK;
	}

	private static void info(FabricClientCommandSource source, String text) {
		source.sendFeedback(Component.literal("[RankMute] ").withStyle(ChatFormatting.AQUA)
				.append(Component.literal(text).withStyle(ChatFormatting.GRAY)));
	}

	private static void ok(FabricClientCommandSource source, String text) {
		source.sendFeedback(Component.literal("[RankMute] ").withStyle(ChatFormatting.AQUA)
				.append(Component.literal(text).withStyle(ChatFormatting.GREEN)));
	}

	private static void error(FabricClientCommandSource source, String text) {
		source.sendError(Component.literal("[RankMute] " + text));
	}
}
