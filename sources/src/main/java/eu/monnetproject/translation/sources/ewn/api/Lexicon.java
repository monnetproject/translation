
package eu.monnetproject.translation.sources.ewn.api;

import java.io.*;
import java.util.*;
import java.sql.*;

import eu.monnetproject.translation.monitor.Messages;



/**
 * An API for EuroWordNet as SQL database (single language).
 *
 * Conventions used here:<ul>
 * <li>"FO" in method names means first-order.</li>
 * <li>Methods accepting a string argument normally accept words,
 *     not ILIs.</li>
 * <li>Methods with names ending with "ByILI" accept Interlanguage
 *     Synset IDs instead of words.</li>
 * <li>Methods accepting ILIs and returning Maps usually return
 *     mappings from the result IDs to the result word forms,
 *     while other methods return mappings from the input word
 *     meanings to sets of words (which can have different synset IDs)
 *     </li>
 * </ul>
 *
 * @author bogdan, changes by Mauricio Espinoza
 * 
 */
public class Lexicon {

	protected Connection con;
	private String partOfSpeech = EuroWordnetAPI.POS_DEFAULT;

	/**
	 * Creates a new object of the type EuroWordNet
	 * @param configFile Name of the configuration file.
	 *                   The file must contain the database URL,
	 *                   the driver classname
	 *                   and username and password for the database
	 * @param language the language code
	 */
	public Lexicon(Map<String,String> ewnPaths, String language)
			throws 	Exception, ClassNotFoundException,
			FileNotFoundException, SecurityException,
			IOException {

		con = new ConnectionFactory(ewnPaths).getConnection(language);
		// ... there should be also some way to save the language in the
		// object -> for special cases like missing relations
		// e.g. hypernyms in Spanish!!
	}

	/**
	 * Creates a new object of the type Lexicon
	 * @param configFile name of the configuration file
	 * @param language the language code
	 * @param partOfSpeech the part-of-speech
	 */
	public Lexicon(Map<String,String> ewnPaths, String language, String partOfSpeech)
			throws Exception {

		this(ewnPaths, language);
		setPartOfSpeech(partOfSpeech);
	}

	// ... explain use of CONSTANT ...
	/**
	 * Set the part-of-speech for forthcoming lexical look-ups.
	 * If you do not want to restrict look-ups to one part-of-speech,
	 * then set it to "_".
	 * 
	 * @param partOfSpeech a value from the following set
	 * 			   {n, v, a} corresponding to
	 * 			   noun, verb, adjective
	 */
	public void setPartOfSpeech(String partOfSpeech) {
		this.partOfSpeech = partOfSpeech;
	}

	/**
	 * get part-of-speech
	 *
	 * @return The value of partOfSpeech
	 */
	public String getPartOfSpeech() {
		return partOfSpeech;
	}

	/**
	 * look up a word in the lexical resource (EWN)
	 * 
	 * @param word the word to be looked for
	 * @return a list of mappings between ili numbers
	 * 	       and synonyms pertaining to each reading of
	 * 	       the input word
	 */
	public Map<String, Set<String>> getSenses(String word) throws Exception {
		Hashtable<String, Set<String>> result = new Hashtable<String, Set<String>>();
		Set<String> ilis = getILIs(word);
		for(String it : ilis) {			
			Set<String> lexemes = getLexemesByILI(it);
			result.put(it, lexemes);			
		}
		return result;
	}


