
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.examples.ExamplePrintVariables;
import pt.up.fe.comp.jmm.ast.examples.ExampleVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

public class AnalysisStage implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = parserResult.getRootNode();
        node.sanitize();
        SymbolTableGenerator generator = new SymbolTableGenerator();

        generator.visit(node);
        SymbolsTable st = generator.getSt();
        System.out.println("GENERATED ============================");
        System.out.println(st);

        SemanticAnalyser semanticAnalyser = new SemanticAnalyser(st);
        List<Report> reports = new ArrayList<>();
        semanticAnalyser.visit(node, reports);
        for (Report r : reports)
            System.out.println(r);


        // System.out.println(
        //         "Print variables name and line, and their corresponding parent with Visitor that automatically performs preorder tree traversal");
        // var varPrinter = new ExamplePrintVariables("Variable", "name", "line");
        // varPrinter.visit(node, null);

        // No Symbol Table being calculated yet
        return new JmmSemanticsResult(parserResult, st, reports);

    }

}