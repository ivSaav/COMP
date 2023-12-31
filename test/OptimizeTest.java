
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

import org.junit.Before;
import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class OptimizeTest {

    private boolean optimization;

    @Before
    public void init() {
        // enable or disable optimizations
        optimization = true;
    }

    @Test
    public void testHelloWorld() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testSimple() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Simple.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testFindMaximum() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testLazySort() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Lazysort.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testLife() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/Life.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testMonteCarlo() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testQuickSort() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/QuickSort.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testTicTacToe() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }


    @Test
    public void testWhileAndIf() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }

    @Test
    public void testTuring() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/private/Turing.jmm"), optimization);
        TestUtils.noErrors(result.getReports());
    }
}