	/**
	 * look for the hypernyms (myWord IS-A X) of a given word
	 * <br>
	 * REMARK: 	a word reading (synset) can have more than one
	 * 		hypernym; this method puts them alltogether mapped
	 * 		to the same ili
	 * 
	 * @param word the word for which the hypernyms are looked for
	 * @param selfIncluded specify whether the word's synset should
	 * 			   be added to the return value
	 * @return a list of mappings between ili numbers
	 * 	       corresponding to each word reading and the
	 *         hypernyms of it (the synset itself is being
	 * 	       added when "selfIncluded" is true)
	 */
	public Map<String, Set<String>> getFOHypernyms(String word, boolean selfIncluded)
			throws Exception {
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		Set<String> concepts = getILIs(word);
		if (concepts.isEmpty())
			return result;
		for(String sense : concepts) {
			TreeSet<String> hypernyms = getHypernyms(sense, selfIncluded);
			if (!hypernyms.isEmpty()) {
				TreeSet<String> hypernymsLexicalForms = getLexemesByILI(hypernyms);
				result.put(sense, hypernymsLexicalForms);
			}			
		}		
		return result;
	}

	public Map<String, Set<String>> getFOHyponyms(String word) throws Exception {
		return getFOHyponyms(word, false);
	}

	public Map<String, Set<String>> getFOHypernyms(String word) throws Exception {
		return getFOHypernyms(word, false);
	}

	/**
	 * look for the hypernym ilis of a given word
	 * <p>
	 * REMARK: 	a word reading (synset) can have more than one
	 * 			hypernym; this method creates a mapping between
	 * 			ili and hypernym ilis
	 * 
	 * @param word the word for which the hypernym ilis
	 * 		   are looked for
	 * @return a list of mappings between ilis and their
	 * 	       corresponding hypernym ilis
	 */
	public Map<String, Set<String>> getFOHypernymILIs(String word) throws Exception {
		Hashtable<String, Set<String>> hypers = new Hashtable<String, Set<String>>();
		Set<String> concepts = getILIs(word);
		if (concepts.isEmpty())
			return hypers;
		for(String sense : concepts) {
			TreeSet<String> hypernyms = getHypernyms(sense, false);
			if (!hypernyms.isEmpty())
				hypers.put(sense, hypernyms);			
		}
		return hypers;
	} 

	/**
	 * Look for the hypernyms of a given word reading/ili (ILI).
	 * <p>
	 * REMARK: 	a word reading (synset) can have more than one
	 * 			hypernym; this method creates a mapping between
	 * 			hypernym ili and hypernym lexical representation
	 * 
	 * @param ili the synsets Interlingua Link ID (ILI)
	 * 		  which the hypernyms are to be looked for.
	 *            You have to make sure that it consists only of digits!
	 * @return a list of mappings between hypernym ilis and their
	 * 	       lexical representation
	 */
	public Map<String, Set<String>> getFOHypernymsByILI(String ili) throws Exception {
		Hashtable<String, Set<String>> hypers = new Hashtable<String, Set<String>>();
		Set<String> hypernyms = getHypernyms(ili, false);
		for(String hypernymIli : hypernyms) {
			TreeSet<String> hypernymLexicalForms = getLexemesByILI(hypernymIli);
			hypernymLexicalForms.addAll(getLexemesByILI(ili));
			hypers.put(hypernymIli, hypernymLexicalForms);		
		}
		return hypers;
	}


	public Map<String, Set<String>> getHypernymsByILI(String ili) throws Exception {
		Hashtable<String, Set<String>> hypers = new Hashtable<String, Set<String>>();
		Set<String> hypernyms = getHypernyms(ili, false);
		for(String hypernymIli : hypernyms) {
			TreeSet<String> hypernymLexicalForms = getLexemesByILI(hypernymIli);
			hypers.put(hypernymIli, hypernymLexicalForms);		
		}
		return hypers;
	}

