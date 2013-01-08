package eu.monnetproject.translation.langmodel;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author john
 */
public class DirectDataMMapBuilderTest {

    public DirectDataMMapBuilderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of accept method, of class DirectDataMMapBuilder.
     */
    @Test
    public void testWrite() throws Exception {
        System.out.println("accept");
        final File folder = new File("testFolder");
        
        final DirectDataMMapBuilder builder = new DirectDataMMapBuilder(folder);
        builder.accept(new int[] { 1,1 }, new double[] { 1,2 }); // hash = 1
        builder.accept(new int[] { 1,2 }, new double[] { 1,2 }); // hash = 2
        builder.accept(new int[] { 2,2 }, new double[] { 1,2 }); // hash = 1
        builder.close();
        
        
        int[] expMap = {
           1,//w
           2,//n
           0,0,0,192,// start and end of block
           2,2,0,192,0,384
        };
        
        final DataInputStream disMap = new DataInputStream(new FileInputStream(new File(folder.getPath()+".map")));
        for(int i = 0; i < expMap.length; i++) {
            final int j = disMap.readInt();
            Assert.assertEquals(expMap[i], j);
        }
        assert(disMap.available() == 0);
        
        int[][] expData = {
            { 24, 1 },
            { 25, 1 },
            { 26, 1072693248 },
            { 27, 0 },
            { 28, 1073741824 },
            { 29, 0 },
            { 30, 1 },
            { 31, 2 },
            { 32, 1072693248 },
            { 33, 0 }
        };
        int r = 0;
        final DataInputStream disData = new DataInputStream(new FileInputStream(new File(folder.getPath()+".data")));
        int i = 0;
        while(i < expData.length) {
            final int d = disData.readInt();
            if(r++ == expData[i][0]) {
                Assert.assertEquals(expData[i][1], d);
                i++;
            }
        }
                
        assert(new File(folder.getPath()+".map").delete());
        assert(new File(folder.getPath()+".data").delete());
    }

}