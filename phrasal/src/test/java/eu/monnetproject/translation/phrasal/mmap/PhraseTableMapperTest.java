/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.monnetproject.translation.phrasal.mmap;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jmccrae
 */
public class PhraseTableMapperTest {

    public PhraseTableMapperTest() {
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
    String testData = "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "that ||| is also a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this ||| is a test\n"
            + "this is ||| unter ||| 8.84408e-05 1.22797e-05 6.27372e-05 2.72e-05 2.718 ||| ||| 22614 31879\n"
            + "this is ||| unternehmen angestellt sind , ist das ||| 1 0.0744504 3.13686e-05 4.39577e-13 2.718 ||| ||| 1 31879\n"
            + "this is ||| unternehmen jetzt ||| 0.2 0.000554959 3.13686e-05 6.16338e-07 2.718 ||| ||| 5 31879\n"
            + "this is ||| unterwirft sie sich ||| 0.333333 0.000915664 3.13686e-05 1.9222e-10 2.718 ||| ||| 3 31879\n"
            + "this is ||| unverzichtbare voraussetzung dafür sind ||| 1 0.00375805 3.13686e-05 3.42874e-15 2.718 ||| ||| 1 31879\n"
            + "this is ||| ursache dafür ist ||| 0.2 0.0492919 3.13686e-05 7.28509e-08 2.718 ||| ||| 5 31879\n"
            + "this is ||| ursache hierfür ist ||| 0.2 0.164573 3.13686e-05 1.41434e-08 2.718 ||| ||| 5 31879\n"
            + "this is ||| verboten ist ||| 0.0103093 0.00391511 3.13686e-05 1.04271e-05 2.718 ||| ||| 97 31879\n"
            + "this is ||| verbunden sind ||| 0.00531915 0.000541733 3.13686e-05 4.71506e-06 2.718 ||| ||| 188 31879\n"
            + "this is ||| verfolgen wir die ||| 0.047619 3.37486e-05 3.13686e-05 8.39995e-09 2.718 ||| ||| 21 31879\n"
            + "this is ||| verfährt das ||| 0.5 0.0185108 3.13686e-05 1.87848e-06 2.718 ||| ||| 2 31879\n"
            + "this is ||| verfügung steht ||| 0.00813008 0.00222827 3.13686e-05 3.28027e-06 2.718 ||| ||| 123 31879\n"
            + "this is ||| verhält es sich ||| 0.028169 0.00667203 6.27372e-05 8.37584e-08 2.718 ||| ||| 71 31879\n"
            + "this is ||| verhält es ||| 0.0172414 0.00667203 3.13686e-05 3.97761e-06 2.718 ||| ||| 58 31879\n"
            + "this is ||| verlangt reife , über die wir ||| 1 0.000190969 3.13686e-05 1.68471e-17 2.718 ||| ||| 1 31879\n"
            + "this is ||| verlaufen die dinge ||| 0.5 0.000198587 3.13686e-05 7.31593e-11 2.718 ||| ||| 2 31879\n"
            + "this is ||| verlaufen die ||| 0.0909091 0.000198587 3.13686e-05 4.90672e-07 2.718 ||| ||| 11 31879\n"
            + "this is ||| vermeiden sollte , ||| 0.25 0.000181143 3.13686e-05 2.02013e-08 2.718 ||| ||| 4 31879\n"
            + "this is ||| vermeiden sollte ||| 0.2 0.000181143 3.13686e-05 1.11762e-07 2.718 ||| ||| 5 31879\n"
            + "this is ||| vermutlich ist ||| 0.0909091 0.0017466 3.13686e-05 2.1875e-06 2.718 ||| ||| 11 31879\n"
            + "this is ||| verspricht man sich ||| 0.5 0.000539809 3.13686e-05 1.26359e-09 2.718 ||| ||| 2 31879\n"
            + "this is ||| verspricht man ||| 0.166667 0.000539809 3.13686e-05 6.00065e-08 2.718 ||| ||| 6 31879\n"
            + "this is ||| verspricht ||| 0.00617284 0.000539809 3.13686e-05 3.53e-05 2.718 ||| ||| 162 31879\n"
            + "this is ||| versuchen , dies ||| 0.111111 0.0172255 3.13686e-05 1.0427e-06 2.718 ||| ||| 9 31879\n"
            + "this is ||| vertritt diese ||| 0.333333 0.0502104 3.13686e-05 3.41798e-05 2.718 ||| ||| 3 31879\n"
            + "this is ||| vertritt ||| 0.00312989 0.000203245 6.27372e-05 0.000147 2.718 ||| ||| 639 31879\n"
            + "this is ||| viele dieser ||| 0.00332226 0.019318 3.13686e-05 1.66356e-05 2.718 ||| ||| 301 31879\n"
            + "this is ||| vielmehr ist dies ||| 0.2 0.335 3.13686e-05 3.57246e-06 2.718 ||| ||| 5 31879\n"
            + "this is ||| vielmehr wollen wir an sie ||| 1 4.43213e-05 3.13686e-05 1.04745e-14 2.718 ||| ||| 1 31879\n"
            + "this is ||| vielmehr wollen wir an ||| 1 4.43213e-05 3.13686e-05 1.42786e-12 2.718 ||| ||| 1 31879\n"
            + "this is ||| vielmehr wollen wir ||| 0.333333 4.43213e-05 3.13686e-05 2.81478e-10 2.718 ||| ||| 3 31879\n"
            + "this is ||| vielmehr ||| 0.000884956 7.85611e-06 6.27372e-05 4.39e-05 2.718 ||| ||| 2260 31879\n";

    /**
     * Test of writePhraseTableMap method, of class PhraseTableMapper.
     */
    @Test
    public void testWritePhraseTableMap_4args() throws Exception {
        System.out.println("writePhraseTableMap");
        System.out.println(testData.getBytes().length);
        File phraseTableFile = File.createTempFile("phrase", ".table");
        phraseTableFile.deleteOnExit();
        final FileWriter fw = new FileWriter(phraseTableFile);
        fw.write(testData);
        fw.close();
        File mapFile1 = File.createTempFile("map", ".tmp");
        mapFile1.deleteOnExit();
        Charset encoding = Charset.forName("UTF-8");
        PhraseTableMapper instance = new PhraseTableMapper();
        instance.mapPhraseTable(phraseTableFile, mapFile1);
    }
}