	/**
	 * look for the hyponyms (X IS-A myWord) of a given word
	 * <p>
	 * REMARK: a word reading (synset) can have more than one
	 * 	     hyponym; this method puts them alltogether mapped
	 * 	     to the same ili
	 * 
	 * @param word the word for which the hyponyms are looked for
	 * @param selfIncluded specify whether the word's synset should
	 *                     be added to the return value
	 * @return a list of mappings between ili numbers
	 * 	     corresponding to each word reading and the
	 * 	     hyponyms of it (the synset itself is being
	 * 	     added when "selfIncluded" is true)
	 */
	public Map<String, Set<String>> getFOHyponyms(String word, boolean selfIncluded)
			throws Exception {
		Hashtable<String, Set<String>> result = new Hashtable<String, Set<String>>();
		for(String ili : getILIs(word)) {
			TreeSet<String> hyponymIlis = getHyponyms(ili, selfIncluded);
			if(!hyponymIlis.isEmpty()) {
				TreeSet<String> lexemes = getLexemesByILI(hyponymIlis);
				result.put(ili, lexemes);
			}
		}
		return result;
	}




	/**
	 * look for the hyponym ilis of a given word
	 * <p>
	 * REMARK: a word reading (synset) can have more than one
	 * 	       hyponym; this method creates a mapping between
	 * 	       ili and hyponym ilis
	 *  
	 * @param word the word for which the hyponym ilis
	 * 		   are looked for
	 * @return a list of mappings between ilis and their
	 * 	       corresponding hyponym ilis
	 * 	       of each reading/sense
	 */
	public Map<String, Set<String>> getFOHyponymILIs(String word) throws Exception {
		Hashtable<String, Set<String>> hypos = new Hashtable<String, Set<String>>();
		Set<String> conceptIlis = getILIs(word);
		if(conceptIlis.isEmpty())
			return hypos;
		for(String ili : conceptIlis) {
			TreeSet<String> hyponymIlis = getHyponyms(ili, false);
			if(!hyponymIlis.isEmpty()) {
				hypos.put(ili, hyponymIlis);
			}			
		}
		return hypos;
	}

	/**
	 * look for the hyponyms of a given word reading/ili
	 * <p>
	 * REMARK: a word reading (synset) can have more than one
	 * 	       hyponym; this method creates a mapping between
	 * 	       hyponym ili and hyponym lexical representation
	 * 
	 * @param ili the ili marking the intended reading for
	 * 		     which the hyponyms are to be looked for
	 * @return a list of mappings between hyponym ilis and their
	 * 	       lexical representation
	 */
	public Map<String, Set<String>> getFOHyponymsByILI(String ili) throws Exception {
		Hashtable<String, Set<String>> hypos = new Hashtable<String, Set<String>>();
		TreeSet<String> hyponyms = getHyponyms(ili, false);
		for(String hyponymIli : hyponyms) {
			TreeSet<String> hyponymLexicalForms = getLexemesByILI(hyponymIli);
			hyponymLexicalForms.addAll(getLexemesByILI(ili));
			hypos.put(hyponymIli, hyponymLexicalForms);	
		}
		return hypos;
	}


	public Map<String, Set<String>> getHyponymsByILI(String ili) throws Exception {
		Hashtable<String, Set<String>> hypos = new Hashtable<String, Set<String>>();
		TreeSet<String> hyponyms = getHyponyms(ili, false);
		for(String hyponymIli : hyponyms) {
			TreeSet<String> hyponymLexicalForms = getLexemesByILI(hyponymIli);
			hypos.put(hyponymIli, hyponymLexicalForms);			
		}
		return hypos;
	}


	public Map<String, Set<String>> getAdjectiveRelatedNounByILI(String ili) throws Exception {
		Hashtable<String, Set<String>> related = new Hashtable<String, Set<String>>();
		TreeSet<String> relatedness = getRelationByILI(ili, "near_synonym");
		for(String nounIli : relatedness) {
			TreeSet<String> relatedLexicalForms = getLexemesByILI(nounIli);
			relatedLexicalForms.addAll(getLexemesByILI(ili));
			related.put(nounIli, relatedLexicalForms);			
		}
		return related;
	}


