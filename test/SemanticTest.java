import static org.junit.Assert.*;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.File;
import java.util.Scanner;

public class SemanticTest {


    private void testFile(String filename, boolean mustfail) {

        JmmSemanticsResult semanticsResult = null;
        try {
            File testFile = new File("test/fixtures/" + filename);
            Scanner scanner = new Scanner(testFile);

            StringBuilder codeBuilder  = new StringBuilder();
            while (scanner.hasNextLine()) {
                codeBuilder.append(scanner.nextLine()).append("\n");
            }
            JmmParserResult parseResult = TestUtils.parse(codeBuilder.toString());//.getRootNode().getKind()

            semanticsResult = TestUtils.analyse(parseResult);

            if (mustfail)
                TestUtils.mustFail(semanticsResult.getReports());
            else
                TestUtils.noErrors(semanticsResult.getReports());
        }
        catch (Exception e) {
            System.out.println("Test Failed =================================");
            System.out.println("Test File: " + filename);

            if (semanticsResult != null)
                for (Report report : semanticsResult.getReports())
                    System.out.println(report);

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
    public void testArrayIndex() {
        testFile("public/fail/semantic/arr_index_not_int.jmm", true);
    }

    @Test
    public void testArraySize() {
        testFile("public/fail/semantic/arr_size_not_int.jmm", true);
    }

    @Test
    public void testArguments() {
        testFile("public/fail/semantic/badArguments.jmm", true);
    }

    @Test
    public void testIncompatibleOperations() {
        testFile("public/fail/semantic/binop_incomp.jmm", true);
    }

    @Test
    public void testFunction() {
        testFile("public/fail/semantic/funcNotFound.jmm", true);
    }

    @Test
    public void testSimpleLength() {
        testFile("public/fail/semantic/simple_length.jmm", true);
    }

    @Test
    public void testVarExpression() {
        testFile("public/fail/semantic/var_exp_incomp.jmm", true);
    }

    @Test
    public void testVarLiteral() {
        testFile("public/fail/semantic/var_lit_incomp.jmm", true);
    }

    @Test
    public void testVarUndefined() {
        testFile("public/fail/semantic/var_undef.jmm", true);
    }

}
