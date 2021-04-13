import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class SemanticAnalyser extends AJmmVisitor<List<Report>, List<Report>> {

    private final SymbolsTable st;

    private final Set<String> visitedVariables;

    public SemanticAnalyser(SymbolsTable st) {
        this.st = st;
        this.visitedVariables = new HashSet<>();

        addVisit("VarDecl", this::dealWithVarDecl);
        addVisit("Plus", this::dealWithOperations);
        addVisit("Equal", this::dealWithAssignment);
        addVisit("MethodCall", this::dealWithMethodCall);
//        addVisit("Method", this::dealWithMethod);
//        addVisit("MainMethod", this::dealWithMainMethod);

        setDefaultVisit(this::defaultVisit);

    }

    private List<Report> dealWithMethodCall(JmmNode node, List<Report> reports) {
        String methodName = node.get("name");

        // checking if it's a defined method in this class
        if (this.st.getMethod(methodName) == null) {
            JmmNode tmp = node.getChildren().get(0);

            // class identifier
            String nodeKind = tmp.getKind();
            if (nodeKind.equals("Ident")) {

                if (!st.getImports().contains(nodeKind))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Couldn't resolve method call: " + methodName));
            }
        }

        // TODO verify if return type is valid
        return null;
    }

    /**
     * Handler for assignment nodes
     * @param node - starting node (Equals)
     * @param reports
     * @return
     */
    private List<Report> dealWithAssignment(JmmNode node, List<Report> reports) {
        JmmNode lhs = node.getChildren().get(0);
        JmmNode rhs = node.getChildren().get(1);

        // Checking if destination has been declared before assignment
        System.out.println(lhs.get("name") + "===============================");
        if (!this.visitedVariables.contains(lhs.get("name"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable: " + lhs.get("name")));
        }

        reports.addAll(validateAssignExpression(lhs, rhs));

        return null;
    }

    private List<Report> validateAssignExpression(JmmNode lhs, JmmNode expr) {
        List<Report> reports = new ArrayList<>();

        Symbol lhsSymb = this.getVariableSymbol(lhs);
        Type lhsType = lhsSymb.getType();

        String kind = expr.getKind();

        if (kind.equals("Ident")) {
            if (!this.visitedVariables.contains(expr.get("name"))) // variable wasn't declared
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable " + expr.get("name")));
            else {
                Symbol rhsSymbol = this.getVariableSymbol(expr);
                Type rhsType = rhsSymbol.getType();

                if (!lhsType.equals(rhsType))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Unmatched data types in assignment: "  + lhsType.getName() + " and " + rhsType.getName()));
            }
        }
        else if (kind.equals("Literal")) { // int or boolean

            // checking lhs and rhs have the same data type
            if (!lhsType.getName().equals(expr.get("type")) || lhsType.isArray())
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                            "Unmatched data types in assignment: "
                                    + lhsType.getName() + (lhsType.isArray() ? "[]" : "")
                                    + " and " + expr.get("type")));
        }
        else if (isOperator(expr)) {
            //TODO determine operation return type
        }

        return reports;
    }

    /**
     *
     * @param node
     * @param reports
     * @return
     */
    private List<Report> dealWithOperations(JmmNode node, List<Report> reports) {
        JmmNode scope = Utils.findScope(node);

        // Verify if operation is made by operator with the same type
        List<JmmNode> opChildren = node.getChildren();

        List<Type> types = new ArrayList<>();

        if (scope.getKind().equals("Class")) {
            Map<String, Symbol> fields = st.getField();
            for (JmmNode children : opChildren) {
                Symbol symbol = fields.get(children.get("name"));
                types.add(symbol.getType());
            }
        }
        else if (scope.getKind().equals("Method")) {
            Map<String, Symbol> getVariables = st.getVariables(scope.get("name"));
            for (JmmNode children : opChildren) {
                Symbol symbol = getVariables.get(children.get("name"));
                types.add(symbol.getType());
            }
        }

        if (!verifySameTypes(types))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Variables with different types"));

        // Verify if it is used arrays directly for arithmetic operations


        return null;
    }

    private List<Report> dealWithVarDecl(JmmNode node, List<Report> reports) {
        String varName = node.get("name");

        // Verify if variable was already declared
        if (visitedVariables.contains(varName))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Variable " + varName + " already declared"));
        else
            visitedVariables.add(varName);


        // Verify if variable type exists
        JmmNode varChild = node.getChildren().get(0);
        String type = varChild.get("type");

        if (!verifyType(type))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Unrecognized type " + type));

        return null;
    }

    /**
     * Verifies if the type exists
     * @param type name of type
     * @return
     */
    private boolean verifyType(String type) {
        return  type.equals("int") || type.equals("boolean") ||
                st.getImports().contains(type) || st.getClassName().equals(type) || st.getSuper().equals(type);
    }

    private boolean isOperator(JmmNode node) {
        String kind = node.getKind();
        return kind.equals("Plus") || kind.equals("Minus") || kind.equals("Mult") || kind.equals("Div");
    }

    /**
     * Verifies if the type exists
     * @param types name of type
     * @return
     */
    private boolean verifySameTypes(List<Type> types) {
        Type defaultType = types.get(0);
        for (Type type : types) {
            if (!defaultType.equals(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fetches variable's symbol from local or global variables
     * @param var
     * @return
     */
    private Symbol getVariableSymbol(JmmNode var) {
        String name = var.get("name");

        // Check in global variables
        Symbol varSymbol = st.getGlobalVariable(name);

        // Wasn't in global variables
        if (varSymbol == null) {
            JmmNode scope = Utils.findScope(var); // determine method there variable is declared
            if (scope != null)
                varSymbol = st.getMethod(scope.get("name")).getVariable(var.get("name"));
        }

        return varSymbol;
    }


    private List<Report> defaultVisit(JmmNode node, List<Report> reports) {
        return null;
    }

    @Override
    public List<Report> visit(JmmNode jmmNode, List<Report> reports) {
        SpecsCheck.checkNotNull(jmmNode, () -> "Node should not be null");

        var visit = getVisit(jmmNode.getKind());

        // Postorder: 1st visit each children
        for (var child : jmmNode.getChildren()) {
            visit(child, reports);
        }

        // Postorder: then, visit the node
        var nodeResult = visit.apply(jmmNode, reports);

        return reports;
    }

}