	public Map<String, Set<String>> getMeronymsByILI(String ili) throws Exception {
		Hashtable<String, Set<String>> meronym = new Hashtable<String, Set<String>>();
		TreeSet<String> meronyms = getRelationByILI(ili, "has_mero_part");
		for(Iterator<String> it = meronyms.iterator(); it.hasNext(); ) {
			String meroIli = (String) it.next();
			TreeSet<String> meronymLexicalForms = getLexemesByILI(meroIli);
			meronymLexicalForms.addAll(getLexemesByILI(ili));
			meronym.put(meroIli, meronymLexicalForms);
		}
		return meronym;

	}


	public Map<String,Set<String>> getHolonymsByILI(String ili) throws Exception {
		Hashtable<String,Set<String>> holonym = new Hashtable<String,Set<String>>();
		TreeSet<String> holonyms = getRelationByILI(ili, "has_holo_part");
		for(Iterator<String> it = holonyms.iterator(); it.hasNext(); ) {
			String holoIli = (String) it.next();
			TreeSet<String> holonymLexicalForms = getLexemesByILI(holoIli);
			holonymLexicalForms.addAll(getLexemesByILI(ili));
			holonym.put(holoIli, holonymLexicalForms);
		}
		return holonym;

	}



	public Map<String, Set<String>> getFOCoordinates(String word) throws Exception {
		return getFOCoordinates(word, false);
	}


	/**
	 * get the coordinates of a given word,
	 * i.e. its siblings (words sharing the same ancestor
	 * with the given word, including the word itself and its synonyms);
	 * on demand, hypernyms are being added to the result
	 * 
	 * @param word the word for which the coordinates are looked for
	 * @param includeHypernyms	specify whether word's hypernyms are
	 * 				to be added to the result
	 * @return a list of mappings between word readings	and their siblings
	 */
	public Map<String, Set<String>> getFOCoordinates(String word, boolean includeHypernyms)
			throws Exception {    
		Hashtable<String, Set<String>> coords = new Hashtable<String, Set<String>>();
		Set<String> concepts = getILIs(word);
		if (concepts.isEmpty())
			return coords;
		Iterator<String> senses = concepts.iterator();
		while (senses.hasNext()) {
			String sense = (String) senses.next();
			TreeSet<String> hyponyms = new TreeSet<String>();
			TreeSet<String> hypernyms = getHypernyms(sense, false);
			Iterator<String> hyper = hypernyms.iterator();

			while (hyper.hasNext()) {

				String hypernymIli = (String) hyper.next();
				TreeSet<String> hypos = getHyponyms(hypernymIli,
						includeHypernyms);

				hyponyms.addAll(hypos);
			}
			TreeSet<String> hyponymsLexicalForms = getLexemesByILI(hyponyms);

			coords.put(sense, hyponymsLexicalForms);
		}

		return coords;
	}

	/**
	 * for a given word get its siblings, its hypernyms and
	 * its hyponyms
	 * 
	 * @param word the word for which the coordinates are looked for
	 * @return a list of mappings between word ilis and
	 *         their first order elements			
	 */
	public Map<String, Set<String>> getFOElements(String word) throws Exception {
		Hashtable<String, Set<String>> elements = new Hashtable<String, Set<String>>();
		Map<String, Set<String>> hypos = getFOHyponyms(word, true);
		Map<String, Set<String>> hypers = getFOHypernymILIs(word);

		if(hypers.isEmpty() && hypos.isEmpty())
			return elements;

		Set<String> hypoKeys = hypos.keySet();
		Set<String> hyperKeys = hypers.keySet();
		Set<String> keys = new HashSet<String>();

		keys.addAll(hypoKeys);
		keys.addAll(hyperKeys);

		for (Iterator<String> senses = keys.iterator(); senses.hasNext(); ) {
			String sense = (String) senses.next();
			TreeSet<String> terms = new TreeSet<String>();
			if(hypers.containsKey(sense)) { 
				Set<String> hyperIlis = hypers.get(sense);
				for(Iterator<String> it = hyperIlis.iterator();
						it.hasNext(); terms = new TreeSet<String>()) {

					String hyperIli = (String) it.next();
					terms.addAll(getLexemesByILI(hyperIli));
					if(hypos.containsKey(sense)) 
						terms.addAll(hypos.get(sense));
					elements.put(sense, terms);

				}
			} else {
				if(hypos.containsKey(sense)) 
					terms.addAll(hypos.get(sense));
				elements.put(sense, terms);
			}
		}
		return elements;
	}


