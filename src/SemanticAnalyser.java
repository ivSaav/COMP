import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsCheck;

import javax.swing.plaf.synth.SynthCheckBoxMenuItemUI;
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
//        addVisit("Plus", this::dealWithOperations);
//        addVisit("Equal", this::dealSomething);
        addVisit("Equal", this::dealWithAssignment);
        addVisit("MethodCall", this::dealWithMethodCall);
//        addVisit("Method", this::dealWithMethod);
        addVisit("Array", this::dealWithArrayAccess);
//        addVisit("MainMethod", this::dealWithMainMethod);

        setDefaultVisit(this::defaultVisit);

    }

    private List<Report> dealWithArrayAccess(JmmNode arrayNode, List<Report> reports) {

        JmmNode arrayIdent = Utils.getChildOfKind(arrayNode, "Ident"); //fetch array identifier
        JmmNode accessNode = arrayNode.getChildren().get(1);

        if (!this.visitedVariables.contains(arrayIdent.get("name"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable: " + arrayIdent.get("name")));
            return null;
        }

        // if it is an array access treat its type as an integer
        if (accessNode != null && accessNode.getKind().equals("Literal")) { // a[1]
            if (!accessNode.get("type").equals("int")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid array access of: "
                        + arrayIdent.get("name") + "[" + accessNode.get("type") + "]"));
            }
        }
        else if (accessNode != null && accessNode.getKind().equals("Ident")) { // a[b]
            if (!this.visitedVariables.contains(accessNode.get("name")))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable: " + accessNode.get("name")));
        }
        return null;
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

        Symbol lhsSymb;
        Type lhsType;
        if (lhs.getKind().equals("Array")) {
            if (isValidArrayAccess(lhs, reports))
                lhsType = new Type("int", false);
            else
                return null;
        }
        else {
            // Checking if destination has been declared before assignment
            if (!this.visitedVariables.contains(lhs.get("name"))){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable: " + lhs.get("name")));
                return null;
            }

            lhsSymb = this.getVariableSymbol(lhs);
            lhsType = lhsSymb.getType();
        }

        // validate lhs with rhs of expression
        reports.addAll(validateAssignExpression(lhsType, rhs));

        return null;
    }

    private List<Report> validateAssignExpression(Type lhsType, JmmNode expr) {
        List<Report> reports = new ArrayList<>();

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
        else if (kind.equals("Array")) {
            isValidArrayAccess(expr, reports);
        }
        else if (isOperator(expr)) {
            //TODO determine operation return type
        }

        return reports;
    }

    /**
     * Validates an array access
     * @param arrayNode array node
     * @param reports
     * @return
     */
    private boolean isValidArrayAccess(JmmNode arrayNode, List<Report> reports) {
        JmmNode arrayIdent = Utils.getChildOfKind(arrayNode, "Ident"); //fetch array identifier
        JmmNode accessNode = arrayNode.getChildren().get(1);

        if (!this.visitedVariables.contains(arrayIdent.get("name"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable: " + arrayIdent.get("name")));
            return false;
        }

        // if it is an array access treat its type as an integer
        if (accessNode != null && accessNode.getKind().equals("Literal")) { // a[1]
            if (accessNode.get("type").equals("int"))
               return true;
            else { // in cases of invalid array access
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid array access of: "
                        + arrayIdent.get("name") + "[" + accessNode.get("type") + "]"));
                return false;
            }
        }
        else if (accessNode != null && accessNode.getKind().equals("Ident")) { // a[b]
            if (!this.visitedVariables.contains(accessNode.get("name"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable: " + accessNode.get("name")));
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Checks whether the arithmetic operation is valid
     * @param node visited node to evaluate
     * @param reports list of existing reports
     * @return list of reports with possible added reports, if necessary
     */
    private List<Report> dealWithOperations(JmmNode node, List<Report> reports) {
        JmmNode scope = Utils.findScope(node);
        List<JmmNode> opChildren = node.getChildren();
        List<Type> types = new ArrayList<>();

        // If the operation comes from the class
        if (scope.getKind().equals("Class")) {
            Map<String, Symbol> fields = st.getField();
            for (JmmNode children : opChildren) {
                Symbol symbol = fields.get(children.get("name"));
                types.add(symbol.getType());
            }
        }

        // If the operation comes from a method
        else if (scope.getKind().equals("Method")) {
            Map<String, Symbol> getVariables = st.getVariables(scope.get("name"));
            for (JmmNode children : opChildren) {
                Symbol symbol = getVariables.get(children.get("name"));
                types.add(symbol.getType());
            }
        }

        // Verify if operation is made by operator with the same type
        if (!verifySameTypes(types)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Arithmetic Operation made by variables with different types"));
            return null;
        }

        // Verify if the type of the List<Type> is of arrays
        if (types.get(0).isArray())
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Arithmetic Operations with arrays"));

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
     * Verify if all elements of the list are the same type
     * @param types list with types
     * @return true if so, false otherwise
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
