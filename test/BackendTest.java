
/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class BackendTest {

    @Test
    public void testHelloWorld() {

        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testSimple() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Simple.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testFindMaximum() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Result: 28", output.trim());
    }

    @Test
    public void testLazySort() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();

        String normalized = SpecsStrings.normalizeFileContents(output.trim());

        List<String> aux = Arrays.asList(normalized.split("\n"));

        // output should have ten numbers
        assertEquals(10, aux.size());
    }

    @Test
    public void testQuickSort() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();

        output = SpecsStrings.normalizeFileContents(output.trim());

        assertEquals("1\n2\n3\n4\n5\n6\n7\n8\n9\n10", output);
    }

    @Test
    public void testMonteCarlo() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run("10000");

        assertTrue(output.trim().contains("Result:"));

        // fetch result
        List<String> split = Arrays.asList(output.trim().split("Result: "));
        assertTrue(Integer.parseInt(split.get(1)) > 300);
    }

    @Test
    public void testLife() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/LifeEndCondition.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        // creating test input
        StringBuilder inputBuilder = new StringBuilder();
        for (int i = 1; i < 15; i++) {
            inputBuilder.append(i).append("\n");
        }
        inputBuilder.append(0).append("\n"); // end condition

        var output = result.run(inputBuilder.toString());

        output= SpecsStrings.normalizeFileContents(output.trim(), true);
        List<String> out = Arrays.asList(output.split("\n"));

        // number of iterations should be 15
        assertEquals(15, out.size());
        // each line must have 100 chars
        assertEquals(100, out.get(0).length());
    }

    @Test
    public void testWhilesAndIfs() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        String expected = SpecsIo.getResource("fixtures/public/WhileAndIf.txt");
        var output = result.run();
        output = SpecsStrings.normalizeFileContents(output.trim());
        assertEquals(expected, output);
    }

    @Test
    public void testTicTacToe() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        String input = SpecsIo.getResource("fixtures/public/TicTacToe.input");
        String expected = SpecsIo.getResource("fixtures/public/TicTacToe.txt");

        var output = result.run(input);

        output = SpecsStrings.normalizeFileContents(output.trim(), true);
        assertEquals(expected, output);
    }

    @Test
    public void testTuring() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/private/Turing.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run("11011");

        output = SpecsStrings.normalizeFileContents(output.trim());
        // final result
        assertTrue(output.contains("000000000000000000\n000000111111000000"));
    }

    //========== PRIVATE TESTS ==========

    @Test
    public void testConflicts() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/private/Conflicts.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();

        output = SpecsStrings.normalizeFileContents(output.trim());
        // final result
        assertEquals("125", output);
    }

    @Test
    public void testArithmetic() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/private/Arithmetic.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();

        output = SpecsStrings.normalizeFileContents(output.trim());
        // final result
        assertEquals("20", output);
    }
}
