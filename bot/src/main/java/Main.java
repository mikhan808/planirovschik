import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.LocalDateTime;

public class Main {
    public static void main(String ... args)
    {
        try {
            TelegramBotsApi telegramBotsApi = createTelegramBotsApi();
            try {
                telegramBotsApi.registerBot(new Bot());
                Bot bot = new Bot();
                Runnable r = new SendToTime(bot);
                Thread t = new Thread(r);
                t.start();
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static TelegramBotsApi createTelegramBotsApi() {
        return createLongPollingTelegramBotsApi();
    }

    private static TelegramBotsApi createLongPollingTelegramBotsApi() {
        try {
            return new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