	public Map<String, Set<String>> getFORelatives(String word) throws Exception {
		return getFORelatives(word, false);
	}

	/**
	 * for a given word get its hypernyms and its hyponyms
	 * 
	 * @param word the word for which the coordinates
	 *             are looked for
	 * @param selfIncluded specify whether the word's synset should
	 * 			   be added to the return value
	 * @return a list of mappings between word readings/ilis
	 * 	       and their relatives (hypernyms + hyponyms)
	 */
	public Map<String, Set<String>> getFORelatives(String word, boolean selfIncluded)
			throws Exception {

		Hashtable<String, Set<String>> relatives = new Hashtable<String, Set<String>>();

		Map<String, Set<String>> hypos = getFOHyponyms(word, false);
		Map<String, Set<String>> hypers = getFOHypernyms(word, selfIncluded);
		if(hypers.isEmpty() && hypos.isEmpty())
			return relatives;
		Set<String> hypoKeys = hypos.keySet();
		Set<String> hyperKeys = hypers.keySet();

		Set<String> keys = new HashSet<String>();
		keys.addAll(hypoKeys);
		keys.addAll(hyperKeys);

		Iterator<String> senses = keys.iterator();
		while(senses.hasNext()) {

			String sense = (String) senses.next();
			TreeSet<String> terms = new TreeSet<String>();
			if(hypos.containsKey(sense)) terms.addAll(hypos.get(sense));
			if(hypers.containsKey(sense)) terms.addAll(hypers.get(sense));
			relatives.put(sense, terms);
		}

		return relatives;
	}

	/** Get all synset definition for a word
	 *
	 *  @param word the word to be looked for
	 *  @return a set of mappings between ILIs (for the different word meanings)     *          and definitions
	 */
	public Map<String, String> getGlosses(String word) throws Exception {
		Hashtable<String, String> result = new Hashtable<String, String>();
		Set<String> ilis = getILIs(word);
		Iterator<String> it = ilis.iterator();
		while (it.hasNext()) {
			String i = (String) it.next();
			String gloss = getGlossByILI(i);
			result.put(i, gloss);
		}
		return result;
	}

	/** Get definition for a given synset.
	 *
	 * @param ili the synset's ILI - this String should only contain digits!
	 * @return a String containing the gloss, an empty string if there is no
	 *         gloss in the database
	 */
	public String getGlossByILI(String ili) throws Exception {
		String result = "";
		Statement stmt = con.createStatement();

		StringBuffer query = new StringBuffer();
		query.append("SELECT Gloss FROM synset ");
		query.append("WHERE Pos LIKE '" + partOfSpeech + "' AND Offset = ");
		query.append(ili);

		ResultSet rs = stmt.executeQuery(query.toString());
		if (rs.next()) {
			result = rs.getString(1);
		}

		return result.trim();
		// if you want to optimize this by removing the trim() operation,
		// it may be better to assure that there are correctly stripped
		// glosses in the data base.
	}


	/** Get PartOfSpeech for a given synset.
	 *
	 * @param ili the synset's ILI - this String should only contain digits!
	 * @return a String containing the part of speech, an empty string if there is no
	 *         in the database
	 */

	public String getPartOfSpeechByILI(String ili) throws Exception {
		String result = "";
		Statement stmt = con.createStatement();

		StringBuffer query = new StringBuffer();
		query.append("SELECT Pos FROM synset ");
		query.append("WHERE Offset = ");
		query.append(ili);

		ResultSet rs = stmt.executeQuery(query.toString());
		if (rs.next()) {
			result = rs.getString(1);
		}

		return result.trim();


	}


