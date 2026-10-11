package dev.erie.rankmute.filter;

/**
 * Outcome of running one chat line through the {@link RankMatcher}.
 *
 * @param matchedRule human-readable description of the rule that matched, or {@code null} if none did
 * @param mentioned   whether the line mentioned the local player (only checked when a rule matched)
 */
public record MatchResult(String matchedRule, boolean mentioned) {
	public static final MatchResult NO_MATCH = new MatchResult(null, false);

	/** True if the message should be dropped from the chat HUD. */
	public boolean hidden() {
		return matchedRule != null && !mentioned;
	}

	/** True if a rule matched but the mention exemption kept the message visible. */
	public boolean exemptedByMention() {
		return matchedRule != null && mentioned;
	}
}
