package eu.monnetproject.nlp.stl.impl;
import eu.monnetproject.nlp.stl.Termbase;
import java.util.Iterator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import org.apache.commons.dbutils.ResultSetIterator;
/**
 * A SQL termbase implementation to lookup terms
 * 
 * @author Tobias Wunner
 */
public class SQLTermbase implements Termbase, Iterable<String> {
    Connection conn = null;
    private Iterator<Object[]> it;
    String language = "en";
    String columnName = null;
    int size = 0;
    public SQLTermbase(Connection conn, String tableName, String columnName, String language) {
      this.language = language;
      //System.out.println("constructor sql termbase lang "+this.language);
      this.conn = conn;
      this.columnName = columnName;
      try {
        // query mysql table size
        String select0 = "count(distinct "+columnName+")";
        this.size = new Integer(querydbforvalue(conn.prepareStatement("select "+select0+" from " + tableName),select0).toString());
        // set iterator on mysql rowdata
        ResultSet rs = querydb(conn.prepareStatement("select distinct "+columnName+" from " + tableName));
        
        it = new ResultSetIterator(rs);//ResultSetIterator.iterable(rs).iterator(); 
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
    private Object querydbforvalue(PreparedStatement statement,String param) throws SQLException{
      ResultSet rs = querydb(statement);
      rs.next();
      return rs.getString(param);
    }
    private ResultSet querydb(PreparedStatement statement) {
      ResultSet rs = null;
      try {
        rs = statement.executeQuery();
        return rs;
      } catch (SQLException x) {
        throw new RuntimeException(x);
      } finally {
        if(rs != null) {
          //try {
          //  rs.close();
          //} catch(Exception x) {
          //  x.printStackTrace();
          //}
        }
      }
    }
    @Override
    public boolean lookup(String term) {
      ResultSet rs = null;
      try {
        PreparedStatement lookup_statement = conn.prepareStatement("select "+columnName+" from test_de where "+columnName+"=\""+term+"\"");
        rs = lookup_statement.executeQuery();
        while (rs.next())
          return true;
      } catch (SQLException x) {
        throw new RuntimeException(x);
      } finally {
        if(rs != null) {
          try {
            rs.close();
          } catch(Exception x) {
            x.printStackTrace();
          }
        }
      }
      return false;
    }
    @Override
    public Iterator<String> iterator() {
      return new Iterator<String>() {
        public boolean hasNext() { return it.hasNext(); } 
        public String next() { Object[] rowdata = it.next(); String firstColumn = (String)rowdata[0]; return firstColumn.toLowerCase().substring(0,firstColumn.length()-1); }
        public void remove() { }
      };
    }
    @Override
    public String getLanguage() {
      return this.language;
    }
    @Override
    public int size() {
      return size;
    }
    public static void main(String[] args) {
      //String dbname = "test";
      //String user = "monnet";
      //String pass = "translation";
      Connection conn = null;
      try {
        conn = DriverManager.getConnection("jdbc:mysql://localhost/test?user=monnet&password=translation");
      } catch (SQLException x) {
        System.err.println("Can't connect to mysql db "+x);
        return;
      }
      SQLTermbase sqltermbase = new SQLTermbase(conn,"test_de","forin","en");
      System.out.println("termbase size "+sqltermbase.size());
      System.out.println("lookup familien "+sqltermbase.lookup("familien"));
      System.out.println("lookup familiensteuer "+sqltermbase.lookup("familiensteuer"));
      for(String term:sqltermbase) 
        System.out.println(term+" -> "+sqltermbase.lookup(term));
    }
}
