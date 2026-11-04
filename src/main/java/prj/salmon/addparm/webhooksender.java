package prj.salmon.addparm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class webhooksender {
    // Discord のメッセージの長さの上限
    private static final int CONTENT_LIMIT = 2000;
    // Discord が書式、リンク、メンション、絵文字、時刻として扱う文字
    private static final String SPECIAL = "\\*_~`|>#-+[]()<@:";
    private static final String ELLIPSIS = "…";
    private static final int TIMEOUT_MILLIS = 10000;

    private final String webhookUrl;

    public webhooksender(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void createThreadAndSendLog(String title) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            System.err.println("Webhook URLが設定されていません。送信スキップします。");
            return;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setDoOutput(true);

            String jsonPayload = buildPayload(title);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                System.err.println("Webhook送信失敗: HTTP " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String buildPayload(String title) {
        JsonObject root = new JsonObject();
        root.addProperty("username", "Addbuilder");
        root.addProperty("content", sanitize(title, CONTENT_LIMIT));

        // 内容にメンションがあっても誰にも通知しない
        JsonObject allowedMentions = new JsonObject();
        allowedMentions.add("parse", new JsonArray());
        root.add("allowed_mentions", allowedMentions);

        return root.toString();
    }

    /** プレイヤーの文字列を、Discord の書式として働かない 1 行にして、上限に収める */
    static String sanitize(String text, int maxLength) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        text.codePoints().forEach(codePoint -> {
            int type = Character.getType(codePoint);
            if (Character.isISOControl(codePoint) || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR) {
                out.append(' ');
            } else if (type != Character.FORMAT) {
                // 見えない文字は、表示の順番を変えられるので落とす
                if (SPECIAL.indexOf(codePoint) >= 0) {
                    out.append('\\');
                }
                out.appendCodePoint(codePoint);
            }
        });
        return truncate(out, maxLength);
    }

    private static String truncate(CharSequence text, int maxLength) {
        if (text.length() <= maxLength) {
            return text.toString();
        }
        int end = maxLength - ELLIPSIS.length();
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        // 最後に \ が残ると、省略記号をエスケープしてしまう
        int backslashes = 0;
        while (backslashes < end && text.charAt(end - 1 - backslashes) == '\\') {
            backslashes++;
        }
        if (backslashes % 2 == 1) {
            end--;
        }
        return text.subSequence(0, end) + ELLIPSIS;
    }
}
