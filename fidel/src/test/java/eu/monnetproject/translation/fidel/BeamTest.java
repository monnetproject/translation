package eu.monnetproject.translation.fidel;

import eu.monnetproject.translation.fidel.Solution;
import eu.monnetproject.translation.fidel.Beam;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author john
 */
public class BeamTest {

    public BeamTest() {
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
     * Test of leastScore method, of class Beam.
     */
    @Test
    public void test1() {
        System.out.println("test1");
        TreeSet<Solution> solns = new TreeSet<Solution>();
        Beam beam = new Beam(100);
        for (int i = 0; i < 1000; i++) {
            final Solution soln = randomSolution();
            solns.add(soln);
            beam.add(soln);
        }
        assertEquals(beam.size(), 100);
        int i = 0;
        final Iterator<Solution> expIter = solns.iterator();
        while (i < 100) {
            assertEquals(expIter.next(), beam.poll());
            // System.err.println(expIter.next().score+" vs "+beam.poll().score);
            i++;
        }
    }

    @Test
    public void test2() {
        System.out.println("test2");
        TreeSet<Solution> solns = new TreeSet<Solution>();
        Beam beam = new Beam(100);
        for (int i = 0; i < 1000; i++) {
            final Solution soln = randomSolution();
            solns.add(soln);
            beam.add(soln);
            if (i % 33 == 32) {
                solns.remove(solns.first());
                beam.poll();
            }
        }
        assertEquals(beam.size(), 100);
        int i = 0;
        final Iterator<Solution> expIter = solns.iterator();
        while (i < 10) {
            final Solution expSoln = expIter.next();
            final Solution beamSoln = beam.poll();
            System.err.println(expSoln.score() + "  " + beamSoln.score());
            assertEquals(expSoln, beamSoln);
            i++;
        }
    }
    private static final Random r = new Random();

    private static Solution randomSolution() {
        final double s = r.nextDouble();
        System.err.println(s);
        return new SolutionImpl(r.nextInt(), new int[0], new int[0], s, s);
    }

    private void verify(TreeSet<Solution> solns, Beam beam, int i) {
        final Iterator<Solution> expIter = solns.iterator();
        final ObjectBidirectionalIterator<Solution> beamIter = beam.iterator();
        while (i < beam.size()) {
            final Solution expSoln = expIter.next();
            final Solution beamSoln = beamIter.next();
            System.err.println(expSoln.score() + "  " + beamSoln.score());
            assertEquals(expSoln, beamSoln);
            i++;
        }
    }
}