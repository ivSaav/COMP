
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Arrays;
import java.util.ArrayList;
import java.io.StringReader;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {

		try {
		    Parser parser = new Parser(new StringReader(jmmCode));
			SimpleNode root = null;
		    try {
		    	root = parser.Start(); // returns reference to root node
//				root.dump(""); // prints the tree on the screen
				System.out.println(root.toJson());

			}
		    catch(Exception e) {
		    	parser.getReports().add(new Report(ReportType.ERROR, Stage.SEMANTIC,
						0, e.toString()));
			}

    		return new JmmParserResult(root, parser.getReports());
		} catch(Exception e) {

			throw new RuntimeException("Error while parsing", e);
		}


//		Parser parser = new Parser(System.in);
//		try {
//			SimpleNode root = parser.Expression(); // returns reference to root node
//			root.dump(""); // prints the tree on the screen
//		}
//		catch (Exception e) {
//			System.out.println(e.toString());
//			this.reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, t.beginLine, e.toString()));
//		}
	}

    public static void main(String[] args) {

        /*if (args[0].contains("fail")) {
            throw new RuntimeException("It's supposed to fail");
        }

         */
		var fileContents = SpecsIo.read("./test.txt");
		System.out.println("Executing with args: " + fileContents);
		JmmParserResult r = new Main().parse(fileContents);
		System.out.println(r.getReports());

    }
}