	/** 
	 * Look up the Interlingua IDs (ILIs, ilis) of a word.
	 * @param word the word to be looked for
	 * @return a set of ILIs that represent meanings of the given word
	 */
	public Set<String> getILIs(String word) throws Exception {
		TreeSet<String> result = new TreeSet<String>();
		try {
			Statement stmt = con.createStatement();
			StringBuffer query = new StringBuffer();
			query.append("SELECT Offset FROM synsetword ");
			query.append("WHERE Word = '");
			query.append(escapeSQL(new String (word.getBytes("UNICODE"),"UTF-16"))); 
			query.append("' AND Pos LIKE '" + partOfSpeech + "'");
			ResultSet rs = stmt.executeQuery(query.toString());			
			while (rs.next()) 
				result.add(rs.getString(1));
		} catch(SQLException ex) {
			Messages.warning("Error in querying (SQL) EuroWordNet");
		//	throw ex;	
		}
		return result;
	}

	/**
	 * get the hypernym ilis for a given word ili
	 * 
	 * @param conceptIli the ili to be looked for
	 * @param selfIncluded specify whether the ili itself
	 *                     shall be added to the result
	 * @return a list of hypernym ilis (+ the ili
	 * 	       itself on demand)
	 */
	private TreeSet<String> getHypernyms(String conceptIli, boolean selfIncluded) {
		TreeSet<String> result = new TreeSet<String>();
		try {
			Statement stmt = con.createStatement();
			StringBuffer query = new StringBuffer();
			query.append("SELECT TargetOffset FROM synsetptr ");
			query.append("WHERE Ptr = '@' AND TargetPos LIKE '" +
					partOfSpeech + "' and SourceOffset = ");
			query.append(conceptIli);
			ResultSet rs = stmt.executeQuery(query.toString());
			while (rs.next()) 
				result.add(rs.getString(1));
		} catch(SQLException ex) {
			ex.printStackTrace();	
		}
		if (selfIncluded && !result.isEmpty()) 
			result.add(conceptIli);
		return result;
	}

	/**
	 * Get the hyponym ilis for a given ILI.
	 * 
	 * @param conceptOffset the ili to be looked for
	 * @param selfIncluded specify whether the ili itself
	 * 			 shall be added to the result
	 * @return a set of hyponym ilis (+ the ili itself on demand)
	 */
	private TreeSet<String> getHyponyms(String conceptOffset, boolean selfIncluded)
			throws Exception {
		TreeSet<String> result = new TreeSet<String>();
		Statement stmt = con.createStatement();
		StringBuffer query = new StringBuffer();
		query.append("SELECT b.Offset FROM synsetptr a, synsetword b ");
		query.append("WHERE a.TargetOffset = b.Offset AND a.Ptr = '~' ");
		query.append("AND b.Pos LIKE '" + partOfSpeech +
				"' AND a.SourceOffset = ");
		query.append(conceptOffset);
		ResultSet rs = stmt.executeQuery(query.toString());
		while(rs.next()) {
			result.add(rs.getString(1));
		}
		if(selfIncluded) {
			result.add(conceptOffset);
		}
		return result;
	}




	/**
	 * get lexical representations for a list of given ilis
	 * 
	 * @param ilis a list of ilis
	 * @return a list of lexical representations
	 */
	public TreeSet<String> getLexemesByILI(Set<String> ilis) throws Exception {
		TreeSet<String> result = new TreeSet<String>();
		for(String it : ilis) 
			result.addAll(getLexemesByILI((it)));					
		return result;
	}


