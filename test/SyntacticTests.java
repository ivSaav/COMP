import static org.junit.Assert.*;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;

import java.io.File;
import java.util.Scanner;

public class SyntacticTests {


    private void testFile(String filename, boolean mustfail) {

        try {
            File testFile = new File("test/fixtures/" + filename);
            Scanner scanner = new Scanner(testFile);

            StringBuilder codeBuilder  = new StringBuilder();
            while (scanner.hasNextLine()) {
                codeBuilder.append(scanner.nextLine()).append("\n");
            }
            JmmParserResult res = TestUtils.parse(codeBuilder.toString());//.getRootNode().getKind()
            if (mustfail)
                TestUtils.mustFail(res.getReports());
            else
                TestUtils.noErrors(res.getReports());
        }
        catch (Exception e) {
            System.out.println("Test Failed =================================");
            System.out.println("Test File: " + filename);
            e.printStackTrace();
            fail();
        }
    }

    /* ==================== SUCCESS TESTS ==================== */
    @Test
    public void testSimple() {
        testFile("public/Simple.jmm", false);
	}

	@Test
	public void testFindMaximum() {
        testFile("public/FindMaximum.jmm", false);
    }

    @Test
    public void testLazysort() {
        testFile("public/Lazysort.jmm", false);
    }

    @Test
    public void testLife() {
        testFile("public/Life.jmm", false);
    }

    @Test
    public void testMonteCarlo() {
        testFile("public/MonteCarloPi.jmm", false);
    }

    @Test
    public void testQuickSort() {
        testFile("public/QuickSort.jmm", false);
    }

    @Test
    public void testTicTacToe() {
        testFile("public/TicTacToe.jmm", false);
    }

    @Test
    public void testWhileAndIf() {
        testFile("public/WhileAndIf.jmm", false);
    }

    /* ==================== FAILURE TESTS ==================== */

    @Test
    public void testWhiles() {
        testFile("public/fail/semantic/LengthError.jmm", true);
    }

    @Test
    public void testBlowUp() {
        testFile("public/fail/syntactical/BlowUp.jmm", true);
    }

    @Test
    public void testLengthError() {
        testFile("public/fail/syntactical/LengthError.jmm", true);
    }

    @Test
    public void testMissingRightPair() {
        testFile("public/fail/syntactical/MissingRightPar.jmm", true);
    }

    @Test
    public void testMultipleSequential() {
        testFile("public/fail/syntactical/MultipleSequential.jmm", true);
    }

    @Test
    public void testNestedLoop() {
        testFile("public/fail/syntactical/NestedLoop.jmm", true);
    }

}
