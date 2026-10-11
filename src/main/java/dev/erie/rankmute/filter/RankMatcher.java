package dev.erie.rankmute.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Immutable, Minecraft-free matcher built from the config.
 *
 * <p>Two kinds of rules:
 * <ul>
 *   <li><b>rank</b> rules: a rank name expanded into the configured prefix template
 *       (default {@code ^\[%s\]}), case-insensitive, and only matched at the <em>start</em> of the line.</li>
 *   <li><b>regex</b> rules: raw Java regexes, matched anywhere in the line ({@link Matcher#find()}).</li>
 * </ul>
 *
 * <p>Invalid patterns never throw; they are skipped and reported through {@link #errors()}.
 */
public final class RankMatcher {
	/**
	 * Default template: optional leading icon glyphs or spaces, then the rank name, then a colon,
	 * e.g. {@code "█ Wanderer : Meigha hello"}. Matches the format of the server this mod was written for.
	 */
	public static final String DEFAULT_PREFIX_TEMPLATE = "^[^A-Za-z0-9]*%s\\s*:";

	private final List<CompiledRule> rules;
	private final List<String> errors;
	private final boolean showIfMentioned;

	private RankMatcher(List<CompiledRule> rules, List<String> errors, boolean showIfMentioned) {
		this.rules = Collections.unmodifiableList(rules);
		this.errors = Collections.unmodifiableList(errors);
		this.showIfMentioned = showIfMentioned;
	}

	/**
	 * Compiles a matcher.
	 *
	 * @param prefixTemplate  regex template; every {@code %s} is replaced with the quoted rank name
	 * @param hiddenRanks     rank names to hide
	 * @param regexRules      raw regexes to hide
	 * @param showIfMentioned if true, lines containing the local player's name are never hidden
	 */
	public static RankMatcher compile(String prefixTemplate, List<String> hiddenRanks, List<String> regexRules, boolean showIfMentioned) {
		List<CompiledRule> rules = new ArrayList<>();
		List<String> errors = new ArrayList<>();
		String template = prefixTemplate == null || prefixTemplate.isBlank() ? DEFAULT_PREFIX_TEMPLATE : prefixTemplate;

		if (hiddenRanks != null) {
			for (String rank : hiddenRanks) {
				if (rank == null || rank.isBlank()) {
					continue;
				}
				String regex = expandTemplate(template, rank.trim());
				try {
					rules.add(new CompiledRule("rank '" + rank.trim() + "'", Pattern.compile(regex, Pattern.CASE_INSENSITIVE), true));
				} catch (PatternSyntaxException e) {
					errors.add("rank '" + rank + "' (template '" + template + "'): " + e.getDescription());
				}
			}
		}

		if (regexRules != null) {
			for (int i = 0; i < regexRules.size(); i++) {
				String regex = regexRules.get(i);
				if (regex == null || regex.isBlank()) {
					continue;
				}
				try {
					rules.add(new CompiledRule("regex #" + (i + 1) + " '" + regex + "'", Pattern.compile(regex), false));
				} catch (PatternSyntaxException e) {
					errors.add("regex #" + (i + 1) + " '" + regex + "': " + e.getDescription());
				}
			}
		}

		return new RankMatcher(rules, errors, showIfMentioned);
	}

	/** Replaces every {@code %s} in the template with the regex-quoted rank name. */
	public static String expandTemplate(String template, String rank) {
		return template.replace("%s", Pattern.quote(rank));
	}

	/** Validates a template by compiling it with a sample rank; returns an error description or {@code null}. */
	public static String validateTemplate(String template) {
		if (template == null || template.isBlank()) {
			return "template is empty";
		}
		if (!template.contains("%s")) {
			return "template must contain %s (placeholder for the rank name)";
		}
		try {
			Pattern.compile(expandTemplate(template, "Sample"), Pattern.CASE_INSENSITIVE);
			return null;
		} catch (PatternSyntaxException e) {
			return e.getDescription();
		}
	}

	/** Validates a raw regex; returns an error description or {@code null}. */
	public static String validateRegex(String regex) {
		if (regex == null || regex.isBlank()) {
			return "pattern is empty";
		}
		try {
			Pattern.compile(regex);
			return null;
		} catch (PatternSyntaxException e) {
			return e.getDescription();
		}
	}

	/**
	 * Runs a raw chat line through the rules.
	 *
	 * @param rawLine  the message as plain text; {@code §} codes are stripped here
	 * @param username the local player's name for the mention exemption (may be null)
	 */
	public MatchResult test(String rawLine, String username) {
		if (rawLine == null) {
			return MatchResult.NO_MATCH;
		}
		String line = FormattingCodes.strip(rawLine).stripLeading();

		for (CompiledRule rule : rules) {
			Matcher m = rule.pattern.matcher(line);
			boolean hit = rule.anchored ? m.lookingAt() : m.find();
			if (hit) {
				boolean mentioned = showIfMentioned && mentions(line, username);
				return new MatchResult(rule.description, mentioned);
			}
		}
		return MatchResult.NO_MATCH;
	}

	/** Whole-word, case-insensitive check for the player's name. */
	static boolean mentions(String line, String username) {
		if (username == null || username.isBlank()) {
			return false;
		}
		Pattern p = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(username) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);
		return p.matcher(line).find();
	}

	public int ruleCount() {
		return rules.size();
	}

	/** Descriptions of patterns that failed to compile and were skipped. */
	public List<String> errors() {
		return errors;
	}

	private record CompiledRule(String description, Pattern pattern, boolean anchored) {
	}
}
