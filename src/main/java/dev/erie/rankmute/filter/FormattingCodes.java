package dev.erie.rankmute.filter;

/**
 * Strips legacy {@code §x} formatting codes from plain chat text.
 *
 * <p>Kept Minecraft-free so the matcher can be unit-tested without the game on the classpath.
 */
public final class FormattingCodes {
	private static final char SECTION_SIGN = '§';

	private FormattingCodes() {
	}

	/**
	 * Removes every {@code §} and the single character that follows it.
	 * A trailing lone {@code §} is dropped as well.
	 */
	public static String strip(String text) {
		if (text == null || text.indexOf(SECTION_SIGN) < 0) {
			return text;
		}

		StringBuilder out = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == SECTION_SIGN) {
				i++; // skip the code character too
				continue;
			}
			out.append(c);
		}
		return out.toString();
	}
}
