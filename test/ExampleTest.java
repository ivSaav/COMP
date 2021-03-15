import static org.junit.Assert.*;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;
import java.io.StringReader;

import pt.up.fe.comp.TestUtils;

public class ExampleTest {


    @Test
    public void testExpression() {		
		assertEquals("Start", TestUtils.parse("class Fac {\n" +
                "    public static void main(String[]args){\n" +
                "        io.println(new Fac().ComputeFac(10));//assuming the existence\n" +
                "        // of the classfile io.class\n" +
                "    }\n" +
                "}").getRootNode().getKind());
	}

}
