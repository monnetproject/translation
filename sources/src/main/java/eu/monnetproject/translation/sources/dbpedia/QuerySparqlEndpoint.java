package eu.monnetproject.translation.sources.dbpedia;

import java.util.List;
import java.util.Vector;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
//import com.hp.hpl.jena.vocabulary.ResultSet;

public class QuerySparqlEndpoint {	
	 
	public QuerySparqlEndpoint() {}  
	  
	/**
	 * Execute a query to a sparql service. It retrieves "unidimensional" results only.
	 * @param sparqlService url of the sparql endpoint
	 * @param queryString sparql query
	 * @return vector of RDF nodes
	 */
//	public synchronized static Vector<RDFNode> executeQuery(String sparqlService, String queryString) {
	public static List<RDFNode> executeQuery(String sparqlService, String queryString) {	 
		  //List result to query
	  	Vector<RDFNode> list = new Vector<RDFNode>(0,1);	  	
	  	try {	  		
	  		//Create query	  		
		  	Query query = 	QueryFactory.create(queryString);
			// Execute the query and obtain results
			QueryExecution qe = QueryExecutionFactory.sparqlService(sparqlService, query);			  	
			//Obtain the results
			ResultSet results = qe.execSelect();			  			  	
			//Review results
			for ( ; results.hasNext() ; )    {  		
				QuerySolution soln = results.nextSolution() ;
				RDFNode x = soln.get("x") ;
				list.add(x);
			}  		
			//Important - free up resources used running the query
		  	qe.close();	 			  	
  		} catch (Exception ex) {
  			System.out.println("Problems querying sparql endpoint: " + ex.getMessage() + " with "+ queryString);
  		}	  	
	  	return list;
	  }
				
	/**
	 * Execute a query to a sparql service. It can retrieve more than one variable in a row (maximum 10).
	 * @param sparqlService url of the sparql endpoint
	 * @param queryString sparql query
	 * @return vector of RDF nodes
	 */
	public synchronized static Vector<RDFNode[]> executeMultidimensionalQuery(String sparqlService, String queryString) {
	 
		  //List result to query
	  	Vector<RDFNode[]> list = new Vector<RDFNode[]>(0,1);
	  	        
	  	try {
	  		
	  		//Create query
		  	Query query = QueryFactory.create(queryString);
			
		  	// Execute the query and obtain results
			QueryExecution qe = QueryExecutionFactory.sparqlService(sparqlService, query);
			ResultSet results = qe.execSelect() ;
			
			// Array with the variables in the query (typically "x", "y", etc...)
			Object[] resultVariables = results.getResultVars().toArray();
			
			//Review results
			for ( ; results.hasNext() ; )    {  		
			  	
				RDFNode[] row = new RDFNode[resultVariables.length];
				
				QuerySolution soln = results.nextSolution() ;
				for (int i=0; i<resultVariables.length; i++ ){
					
					RDFNode x = soln.get(resultVariables[i].toString()) ;
					row[i]= x;
						
				}
				
				list.add(row);
				
			}  
		
			//Important - free up resources used running the query
		  	qe.close();	 
			  	
  		} catch (Exception ex) {
  			System.out.println("Problems querying sparql endpoint: " + sparqlService + " " + ex.getMessage() + " with "+ queryString);
  		}
	  	
	  	return list;
	  }
				
	

}
