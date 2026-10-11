package dev.erie.rankmute;

import java.util.ArrayList;
import java.util.List;

import dev.erie.rankmute.filter.RankMatcher;

/**
 * Plain data class serialised to {@code config/rankmute.json} with Gson.
 * Field names are the JSON keys.
 */
public class RankMuteConfig {
	public boolean enabled = true;
	public String prefixTemplate = RankMatcher.DEFAULT_PREFIX_TEMPLATE;
	public boolean showIfMentioned = true;
	public boolean showCounterInActionBar = false;
	/** Development aid: logs which event (chat/game) delivered each hidden message. */
	public boolean debugLogEvents = false;
	public List<String> hiddenRanks = new ArrayList<>(List.of("Wanderer"));
	public List<String> regexRules = new ArrayList<>();

	/** Fills in anything Gson left null (missing keys) so the rest of the mod never null-checks. */
	public RankMuteConfig normalize() {
		if (prefixTemplate == null || prefixTemplate.isBlank()) {
			prefixTemplate = RankMatcher.DEFAULT_PREFIX_TEMPLATE;
		}
		if (hiddenRanks == null) {
			hiddenRanks = new ArrayList<>();
		} else {
			hiddenRanks.removeIf(r -> r == null || r.isBlank());
		}
		if (regexRules == null) {
			regexRules = new ArrayList<>();
		} else {
			regexRules.removeIf(r -> r == null || r.isBlank());
		}
		return this;
	}

	/** Case-insensitive lookup; returns the stored spelling or {@code null}. */
	public String findRank(String rank) {
		for (String r : hiddenRanks) {
			if (r.equalsIgnoreCase(rank)) {
				return r;
			}
		}
		return null;
	}

	public RankMatcher buildMatcher() {
		return RankMatcher.compile(prefixTemplate, hiddenRanks, regexRules, showIfMentioned);
	}
}