	/**
	 * Get lexical representation of a synset specified by its
	 * inter-lingual index.
	 * 
	 * @param ili the inter-lingual index
	 * @return a set of Strings of all words/entries that represent the given
	 *         ILI
	 */
	public TreeSet<String> getLexemesByILI(String ili) throws Exception {

		TreeSet<String> result = new TreeSet<String>();

		Statement stmt = con.createStatement();

		StringBuffer query = new StringBuffer();
		query.append("SELECT Word FROM synsetword ");
		query.append("WHERE Pos LIKE '" + partOfSpeech + "' " +
				"AND Suffix IS NULL " +
				"AND Prefix IS NULL " +
				"AND Offset = ");
		query.append(ili);

		ResultSet rs = stmt.executeQuery(query.toString());
		while(rs.next()) {
			String s = rs.getString(1);
			if(!s.endsWith("In")) {
				result.add (s);
			} else {
				result.add (s);
			}
		}	
		return result;
	}


	/** 
	 * Queries an relation in the wordnet.
	 * Read the EuroWordNet manual in order to find out what relations exist.
	 *
	 * @param word a word
	 * @param relation the EWN name of the relation
	 * @return a map with the word's different ILIs as keys and sets of the
	 *         relation targets as values.
	 */
	public Map<String, Set<String>> getRelation(String word, String relation) throws Exception {

		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		Iterator<String> ilis = getILIs(word).iterator();

		String i;

		while (ilis.hasNext()) {
			i = (String) ilis.next();
			result.put(i, getRelationByILI(i, relation));
		}

		return result;
	}

	/**
	 * Queries any relation in the wordnet.
	 * When using this query, make sure you have set the correct part of speech
	 * for the results that you expect.
	 * If you are not sure, it is better to call setPartOfSpeech(POS_ANY) before.
	 * Read the EuroWordNet manual in order to find out what relations exist.
	 *
	 * @param ili the concept ILI
	 * @param relation the EuroWordNet name of the relation
	 * @return a set of ILIs where the relation points to from the given ili
	 */
	public TreeSet<String> getRelationByILI(String ili, String relation)
			throws Exception {

		return getRelationByILI(ili, relation, true);
	}



	/**
	 * Queries any relation in the wordnet.
	 * When using this query, make sure you have set the correct part of speech
	 * for the results that you expect.
	 * If you are not sure, it is better to call setPartOfSpeech(POS_ANY) before.
	 * Read the EuroWordNet manual in order to find out what relations exist.
	 * 
	 * @param ili the concept ili
	 * @param relation the name of the relation, either the EWN or the WordNet
	 *                 name.
	 * @param ewnRelation determines whether an EWN (true) or a WordNet (false)
	 *                    relation name is used.
	 */
	public TreeSet<String> getRelationByILI(String ili,
			String relation,
			boolean ewnRelation)
					throws Exception {

		TreeSet<String> result = new TreeSet<String>();
		StringBuffer query = new StringBuffer();

		try {
			Statement stmt = con.createStatement();

			query.append("SELECT TargetOffset FROM synsetptr s, pointer p ");

			query.append("WHERE p.ptr=s.ptr AND ");
			query.append( (ewnRelation?"p.txt='":"p.Description='wn: ") +
					escapeSQL(relation) + "' AND ");
			query.append("s.TargetPos LIKE '" + partOfSpeech + "' AND " +
					"s.SourceOffset=" + ili);

			ResultSet rs = stmt.executeQuery(query.toString());
			while(rs.next()) {
				result.add( rs.getString(1) );
			}		    
		} catch(SQLException e) {
			log(query.toString());
			throw e;
		}

		return result;
	}


	/**
	 * escape a special SQL character (') within a given string
	 * 
	 * @param input the string to be escaped
	 * @return the escaped string
	 */
	private String escapeSQL(String input) {

		if(input == null) 
			return null;

		StringTokenizer st = new StringTokenizer(input, "'");
		StringBuffer buffer = new StringBuffer();

		if(st.countTokens() == 1) 
			return st.nextToken();
		buffer.append(st.nextToken());
		while(st.hasMoreTokens()){
			buffer.append("\\" + "'" + st.nextToken());
		}
		return buffer.toString();
	}  

	private void log(String msg) {
		System.out.println(msg);
	}
}
