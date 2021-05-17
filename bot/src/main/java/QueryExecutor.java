

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class QueryExecutor {

    public static Connection getConnection() {
        try {
            Properties connInfo = new Properties();
            connInfo.put("user", BotSettings.RDB_USERNAME);
            connInfo.put("password", BotSettings.RDB_PASSWORD);
            connInfo.put("charSet", "utf8");
            return DriverManager.getConnection(BotSettings.URL_CONNECTION, connInfo);
        } catch (Exception e) {
            Log.error(e.getMessage());
            return null;
        }
    }

    public static Statement getStatement() {
        try {
            return getConnection().createStatement();
        } catch (Exception e) {
            Log.error(e.getMessage());
            return null;
        }
    }

    public static ResultSet getResultSet(String query) {
        Log.add("Executing:" + query);
        try {
            return getStatement().executeQuery(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
            return null;
        }
    }

    public static void executeUpdate(String query) {
        Log.add("Executing:" + query);
        Statement st = null;
        try {
            st = getStatement();
            st.executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            releaseResources(st);
        }
    }

    public static PreparedStatement getPreparedStatement(String sql) {
        try {
            return getConnection().prepareStatement(sql);
        } catch (Exception e) {
            Log.error(e.getMessage());
            return null;
        }
    }


    public static void executeUpdate(String query, List<Object> params) {
        Log.add("Executing:" + query);
        PreparedStatement st = null;
        try {
            st = getPreparedStatement(query);
            for (int i = 0; i < params.size(); i++) {
                st.setObject(i + 1, params.get(i));
            }
            st.executeUpdate();
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            releaseResources(st);
        }
    }
    public static void releaseResources(Statement st) {
        try {
            if(st!=null) {
                Connection con = st.getConnection();
                con.close();
            }
        } catch (SQLException e) {
            Log.error(e.getMessage());
        }
    }

    public static void releaseResources(ResultSet rs) {
        try {
            if(rs!=null) {
                Statement st = rs.getStatement();
                releaseResources(st);
            }
        } catch (SQLException e) {
            Log.error(e.getMessage());
        }
    }

    public static String buildQueryForInsert(String tableName,int countParameters)
    {
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(tableName);
        query.append(" VALUES(");
        for (int i=0;i<countParameters;i++) {
            if (i != 0)
                query.append(",");
            query.append(" ?");
        }
        query.append(")");
        return query.toString();
    }

    public static String getNextValueFromGenerator(String nameGenerator)
    {
        ResultSet rs = null;
        try {
            String query = "SELECT GEN_ID (" + nameGenerator + ",1) FROM RDB$DATABASE";
            rs = QueryExecutor.getResultSet(query);
            if (rs.next())
                return rs.getString(1);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            QueryExecutor.releaseResources(rs);
        }
        return null;
    }

    public static String getNextIdForTask(String tableName)
    {
        return getNextValueFromGenerator("SEQ_"+tableName+"_ID");
    }

    public static void updateDataRecordForID(String id, String table, String field, String data,boolean useQuotes) {
        updateDataRecordForOneField("id",id,table,field,data,useQuotes);
    }
    public static void updateDataRecordForIDByParam(String id, String table, String field, Object data) {
        updateDataRecordForOneFieldByParam("id",id,table,field,data);
    }

    public static void updateDataRecordForOneFieldByParam(String keyField,String valueForKeyField, String table, String field, Object data) {
        try {
            String query = "UPDATE "+table+"\n" +
                    "SET " + field + " = ?\n" +
                    "where "+keyField+" = " + valueForKeyField;
            List<Object> params = new ArrayList<>();
            params.add(data);
            executeUpdate(query,params);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }


    public static void updateDataRecordForOneField(String keyField,String valueForKeyField, String table, String field, String data,boolean useQuotes) {
        try {
            String query = "UPDATE "+table+"\n" +
                    "SET " + field + " = ";
            if(useQuotes)
                query+="'";
            query+= data;
            if(useQuotes)
                query+="'";
            query+="\nwhere "+keyField+" =  " + valueForKeyField;
            executeUpdate(query);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    public static String buildQueryForAllRecords(String table)
    {
       return  "select * from "+table;
    }
    public static String buildQueryForAllRecordsWithCondition(String table,String condition)
    {
        return  buildQueryForAllRecords(table)+" where " + condition;
    }

    public static ResultSet getAllRecordsFromTable(String table)
    {
        return getResultSet(buildQueryForAllRecords(table));
    }

    public static ResultSet getAllRecordsFromTableWithCondition(String table,String condition)
    {
        String query = buildQueryForAllRecordsWithCondition(table,condition);
        return getResultSet(query);
    }

    public static String getFirstValueFromResultSet(ResultSet rs,String colName)
    {
        try {
            if(rs.next())
                return rs.getString(colName);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            releaseResources(rs);
        }
        return null;
    }

}
