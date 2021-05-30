
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

			// output ast into file
			Utils.saveContents(root.toJson(), "ast.txt");

			return new JmmParserResult(root, parser.getReports());

		} catch(Exception j) {
			List<Report> reports = new ArrayList<>();
			reports.add(new Report(ReportType.ERROR, Stage.OTHER, 0, "Error while parsing: " + j.getMessage()));
			return new JmmParserResult(null, reports);
		}
	}

    public static void main(String[] args) {

		boolean optm = false;
		String filename = "test.txt";

		if (args.length < 1) {
			System.out.println("Usage: Main -o <filename>");
			exit(1);
		}

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-o"))
				optm = true;
			else if (args[i].equals("-r")) {
				System.out.println("Register allocation not implemented. (ignoring)");
			}
			else {
				filename = args[i];
				Path filePath = Paths.get(filename);
				if (!Files.exists(filePath))
					throw new IllegalArgumentException("File not found: " + filename);
			}
		}

		System.out.println("Executing with args: " + (optm ? "-o " : "" ) + filename);
		var fileContents = SpecsIo.read(filename);

		Main m = new Main();
		JmmParserResult parseResult = m.parse(fileContents);

		if (parseResult.getReports().size() > 0) {
			System.out.println("*** Found errors in syntactical stage (aborting). ***");
			Utils.printReports(parseResult.getReports());
			exit(1);
		}

		AnalysisStage analysisStage = new AnalysisStage();
		JmmSemanticsResult semanticResult = analysisStage.semanticAnalysis(parseResult);

		if (semanticResult.getReports().size() > 0) {
			System.out.println("*** Found errors in semantics stage (aborting). ***");
			Utils.printReports(semanticResult.getReports());
			exit(1);
		}

		OptimizationStage optimization = new OptimizationStage(optm);
		OllirResult ollirResult = optimization.toOllir(semanticResult);

		if (ollirResult.getReports().size() > 0) {
			System.out.println("*** Found errors in optimization stage (aborting). ***");
			Utils.printReports(ollirResult.getReports());
			exit(1);
		}

		BackendStage backendStage = new BackendStage();
		JasminResult jasminResult = backendStage.toJasmin(ollirResult);

		if (jasminResult.getReports().size() > 0) {
			System.out.println("*** Found errors in backend stage (aborting). ***");
			Utils.printReports(jasminResult.getReports());
			exit(1);
		}

		System.out.println("All generated files placed under output/");
    }
}