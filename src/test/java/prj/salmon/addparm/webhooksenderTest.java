package prj.salmon.addparm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class webhooksenderTest {
    private static JsonObject payload(String title) {
        return JsonParser.parseString(webhooksender.buildPayload(title)).getAsJsonObject();
    }

    private static String content(String title) {
        return payload(title).get("content").getAsString();
    }

    @Test
    void mentionsNeverNotifyAnyone() {
        JsonObject payload = payload("@everyone");
        assertEquals("Addbuilder", payload.get("username").getAsString());
        assertEquals(0, payload.getAsJsonObject("allowed_mentions").getAsJsonArray("parse").size());
    }

    @Test
    void jsonIsValidForAnyText() {
        assertEquals("a\"b", content("a\"b"));
        assertEquals("a b  c", content("a\nb\r\nc"));
        assertEquals("a\\\\b", content("a\\b"));
        assertEquals("日本語 😀", content("日本語 😀"));
    }

    @Test
    void formattingLinksAndMentionsAreEscaped() {
        assertEquals("Console が a\\_b\\_c に WEを付与しました", content("Console が a_b_c に WEを付与しました"));
        assertEquals("\\[x\\]\\(https\\://example.com\\)", content("[x](https://example.com)"));
        assertEquals("\\@everyone \\<\\@123\\>", content("@everyone <@123>"));
        assertEquals("\\*\\*a\\*\\* \\`b\\` \\~\\~c\\~\\~ \\|\\|d\\|\\| \\> e \\# f \\- g", content("**a** `b` ~~c~~ ||d|| > e # f - g"));
    }

    @Test
    void invisibleCharactersAreRemoved() {
        assertEquals("abc", content("a‮b​c"));
    }

    @Test
    void contentFitsDiscordLimit() {
        assertEquals("a".repeat(2000), content("a".repeat(2000)));
        String cut = content("a".repeat(2001));
        assertEquals(2000, cut.length());
        assertTrue(cut.endsWith("…"));
    }

    @Test
    void cuttingLeavesNoEscapeOrHalfCharacterAtTheEnd() {
        String escaped = content("a" + "*".repeat(3000));
        assertTrue(escaped.length() <= 2000);
        assertTrue(escaped.endsWith("\\*…"), escaped.substring(escaped.length() - 5));

        String emoji = content("a" + "😀".repeat(3000));
        assertTrue(emoji.length() <= 2000);
        assertFalse(Character.isHighSurrogate(emoji.charAt(emoji.length() - 2)));
    }
}
