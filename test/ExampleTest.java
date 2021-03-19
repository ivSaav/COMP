import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmParserResult;

public class ExampleTest {


    @Test
    public void testSimple() {
        JmmParserResult result = TestUtils.parse("test/fixtures/public/Simple.jmm");//.getRootNode().getKind()

        try {
            assertEquals("Root", result.getRootNode().getKind());
        }catch (Exception e) {
            System.out.println(result.getReports());
            fail();
        }
	}

	@Test
	public void testFindMaximum() {
        JmmParserResult result = TestUtils.parse("test/fixtures/public/FindMaximum.jmm");//.getRootNode().getKind()

        try {
            assertEquals("Root", result.getRootNode().getKind());
        }catch (Exception e) {
            System.out.println(result.getReports());
            fail();
        }
    }

    @Test
    public void testHello() {
        JmmParserResult result = TestUtils.parse("test/fixtures/public/HelloWorld.jmm");//.getRootNode().getKind()

        try {
            assertEquals("Root", result.getRootNode().getKind());
        }catch (Exception e) {
            System.out.println(result.getReports());
            fail();
        }
    }

}
