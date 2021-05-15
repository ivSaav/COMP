
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public void testLazysort() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();

        /*String aux = output.replace('\n', ' ');

        System.out.println("AUX - " + aux);

        assertEquals("00000000", output.trim());*/
    }

    @Test
    public void testMonteCarlo() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/MonteCarlitos.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Result: 3", output.trim());
    }

    @Test
    public void testWhilesAndIfs() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();
    }

    @Test
    public void testTicTacToe() {

        OllirResult optm = TestUtils.optimize(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));

        var result = TestUtils.backend(optm);
        TestUtils.noErrors(result.getReports());

        var output = result.run();
    }
}
