package dev.erie.rankmute.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RankMatcherTest {
	private static final String ME = "Erie";

	private static RankMatcher defaults(String... ranks) {
		return RankMatcher.compile(RankMatcher.DEFAULT_PREFIX_TEMPLATE, List.of(ranks), List.of(), true);
	}

	@Test
	void hidesRankPrefixAtStartOfLine() {
		MatchResult r = defaults("Wanderer").test("[Wanderer] Steve: hello", ME);
		assertTrue(r.hidden());
		assertEquals("rank 'Wanderer'", r.matchedRule());
	}

	@Test
	void showsOtherRanks() {
		assertFalse(defaults("Wanderer").test("[Knight] Steve: hello", ME).hidden());
		assertFalse(defaults("Wanderer").test("Steve: hello", ME).hidden());
	}

	@Test
	void rankRuleIsCaseInsensitive() {
		assertTrue(defaults("Wanderer").test("[WANDERER] Steve: hi", ME).hidden());
		assertTrue(defaults("wanderer").test("[Wanderer] Steve: hi", ME).hidden());
	}

	@Test
	void rankRuleOnlyMatchesAtStartOfLine() {
		assertFalse(defaults("Wanderer").test("Steve: I saw [Wanderer] earlier", ME).hidden());
		assertFalse(defaults("Wanderer").test("<Steve> [Wanderer] hi", ME).hidden());
	}

	@Test
	void rankRuleAnchorsEvenWithoutCaretInTemplate() {
		RankMatcher m = RankMatcher.compile("\\[%s\\]", List.of("Wanderer"), List.of(), true);
		assertTrue(m.test("[Wanderer] Steve: hi", ME).hidden());
		assertFalse(m.test("Steve: [Wanderer] hi", ME).hidden());
	}

	@Test
	void rankNameIsRegexQuoted() {
		assertTrue(defaults("VIP+").test("[VIP+] Steve: hi", ME).hidden());
		assertFalse(defaults("VIP+").test("[VIPP] Steve: hi", ME).hidden());
	}

	@Test
	void customTemplate() {
		RankMatcher m = RankMatcher.compile("^%s \\| ", List.of("Wanderer"), List.of(), true);
		assertTrue(m.test("Wanderer | Steve » hi", ME).hidden());
		assertFalse(m.test("[Wanderer] Steve: hi", ME).hidden());
	}

	@Test
	void stripsSectionFormattingCodes() {
		assertTrue(defaults("Wanderer").test("§7[§aWanderer§7] §fSteve§7: §fhello", ME).hidden());
		assertEquals("[Wanderer] hi", FormattingCodes.strip("§7[§aWanderer§7] hi"));
		assertEquals("abc", FormattingCodes.strip("abc§"));
	}

	@Test
	void leadingWhitespaceIsIgnored() {
		assertTrue(defaults("Wanderer").test("  [Wanderer] Steve: hi", ME).hidden());
	}

	@Test
	void mentionExemptionShowsMessage() {
		MatchResult r = defaults("Wanderer").test("[Wanderer] Steve: hey Erie come here", ME);
		assertFalse(r.hidden());
		assertTrue(r.exemptedByMention());
		assertEquals("rank 'Wanderer'", r.matchedRule());
	}

	@Test
	void mentionIsCaseInsensitiveAndWholeWord() {
		assertFalse(defaults("Wanderer").test("[Wanderer] Steve: hey erie", ME).hidden());
		assertTrue(defaults("Wanderer").test("[Wanderer] Steve: hey Erieanne", ME).hidden());
		assertFalse(defaults("Wanderer").test("[Wanderer] Steve: @Erie!", ME).hidden());
	}

	@Test
	void mentionExemptionCanBeDisabled() {
		RankMatcher m = RankMatcher.compile(RankMatcher.DEFAULT_PREFIX_TEMPLATE, List.of("Wanderer"), List.of(), false);
		MatchResult r = m.test("[Wanderer] Steve: hey Erie", ME);
		assertTrue(r.hidden());
		assertFalse(r.mentioned());
	}

	@Test
	void mentionWithNullUsernameNeverExempts() {
		assertTrue(defaults("Wanderer").test("[Wanderer] Steve: hey Erie", null).hidden());
	}

	@Test
	void regexRulesMatchAnywhere() {
		RankMatcher m = RankMatcher.compile(RankMatcher.DEFAULT_PREFIX_TEMPLATE, List.of(), List.of("(?i)buy ranks at"), true);
		MatchResult r = m.test("Steve: BUY RANKS AT example.com", ME);
		assertTrue(r.hidden());
		assertEquals("regex #1 '(?i)buy ranks at'", r.matchedRule());
	}

	@Test
	void regexRulesAreCaseSensitiveUnlessFlagged() {
		RankMatcher m = RankMatcher.compile(RankMatcher.DEFAULT_PREFIX_TEMPLATE, List.of(), List.of("Wanderer \\| "), true);
		assertTrue(m.test("Wanderer | Steve » hi", ME).hidden());
		assertFalse(m.test("wanderer | Steve » hi", ME).hidden());
	}

	@Test
	void invalidPatternsAreSkippedNotThrown() {
		RankMatcher m = RankMatcher.compile("^\\[%s", List.of("Wanderer"), List.of("(unclosed", "ok"), true);
		// The template expands to "^\[\QWanderer\E", which is valid, so only "(unclosed" is dropped.
		assertEquals(2, m.ruleCount());
		assertEquals(1, m.errors().size());
		assertTrue(m.errors().get(0).startsWith("regex #1"));
		assertTrue(m.test("ok then", ME).hidden());
	}

	@Test
	void invalidTemplateIsReported() {
		RankMatcher m = RankMatcher.compile("^[%s", List.of("Wanderer"), List.of(), true);
		assertEquals(0, m.ruleCount());
		assertEquals(1, m.errors().size());
		assertFalse(m.test("[Wanderer] Steve: hi", ME).hidden());
	}

	@Test
	void blankAndNullEntriesAreIgnored() {
		RankMatcher m = RankMatcher.compile(null, java.util.Arrays.asList("", null, "Wanderer"), java.util.Arrays.asList(null, " "), true);
		assertEquals(1, m.ruleCount());
		assertTrue(m.errors().isEmpty());
		assertTrue(m.test("[Wanderer] Steve: hi", ME).hidden());
	}

	@Test
	void validators() {
		assertNull(RankMatcher.validateTemplate(RankMatcher.DEFAULT_PREFIX_TEMPLATE));
		assertNotNull(RankMatcher.validateTemplate("^\\[Wanderer\\]"));
		assertNotNull(RankMatcher.validateTemplate("^[%s"));
		assertNotNull(RankMatcher.validateTemplate(""));
		assertNull(RankMatcher.validateRegex("a+b"));
		assertNotNull(RankMatcher.validateRegex("(a"));
		assertNotNull(RankMatcher.validateRegex(" "));
	}

	@Test
	void nullLineIsNoMatch() {
		assertFalse(defaults("Wanderer").test(null, ME).hidden());
	}
}
