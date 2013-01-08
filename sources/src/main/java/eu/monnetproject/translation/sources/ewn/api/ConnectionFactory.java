package eu.monnetproject.translation.sources.ewn.api;



import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;



/**
 *  This class is used to establish connections between the API and the
 *  database system that contains the EWN data.
 *  The getConnection() method returns Connection objects that can be used
 *  to query a dictionary in one language.
 *
 * @author Zeno Gantner modified by Mauricio Espinoza
 * 
 */
public class ConnectionFactory {
  
		
    /** The path and filename of the configuration file */
    private final Map<String,String> ewnPaths;

    /**
     * Creates an object and loads the configuration data from disk.
     *  @param configFile the path and filename of the configuration file
     */
    public ConnectionFactory(Map<String,String> ewnPaths) {
    	this.ewnPaths = ewnPaths;
    }
  
    /** 
     * Establish database connection
     * @param language the two-letter code for the language that
     *                 should be connected
     * @return the java.sql.Connection object that represents the database
     *         connection
     */
    public Connection getConnection(String language)
	throws SQLException, ClassNotFoundException,
	       FileNotFoundException, SecurityException,
	       IOException {
	
    	Connection conn = null;
    	
        try {
        	
        	//get path of EWN databases
        	String path = getPathEWNDatabases(language);
        	
            //Class.forName(sqlDriverName).newInstance();            
        	Class.forName("org.sqlite.JDBC").newInstance();
        	
            //	get ewn database in the specific language
            String databaseURL = "jdbc:sqlite:" + path ;//+ "ewn_" + language + ".db";
            conn = DriverManager.getConnection(databaseURL);          
            
        } catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
        return conn;
    }
    
    //get the path where are installed the ewn databases  
    private String getPathEWNDatabases (String language) {
    	return ewnPaths.get(language);//System.getProperty("ewn_path");
    }
    
}
