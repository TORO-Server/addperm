package prj.salmon.addparm;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class webhooksender {
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
            connection.setDoOutput(true);

            String jsonPayload = """
                {
                    "username": "Addbuilder",
                    "content": "%s"
                }
                """.formatted(title.replace("\"", "\\\""));

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes());
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
}
