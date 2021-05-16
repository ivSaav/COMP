
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import static java.lang.System.exit;

public class Main implements JmmParser {

	private SimpleNode root;

	public JmmParserResult parse(String jmmCode) {

		try {
		    Parser parser = new Parser(new StringReader(jmmCode));

		    try {
		    	root = parser.Start(); // returns reference to root node
//				root.dump(""); // prints the tree on the screen
//				System.out.println(root.toJson()); //prints Json version of ast

			}catch(TokenMgrError e) {
		    	parser.getReports().add(new Report(ReportType.ERROR, Stage.LEXICAL,
						-1, e.toString()));
			}

			return new JmmParserResult(root, parser.getReports());

		} catch(Exception j) {
			List<Report> reports = new ArrayList<>();
			reports.add(new Report(ReportType.ERROR, Stage.OTHER, 0, "Error while parsing: " + j.getMessage()));
			return new JmmParserResult(null, reports);
		}
	}

    public static void main(String[] args) {
		var fileContents = SpecsIo.read("./test.txt");
//		System.out.println("Executing with args: " + fileContents);
		Main m = new Main();
		JmmParserResult parseResult = m.parse(fileContents);

		if (parseResult.getReports().size() > 0) {
			System.out.println("*** Found errors in syntactical stage (aborting). ***");
			System.out.println(parseResult.getReports());
			exit(1);
		}

		AnalysisStage analysisStage = new AnalysisStage();
		JmmSemanticsResult semanticResult = analysisStage.semanticAnalysis(parseResult);

		if (semanticResult.getReports().size() > 0) {
			System.out.println("*** Found errors in semantics stage (aborting). ***");
			System.out.println(semanticResult.getReports());
			exit(1);
		}

		OptimizationStage optimization = new OptimizationStage();
		OllirResult ollirResult = optimization.toOllir(semanticResult);

		if (ollirResult.getReports().size() > 0) {
			System.out.println("*** Found errors in optimization stage (aborting). ***");
			System.out.println(ollirResult.getReports());
			exit(1);
		}

		BackendStage backendStage = new BackendStage();
		JasminResult jasminResult = backendStage.toJasmin(ollirResult);

		if (jasminResult.getReports().size() > 0) {
			System.out.println("*** Found errors in backend stage (aborting). ***");
			System.out.println(jasminResult.getReports());
			exit(1);
		}

		try {
			// AST ===============
			File astOutput = new File("generated" + File.separator + "ast.txt");
			FileWriter astWriter = new FileWriter(astOutput);
			astWriter.write(m.root.toJson());
			astWriter.flush();
			astWriter.close();

		}
		catch (IOException e) {
			System.out.println("Couldn't write files.");
			e.printStackTrace();
		}


		jasminResult.run();
    }
}