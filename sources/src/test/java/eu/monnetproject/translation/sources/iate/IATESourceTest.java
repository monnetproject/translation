package eu.monnetproject.translation.sources.iate;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.monnetproject.lang.Language;
import eu.monnetproject.translation.PhraseTable;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TranslationSource;
import eu.monnetproject.translation.sources.common.ChunkImpl;
import eu.monnetproject.translation.sources.iate.IATESourceFactory;


public class IATESourceTest {


	public IATESourceTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testIATESource() {
		IATESourceFactory iateFactory = new IATESourceFactory();
//		TranslationSource iate = iateFactory.getSource(Language.ENGLISH, Language.SPANISH);
//		PhraseTable<PhraseTableEntry> candidates = iate.candidates(new ChunkImpl("cat"));		
	//	Iterator<PhraseTableEntry> iterator = candidates.iterator();	
//		iate.close();
//		assertEquals(true, iterator.hasNext());		
	}	

}


