import java.sql.ResultSet;

public class SendToTime implements Runnable {
    Bot bot;

    public SendToTime(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        while (true) {
            try {
                checkTasks(1,"Подходит срок начала задачи:","LAST_TIME_ADVANCE");
                checkTasks(3,"Начало задачи:","LAST_TIME_BEGIN");
                checkTasks(5,"Подошло время завершить задачу:","LAST_TIME_END");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            try {
                Thread.sleep(59000);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }


    }
    void checkTasks(int status,String firstMsg,String colName)
    {
        String query = QueryExecutor.buildQueryForAllRecordsWithCondition(Bot.TABLES[Bot.ONE_TIME],"STATUS = "+status);
        bot.sendInfoAboutTasks(query,firstMsg,true,colName);
    }
}
