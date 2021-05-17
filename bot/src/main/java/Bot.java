import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class Bot extends TelegramLongPollingBot {


    public static final int SHOW_TASKS_TODAY = 0;
    public static final int CHANGE_TIME_ADVANCE = SHOW_TASKS_TODAY +1;
    public static final int ADD_TASK = CHANGE_TIME_ADVANCE+1;
    public static final int SHOW_CURRENT_TASKS = ADD_TASK + 1;
    public static final int SHOW_SUCCESS_TASKS= SHOW_CURRENT_TASKS + 1;
    public static final int SHOW_FAILURE_TASKS = SHOW_SUCCESS_TASKS + 1;
    public static final int POSTPONE_TASK = SHOW_FAILURE_TASKS + 1;
    public static final int CANCEL = POSTPONE_TASK + 1;
    public static final int HELP = CANCEL + 1;
    public static final int LIST_COMMANDS = HELP + 1;

    public final static String [] COMMANDS = {
            "show_tasks_today",
            "change_time_advance",
            "add_task",
            "show_current_tasks",
            "show_success_tasks",
            "show_failure_tasks",
            "postpone_task",
            "cancel",
            "help",
            "list_commands"
    };

    public final static String [] DESCRIPTIONS = {
            "Показать задачи на сегодня",
            "Изменить количество минут до начала задачи в которые приходит напоминание о задаче",
            "Добавить задачу",
            "Показать текущие задачи",
            "Показать выполненные задачи",
            "Показать провальные задачи",
            "Отложить задачу",
            "Отмена",
            "Помощь - список команд",
            "Список команд без косой черты"
    };

    public final static int ONE_TIME = 0;
    public final static int RECURRING = ONE_TIME+1;

    public final static String ONE_TIME_S = "Единоразовая";
    public final static String RECURRING_S = "Повторяющаяся";

    public final static String[] TASK_TYPES={
            ONE_TIME_S,
            RECURRING_S
    };

    public final static String[] TABLES={
         "TASKS",
         "RECURRING_TASKS"
    };

    public final static String CHAT_ID = "CHAT_ID";
    public final static String CHATS = "CHATS";
    public final static String TASK_ID = "TASK_ID";
    public final static String TABLE_TASK= "TABLE_TASK";

    public static String buildHelpMessage()
    {
        StringBuilder res = new StringBuilder();
        for(int i=0;i<DESCRIPTIONS.length;i++)
        {
            res.append(getCommand(i)).append(" - ").append(DESCRIPTIONS[i]).append("\n");
        }
        return res.toString();
    }

    public static String buildCommands()
    {
        StringBuilder res = new StringBuilder();
        for(int i=0;i<DESCRIPTIONS.length;i++)
        {
            res.append(COMMANDS[i]).append(" - ").append(DESCRIPTIONS[i]).append("\n");
        }
        return res.toString();
    }

    public static String getCommand(int number)
    {
        return "/"+COMMANDS[number];
    }

    public final static String [] YES_NO =
            {
                    "Да",
                    "Нет"
            };


    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();
        Long id = msg.getChatId();
        Chat chat = msg.getChat();
        int status;
        try {
            String query = "select * from chats where chat_id = " + id;
            ResultSet rs = QueryExecutor.getResultSet(query);
            if (rs != null) {
                if (rs.next()) {
                    status = rs.getInt("STATUS");
                } else {
                    if (Objects.equals(msg.getText(), BotSettings.PASSWORD)) {
                        addNewChatAndDialog(chat);
                        status = Status.NORMAL;
                    } else {
                        sendMsg(id, "Введите пароль");
                        QueryExecutor.releaseResources(rs);
                        return;
                    }
                }
            } else return;
            QueryExecutor.releaseResources(rs);
            String txt = msg.getText();
            if (txt != null) {
                if (txt.equals(getCommand(CANCEL)))
                    updateStatus(id, Status.NORMAL);
                else if (txt.equals(getCommand(HELP)))
                    sendMsg(id,buildHelpMessage());
                else if (txt.equals(getCommand(LIST_COMMANDS)))
                    sendMsg(id,buildCommands());
                else
                    switch (status) {
                        case Status.NORMAL:
                            if (txt.equals("Привет"))
                                sendMsg(id, "Привет");
                            if (txt.equals(getCommand(SHOW_TASKS_TODAY))) {

                            } else if (txt.equals(getCommand(ADD_TASK))) {
                                updateStatus(id,Status.SELECT_TYPE_TASK);
                                sendKeyBoard(id,"Выберите тип задачи",TASK_TYPES);
                            } else if (txt.equals(getCommand(SHOW_CURRENT_TASKS))) {

                            } else if (txt.equals(getCommand(SHOW_SUCCESS_TASKS))) {

                            } else if (txt.equals(getCommand(SHOW_FAILURE_TASKS))) {

                            } else if (txt.equals(getCommand(POSTPONE_TASK))) {

                            }  else if (txt.equals(getCommand(CHANGE_TIME_ADVANCE))) {

                            }
                            break;
                            case Status.SELECT_TYPE_TASK:
                                insertTask(id,txt);
                                break;
                            case Status.TASK:
                                String tableName = getWorkTableForChat(id);
                                String taskId = getTaskIdForChat(id);
                                QueryExecutor.updateDataRecordForID(taskId,tableName,"TASK",txt,true);
                                updateStatus(id,Status.EXECUTOR);
                                sendMsg(id,"Введите исполнителя задачи");
                                break;
                        case Status.EXECUTOR:
                            tableName = getWorkTableForChat(id);
                            taskId = getTaskIdForChat(id);
                            QueryExecutor.updateDataRecordForID(taskId,tableName,"EXECUTOR",txt,true);
                            if(TABLES[ONE_TIME].equals(tableName)) {
                                updateStatus(id, Status.BEGIN_DATE);
                                sendMsg(id, "Введите дату начала выполнения задачи");
                            }
                            else {
                                updateStatus(id,Status.BEGIN_TIME);
                                sendMsg(id,"Введите время начала задачи в формате ЧЧ:ММ");
                            }
                            break;
                        case Status.BEGIN_DATE:
                            fillDate(id,txt,"BEGIN_TIME",Status.BEGIN_TIME,"Введите время начала задачи в формате ЧЧ:ММ");
                            break;
                        case Status.BEGIN_TIME:
                            fillTime(id,txt,"BEGIN_TIME",Status.END_DATE,Status.END_TIME,"Введите дату завершения задачи","Введите время завершения задачи в формате ЧЧ:ММ");
                            break;
                        case Status.END_DATE:
                            fillDate(id,txt,"END_TIME",Status.END_TIME,"Введите время завершения задачи в формате ЧЧ:ММ");
                            break;
                        case Status.END_TIME:
                            fillTime(id,txt,"END_TIME",Status.NORMAL,Status.SELECT_REPEAT_TYPE,"Задача добавлена","Выберите режим повторения для вашей задачи");
                            break;
                            case Status.REQUEST_DONE:
                                if(txt.equals(YES_NO[1]))
                                {
                                    updateStatus(id,Status.TYPE_REASON_FAILURE);
                                    QueryExecutor.updateDataRecordForID(getTaskIdForChat(id),TABLES[ONE_TIME],"DONE","false",false);
                                    sendMsg(id,"Напишите причину невыполнения задачи");
                                } else if (txt.equals(YES_NO[0]))
                            {
                                updateStatus(id,Status.NORMAL);
                                QueryExecutor.updateDataRecordForID(getTaskIdForChat(id),TABLES[ONE_TIME],"DONE","true",false);
                            } else {
                                    sendKeyBoard(id,"Для ответа воспользуйтесь кнопками бота. Выполнили вы эту задачу?",YES_NO);
                                }
                                break;
                                case Status.TYPE_REASON_FAILURE:
                                    QueryExecutor.updateDataRecordForID(getTaskIdForChat(id),TABLES[ONE_TIME],"REASON_FOR_FAILURE",txt,true);
                                    sendKeyBoard(id,"Ок",new ArrayList<>());
                                    break;
                        default:break;
                    }
            }

        } catch (Exception e) {
            Log.error(e.getMessage());
            sendMsg(chat.getId(), "Ошибка: " + e.getMessage());
        }


    }

    int getStatusForChatID(Long id)
    {
        return Integer.parseInt(QueryExecutor.getFirstValueFromResultSet(QueryExecutor.getAllRecordsFromTableWithCondition(CHATS,CHAT_ID+" = "+id),"STATUS"));
    }

    void fillTime(Long id,String txt,String colName,int firstNextStatus,int secondNextStatus,String firstMsg,String secondMsg)
    {
        String tableName = getWorkTableForChat(id);
        String taskId = getTaskIdForChat(id);
        txt+=":00";
        Object time;
        if(TABLES[ONE_TIME].equals(tableName)) {
            String timestamp = QueryExecutor.getFirstValueFromResultSet(
                    QueryExecutor.getAllRecordsFromTableWithCondition(tableName,"id = "+taskId),colName);
            time = dateAndTime(timestamp,txt);
        }
        else try {
            time = Time.valueOf(txt);
        }catch (Exception e)
        {
            e.printStackTrace();
            time = null;
        }
        if(time!=null) {
            QueryExecutor.updateDataRecordForIDByParam(taskId, tableName, colName, time);
            if(TABLES[ONE_TIME].equals(tableName)) {
                updateStatus(id, firstNextStatus);
                sendMsg(id, firstMsg);
            }
            else {
                updateStatus(id,secondNextStatus);
                sendMsg(id,secondMsg);
            }
        } else sendErrorEnter(id);
    }

    void fillDate(Long id,String txt, String field, int nextStatus,String nextMessage)
    {
        String tableName = getWorkTableForChat(id);
        String taskId = getTaskIdForChat(id);
        Timestamp t = parsingDate(txt);
        if(t!=null) {
            QueryExecutor.updateDataRecordForIDByParam(taskId, tableName, field, t);
            updateStatus(id, nextStatus);
            sendMsg(id,nextMessage);
        }
        else sendErrorEnter(id);
    }

    void sendErrorEnter(Long id)
    {
        sendMsg(id,"Что-то неверно, повторите ввод!");
    }

    public static final String[] DAYS_OF_WEEK=
            {
                    "ПОНЕДЕЛЬНИК",
                    "ВТОРНИК",
                    "СРЕДА",
                    "ЧЕТВЕРГ",
                    "ПЯТНИЦА",
                    "СУББОТА",
                    "ВОСКРЕСЕНЬЕ"
            };

    int dayOfWeek(String txt)
    {
        for(int i =0;i<DAYS_OF_WEEK.length;i++)
        {
            if(txt.equals(DAYS_OF_WEEK[i]))
                return i+1;
        }
        return -1;
    }

    Timestamp dateAndTime(String timestamp, String time)
    {
        try {
            Timestamp x = Timestamp.valueOf(timestamp);
            LocalDateTime localDateTime = x.toLocalDateTime();
            LocalTime localTime = LocalTime.parse(time);
            localDateTime=localDateTime.withHour(localTime.getHour());
            localDateTime=localDateTime.withMinute(localTime.getMinute());
            localDateTime = localDateTime.withSecond(localTime.getSecond());
            return Timestamp.valueOf(localDateTime);

        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }


    }

    Timestamp parsingDate(String txt)
    {
        try {
            return Timestamp.valueOf(txt);
        } catch (Exception e)
        {
            LocalDateTime t = LocalDateTime.now();
            txt = txt.toUpperCase();
            switch (txt)
            {
                case "ПОЗАВЧЕРА": return Timestamp.valueOf(t.minusDays(2));
                case "ВЧЕРА": return Timestamp.valueOf(t.minusDays(1));
                case "СЕГОДНЯ": return Timestamp.valueOf(t);
                case "ЗАВТРА": return Timestamp.valueOf(t.plusDays(1));
                case "ПОСЛЕЗАВТРА":return Timestamp.valueOf(t.plusDays(2));
            }
            int day = dayOfWeek(txt);
            if (day!=-1)
            {
                while(t.getDayOfWeek()!= DayOfWeek.of(day))
                    t = t.plusDays(1);
                return Timestamp.valueOf(t);
            }

        }
        return null;
    }

    String getWorkTableForChat(Long id)
    {
        return QueryExecutor.getFirstValueFromResultSet(QueryExecutor.getAllRecordsFromTableWithCondition(CHATS,CHAT_ID+" = "+id),TABLE_TASK).trim();
    }
    String getTaskIdForChat(Long id)
    {
        return QueryExecutor.getFirstValueFromResultSet(QueryExecutor.getAllRecordsFromTableWithCondition(CHATS,CHAT_ID+" = "+id),TASK_ID).trim();
    }

    void sendAdmin(Chat chat, String txt) {
        try {
            txt = "ID=" + chat.getId() + "\n" +
                    "Имя:" + chat.getFirstName() + " " + chat.getLastName() + "\n" + txt;
            sendAdmin(txt);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }
    void sendAdmin( String txt) {
        try {
            String query = "select * from chats where nickname = 'mikhan808'";
            Long id = (long) 0;
            ResultSet rs = QueryExecutor.getResultSet(query);
            while (rs.next()) {
                id = rs.getLong(CHAT_ID);
            }
            QueryExecutor.releaseResources(rs);
            sendMsg(id, txt);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    List<Long> allChats() {
        List<Long> idChats = new ArrayList<>();
        try {
            ResultSet rs = QueryExecutor.getAllRecordsFromTable(CHATS);
            while (rs.next()) {
                idChats.add(rs.getLong("CHAT_ID"));
            }
            QueryExecutor.releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        return idChats;
    }

    void sendAll(String txt) {
        try {
            List<Long> idChats = allChats();
            for (Long id : idChats) {
                sendMsg(id, txt);
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }








    void insertTask(Long id,String type) {
        String task_id;
        int type_task;
        List<Object> params = new ArrayList<>();
        switch (type)
        {
            case ONE_TIME_S:
                type_task = ONE_TIME;
                break;
            case RECURRING_S:
                type_task = RECURRING;
                break;
            default:
                sendKeyBoard(id,"Пожалуйста выберите тип задачи с помощью кнопок бота",TASK_TYPES);
                return;
        }
        int countCols = 11;
        if(type_task==RECURRING)
            countCols = 9;
        String query = QueryExecutor.buildQueryForInsert(TABLES[type_task],countCols);
        task_id = QueryExecutor.getNextIdForTask(TABLES[type_task]);
        try {
            params.add(task_id);
            params.add(id);
            for(int i=3;i<=countCols;i++)
            {
                params.add(null);
            }
            QueryExecutor.executeUpdate(query,params);
           updateTaskIdAndTableForChatId(id,task_id,type_task);
            updateStatus(id,Status.TASK);
            sendKeyBoard(id,"Введите текст задачи",new ArrayList<>());

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }

    void updateTaskIdAndTableForChatId(Long id,String task_id,int type_task)
    {
        QueryExecutor.updateDataRecordForOneField(CHAT_ID,id.toString(),CHATS,TASK_ID,task_id,true);
        QueryExecutor.updateDataRecordForOneField(CHAT_ID,id.toString(),CHATS,TABLE_TASK,TABLES[type_task],true);
    }






    void updateStatus(Long id, int status) {
        try {
            String query = "UPDATE CHATS\n" +
                    "SET STATUS = " + status + "\n" +
                    "where CHAT_ID =  " + id;
            QueryExecutor.executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }



    void deleteID(Chat chat) {
        try {
            String query = "delete from CHATS where CHAT_ID =  " + chat.getId();
            QueryExecutor.executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "";
    }

    @Override
    public String getBotToken() {
        return BotSettings.TOKEN;
    }

    public void sendKeyBoard(Long chatId, String text, String[] buttons) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        for (int i = 0; i < buttons.length; i += 2) {
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(buttons[i]);
            if (i + 1 < buttons.length)
                keyboardRow.add(buttons[i + 1]);
            keyboard.add(keyboardRow);
        }
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    public void sendKeyBoard(Long chatId, String text, List<String> buttons) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += 2) {
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(buttons.get(i));
            if (i + 1 < buttons.size())
                keyboardRow.add(buttons.get(i + 1));
            keyboard.add(keyboardRow);
        }
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(Long chatId, String text) {
        SendMessage s = new SendMessage();
        s.setChatId(chatId.toString()); // Боту может писать не один человек, и поэтому чтобы отправить сообщение, грубо говоря нужно узнать куда его отправлять
        s.setText(text);
        try { //Чтобы не крашнулась программа при вылете Exception
            execute(s);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    void addNewChatAndDialog(Chat chat) {
        try {
            String query = "INSERT INTO CHATs VALUES ( " + chat.getId() + ", '" + chat.getFirstName() + " " + chat.getLastName() + "', '" + chat.getUserName() + "', "+Status.NORMAL+", -1, null )";
            QueryExecutor.executeUpdate(query);
            query = "INSERT INTO ADVANCES VALUES ( " + chat.getId() + ", " + 30 + ")";
            QueryExecutor.executeUpdate(query);
            String txt = "К боту планирования присоединился пользователь: '" + chat.getFirstName() + " " + chat.getLastName() + "', '" + chat.getUserName()+"'";
            sendAdmin(txt);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }



    String buildDate(int days) {
        if (days == 0)
            return "current_date";
        return "current_date+" + days;
    }






    String getDayFormatted(int x) {
        int y;
        if (x % 100 > 20)
            y = x % 10;
        else y = x % 100;
        switch (y) {
            case 1:
                return "день";
            case 2:
            case 3:
            case 4:
                return "дня";
            default:
                return "дней";
        }
    }

    String getYearFormatted(int x) {
        int y;
        if (x % 100 > 20)
            y = x % 10;
        else y = x % 100;
        switch (y) {
            case 1:
                return "год";
            case 2:
            case 3:
            case 4:
                return "года";
            default:
                return "лет";
        }
    }

    void sendInfoAboutTask(Long chat, String query, String firstMsg, String emptyMsg, boolean useId) {
        try {
            ResultSet rs = QueryExecutor.getResultSet(query);
            boolean first = true;
            while (rs.next()) {
                if (first) {
                    sendMsg(chat, firstMsg);
                    first = false;
                }
                String id = rs.getString("ID");
                String task = rs.getString("TASK");
                String begin_time = rs.getString("BEGIN_TIME");
                String end_time = rs.getString("END_TIME");
                boolean done = rs.getBoolean("DONE");
                String executor = rs.getString("EXECUTOR");
                String text = task;
                text += "\nИсполнитель - "+executor;
                text += "\nВремя начала - "+begin_time;
                text += "\nВремя конца - "+end_time;
                text+="\nЗадача ";
                if(!done)
                    text+="не";
                text += "выполнена";
                if (useId) {
                    text += "\n№" + id;
                }
                sendMsg(chat, text);
            }
            if (first) {
                if (emptyMsg != null)
                    sendMsg(chat, emptyMsg);
            }
            QueryExecutor.releaseResources(rs);
        } catch (Exception e) {
            Log.error(e.getMessage());
            sendMsg(chat, e.getMessage());
        }
    }
    boolean sendInfoAboutTasks( String query, String firstMsg, boolean useId) {
       return sendInfoAboutTasks(query,firstMsg,useId,null);
    }
    boolean sendInfoAboutTasks( String query, String firstMsg, boolean useId,String colName) {
        try {
            ResultSet rs = QueryExecutor.getResultSet(query);
            while (rs.next()) {
                Long chat = rs.getLong("CHAT_ID");
                String id = rs.getString("ID");
                String task = rs.getString("TASK");
                String begin_time = rs.getString("BEGIN_TIME");
                begin_time = begin_time.substring(0,16);
                String end_time = rs.getString("END_TIME");
                end_time = end_time.substring(0,16);
                boolean done = rs.getBoolean("DONE");
                String executor = rs.getString("EXECUTOR");
                String text = firstMsg+task;
                text += "\nИсполнитель - "+executor;
                text += "\nВремя начала - "+begin_time;
                text += "\nВремя конца - "+end_time;
                text+="\nЗадача ";
                if(!done)
                    text+="не";
                text += "выполнена";
                if (useId) {
                    text += "\n№" + id;
                }
                sendMsg(chat, text);
                if(colName!=null)
                {
                    if(colName.equals("LAST_TIME_END")&&getStatusForChatID(chat)==Status.NORMAL)
                    {
                        updateStatus(chat,Status.REQUEST_DONE);
                        updateTaskIdAndTableForChatId(chat,id,ONE_TIME);
                        sendKeyBoard(chat,"Выполнили вы эту задачу?",YES_NO);
                    }
                    else if(!colName.equals("LAST_TIME_END"))
                    QueryExecutor.updateDataRecordForID(id,TABLES[ONE_TIME],colName,"current_timestamp",false);
                }
            }
            QueryExecutor.releaseResources(rs);
            return true;
        } catch (Exception e) {
            Log.error(e.getMessage());
            return false;
        }
    }
}
