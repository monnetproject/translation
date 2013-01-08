package eu.monnetproject.translation.sources.common;

import java.util.Properties;


public class Parameters {

	//private static Properties properties;
	
	//-------------------------------------
	// OTHER USER ONTOLOGY POOLS
	//-------------------------------------

	public static String URL;
	
	public static String LocalOntologiesFile;
	public static String RemoteOntologiesFile;
	public static String RemoteSPARQLEndpointsFile;
	//-------------------------------------
	// DISCOVERING
	//-------------------------------------
	
	// Maximum number of searched ontologies per source (it only works on Watson, at the moment) 
    public static int MaxNumOntologies;
    
    // Maximum number of handled senses per keyword and ontology
    public static int MaxNumSensesPerSource;
    
    //Time to disconnect ontology reading (sg)
    public static int TimeOntDisconn;
     
    //Type of discovered Ontologycal Terms (it only works on Watson, at the moment)
    public static int TypeOfTerms;
    
    //Default language (it only works on SPQRQLEndpoints for the moment; for the rest, searches are done no matter the language)
    public static String DefaultLanguage;
    
    
    // -----------------------------------------------------------------------------
    // EXTRACTION
    // -----------------------------------------------------------------------------

    // Depth of ontological context extraction
    public static int Depth;

    // Maximum number of Hypernyms, Hyponyms, etc. to extract 
    public static int MaxNumHypernyms; //5
    public static int MaxNumHyponyms; //5
    public static int MaxNumRoles; //10
    public static int MaxNumDomains;
    public static int MaxNumRanges;

    // -----------------------------------------------------------------------------
	// REASONING in DISCOVERING and EXTRACTION (it does not work in WordNet)
    // -----------------------------------------------------------------------------
    public static int InferenceLevel;

    /*static{
		//try{
			 LocalOntologiesFile = properties.getProperty("LocalOntologiesFile", "");
			 RemoteOntologiesFile = properties.getProperty("RemoteOntologiesFile");
			 //RemoteSPARQLEndpointsFile = ConfigurationParametersManager.getParameter("RemoteSPARQLEndpointsFile");
			 MaxNumOntologies = Integer.parseInt(properties.getProperty("MaxNumOntologies"));
			 MaxNumSensesPerSource = Integer.parseInt(properties.getProperty("MaxNumSensesPerSource"));
			 Depth = Integer.parseInt(properties.getProperty("Depth"));
			 TimeOntDisconn = Integer.parseInt(properties.getProperty("TimeOntDisconn"));
			 TypeOfTerms = Integer.parseInt(properties.getProperty("TypeOfTerms"));
			 MaxNumHypernyms = Integer.parseInt(properties.getProperty("MaxNumHypernyms"));
			 MaxNumHyponyms = Integer.parseInt(properties.getProperty("MaxNumHyponyms"));
			 MaxNumRoles = Integer.parseInt(properties.getProperty("MaxNumRoles"));
			 MaxNumDomains = Integer.parseInt(properties.getProperty("MaxNumDomains"));
			 MaxNumRanges = Integer.parseInt(properties.getProperty("MaxNumRanges"));
			 InferenceLevel  = Integer.parseInt(properties.getProperty("InferenceLevel"));
			 
		   //}catch (Exception e){         
	      //      System.out.println("*** ERROR handling configuration parameters file: missing parameter  ***");	      
		   //}		
    }*/
    
    public static void setParameters(Properties properties) {
    	
		 LocalOntologiesFile = properties.getProperty("LocalOntologiesFile", "");
		 RemoteOntologiesFile = properties.getProperty("RemoteOntologiesFile", "");
		 RemoteSPARQLEndpointsFile = properties.getProperty("RemoteSPARQLEndpointsFile", "");
		 MaxNumOntologies = Integer.parseInt(properties.getProperty("MaxNumOntologies", "40"));
		 MaxNumSensesPerSource = Integer.parseInt(properties.getProperty("MaxNumSensesPerSource","5"));
		 Depth = Integer.parseInt(properties.getProperty("Depth","2"));
		 TimeOntDisconn = Integer.parseInt(properties.getProperty("TimeOntDisconn","120"));
		 TypeOfTerms = Integer.parseInt(properties.getProperty("TypeOfTerms", "3"));
		 MaxNumHypernyms = Integer.parseInt(properties.getProperty("MaxNumHypernyms", "5"));
		 MaxNumHyponyms = Integer.parseInt(properties.getProperty("MaxNumHyponyms", "5"));
		 MaxNumRoles = Integer.parseInt(properties.getProperty("MaxNumRoles", "10"));
		 MaxNumDomains = Integer.parseInt(properties.getProperty("MaxNumDomains", "5"));
		 MaxNumRanges = Integer.parseInt(properties.getProperty("MaxNumRanges", "5"));
		 InferenceLevel  = Integer.parseInt(properties.getProperty("InferenceLevel", "2"));
		 
		 DefaultLanguage = properties.getProperty("DefaultLanguage", "en");
    	
		 URL = properties.getProperty("URL");
    }
    
}
