
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Arrays;
import java.util.ArrayList;
import java.io.StringReader;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {
		
		try {
		    Parser parser = new Parser(new StringReader(jmmCode));
    		SimpleNode root = parser.Expression(); // returns reference to root node
            	
    		root.dump(""); // prints the tree on the screen
    	
    		return new JmmParserResult(root, new ArrayList<Report>());
		} catch(ParseException e) {
			throw new RuntimeException("Error while parsing", e);
		}
	}

    public static void main(String[] args) {

        /*if (args[0].contains("fail")) {
            throw new RuntimeException("It's supposed to fail");
        }

         */
		var fileContents = SpecsIo.read("./test.txt");
		System.out.println("Executing with args: " + fileContents);
		new Main().parse(fileContents);
    }
}