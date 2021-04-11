
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.StringReader;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {

		try {
		    Parser parser = new Parser(new StringReader(jmmCode));

		    SimpleNode root = null;
		    try {
		    	root = parser.Start(); // returns reference to root node
				//root.dump(""); // prints the tree on the screen
				//System.out.println(root.toJson()); //prints Json version of ast
			}
		    catch(Exception e) {
		    	parser.getReports().add(new Report(ReportType.ERROR, Stage.OTHER,
						-1, e.toString()));
			}catch(TokenMgrError e) {
		    	parser.getReports().add(new Report(ReportType.ERROR, Stage.LEXICAL,
						-1, e.toString()));
			}

			return new JmmParserResult(root, parser.getReports());

		} catch(Exception j) {
			List<Report> reports = new ArrayList<>();
			reports.add(new Report(ReportType.ERROR, Stage.OTHER, -1, "Error while parsing: "+j.getMessage()));
			return new JmmParserResult(null, reports);
		}
	}

    public static void main(String[] args) {
		var fileContents = SpecsIo.read("./test.txt");
		System.out.println("Executing with args: " + fileContents);
		JmmParserResult parseResult = new Main().parse(fileContents);
//		System.out.println(r.getReports());

		AnalysisStage analysisStage = new AnalysisStage();
		JmmSemanticsResult semanticResult = analysisStage.semanticAnalysis(parseResult);

		System.out.println(semanticResult.getReports());


    }
}