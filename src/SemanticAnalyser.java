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
        addVisit("Plus", this::dealWithOperations);
        addVisit("Equal", this::dealWithAssignment);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("Array", this::dealWithArrayAccess);
        addVisit("NewArray", this::dealWithArrayInit);

        setDefaultVisit(this::defaultVisit);

    }

    private List<Report> dealWithArrayInit(JmmNode arrayNode, List<Report> reports) {
        JmmNode literal = Utils.getChildOfKind(arrayNode, "Literal");

        if (literal == null || !literal.get("type").equals("int"))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Invalid array initialization size: " +
                            (literal == null ? "" : literal.get("value"))));
        return null;
    }

    /**
     * Verify if the access to an array variable is done correctly
     * @param arrayNode visited node to evaluate
     * @param reports list of existing reports
     * @return list of reports with possible added reports, if necessary
     */
    private List<Report> dealWithArrayAccess(JmmNode arrayNode, List<Report> reports) {

        if (arrayNode.getKind().equals("Array")) {

            // Verify if an access array is in fact done over an array
            JmmNode firstChild = arrayNode.getChildren().get(0);
            if (firstChild.getKind().equals("Ident")) {

                if (!this.visitedVariables.contains(firstChild.get("name"))){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable: " + firstChild.get("name")));
                    return null;
                }
                Symbol symbolArrayDec = this.getVariableSymbol(firstChild);

                if (!symbolArrayDec.getType().isArray()) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Access to non-array variable"));
                }
            }
            else {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Access to non-array variable"));
            }

            // Verify if the index of the access array is an integer
            JmmNode secondChild = arrayNode.getChildren().get(1);
            if (secondChild.getKind().equals("Ident")) {

                if (!this.visitedVariables.contains(secondChild.get("name"))){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable: " + secondChild.get("name")));
                    return null;
                }
                Symbol symbolArrayInd = this.getVariableSymbol(secondChild);

                if (!symbolArrayInd.getType().getName().equals("int")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Access to an array with a non-integer"));
                }
            }
            else if (secondChild.getKind().equals("Literal")) {
                if (!secondChild.get("type").equals("int"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Access to an array with a non-integer"));
            }
        }
        return null;
    }

    private List<Report> dealWithMethodCall(JmmNode node, List<Report> reports) {
        String methodName = node.get("name");

        JmmNode methodNode = null;
        // checking if it's not a defined method in this class or in one of the imports
        if (this.st.getMethod(methodName) == null && !this.st.getImports().contains(methodName)) {
            JmmNode tmp = node.getChildren().get(0);

            // class identifier
            String nodeKind = tmp.getKind();
            if (nodeKind.equals("Ident")) {
                if (!st.getImports().contains(tmp.get("name")))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Couldn't resolve method call: " + methodName));
            }
        }

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
            // assuming array is valid ( it is checked in another function )
            lhsType = new Type("int", false);
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
            JmmNode arrayIdent = Utils.getChildOfKind(expr, "Ident");
            Symbol rhsSymbol = this.getVariableSymbol(arrayIdent);

            // variable wasn't declared (handled in another function)
            if (rhsSymbol == null)
                return reports;

            Type rhsType = rhsSymbol.getType();
            if (!lhsType.getName().equals(rhsType.getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                            "Unmatched data types in assignment: "  + lhsType.getName() + " and " + rhsType.getName()));
            }
        }
        else if (Utils.isOperator(expr)) {
           Type t = findOperationReturnType(expr);

           if (t == null)
               reports.add(new Report(ReportType.WARNING, Stage.SEMANTIC, -1,
                       "Couldn't determine expression result type "));
           else {
               if (!lhsType.equals(t))
                   reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                           "Unmatched data types in assignment after expression: "  + lhsType.getName() + " and " + t.getName()));
           }

        }
        else if (kind.equals("NewArray")) {
            // assuming right side is correct (validated elsewhere)
            if (!lhsType.isArray())
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                        "Invalid variable assignment: "  + lhsType.getName() + " = int[]"));
        }
        return reports;
    }

    


    /**
     * Determines the return type of an expression
     * (Assumes operation is correct)
     * @param oper
     * @return
     */
    public Type findOperationReturnType(JmmNode oper) {
        for (JmmNode child : oper.getChildren()) {
            if (!Utils.isOperator(child)) {
                if (child.getKind().equals("Literal")) {
                    if (child.get("type").equals("int"))
                        return new Type("int", false);
                    return new Type("boolean", false);
                }
                else if (child.getKind().equals("Ident")){
                    Symbol symb = getVariableSymbol(child);
                    return symb == null ? null : symb.getType();
                }
                else if (child.getKind().equals("Array")) {
                    return new Type("int", false); // assuming it's correct
                }
            }
        }
        return null;
    }

    
    /**
     * Verify whether the arithmetic operation is valid
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
