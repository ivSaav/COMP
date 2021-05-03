import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsCheck;

import java.net.URI;
import java.util.*;

public class SemanticAnalyser extends AJmmVisitor<List<Report>, Void> {

    private final SymbolsTable st;


    private final Set<String> visitedVariables; // temporary method vars

    public SemanticAnalyser(SymbolsTable st) {
        this.st = st;
        this.visitedVariables = new HashSet<>();

        addVisit("VarDecl", this::dealWithVarDecl);
        addVisit("ret", this :: dealWithReturn);
        addVisit("Plus", this::dealWithOperations);
        addVisit("Minus", this::dealWithOperations);
        addVisit("Div", this::dealWithOperations);
        addVisit("Mult", this::dealWithOperations);
        addVisit("Smaller", this::dealWithOperations);
        addVisit("Negation", this::dealWithBool);
        addVisit("If", this:: dealWithIfWhileStatement);
        addVisit("While", this:: dealWithIfWhileStatement);
        addVisit("And", this::dealWithBool);
        addVisit("Equal", this::dealWithAssignment);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("Array", this::dealWithArrayAccess);
        addVisit("NewArray", this::dealWithArrayInit);
        addVisit("Method", this::dealWithMethod);
        addVisit("Length", this::dealWithLength);

        setDefaultVisit(this::defaultVisit);
    }

    private Void dealWithLength(JmmNode node, List<Report> reports) {
        JmmNode child = node.getChildren().get(0);

        if (child.getKind().equals("Ident")) {
            Symbol symbol = st.getVariableSymbol(child);

            if (symbol == null)
                return null;
            Type type = symbol.getType();

            if (!type.isArray())
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Variable isn't an array"));
        }
        else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Variable isn't an array"));
        }
        return null;
    }

    private Void dealWithMethod(JmmNode jmmNode, List<Report> reports) {
        this.visitedVariables.clear();
        return null;
    }


    private Void dealWithReturn(JmmNode node, List <Report> reports){

        JmmNode scope = Utils.findScope(node);
        MethodSymbols method = this.st.getMethod(scope);

        String methodType = method.getReturnType().getName();
        String methodName = method.getName();

        JmmNode returnNode = node.getChildren().get(0);

        switch (returnNode.getKind()){
            case "Ident":
                if (st.getVariableSymbol(returnNode) != null) {
                    if (!st.getVariableSymbol(returnNode).getType().getName().equals(methodType))
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Unmatched return types at method " + methodName));
                }
                else
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Undeclared variable: " + returnNode.get("name")));
                break;
            case "Literal":
                if(!returnNode.get("type").equals(methodType))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Unmatched return types at method " + methodName));
                break;
            case "And":
            case "Smaller":
            case "Negation":
                if (!methodType.equals("boolean"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Unmatched return types at method " + methodName));
                break;
            case "Plus":
            case "Minus":
            case "Mult":
            case "Div":
                if (!methodType.equals("int"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Unmatched return types" ));
                break;
        }
        return null;
    }



    private Void dealWithArrayInit(JmmNode arrayNode, List<Report> reports) {
        JmmNode firstChild = arrayNode.getChildren().get(0);

        String kind = firstChild.getKind();
        switch (kind) {
            case "Literal":
                if (!firstChild.get("type").equals("int")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, arrayNode,
                            "Invalid array initialization size: " + firstChild.get("value")));
                }
                break;
            case "Ident": {
                Symbol symbol = st.getVariableSymbol(firstChild);
                if (symbol == null)
                    return null;
                Type t = symbol.getType();
                if (!(t.getName().equals("int") && !t.isArray())) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, arrayNode,
                            "Invalid array initialization size: " + firstChild.get("value")));
                }
                break;
            }
            case "Length": {
                JmmNode lengthChild = firstChild.getChildren().get(0);
                Symbol symbol = st.getVariableSymbol(lengthChild);
                Type type = symbol.getType();
                if (!type.isArray()) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, arrayNode,
                            "Invalid array initialization size: " + firstChild.get("value")));
                }
                break;
            }
        }
        return null;
    }

    /**
     * Verify if the access to an array variable is done correctly
     * @param arrayNode visited node to evaluate
     * @param reports list of existing reports
     * @return list of reports with possible added reports, if necessary
     */
    private Void dealWithArrayAccess(JmmNode arrayNode, List<Report> reports) {

        if (arrayNode.getKind().equals("Array")) {

            // Verify if an access array is in fact done over an array
            JmmNode firstChild = arrayNode.getChildren().get(0);
            if (firstChild.getKind().equals("Ident")) {

                Symbol symbolArrayDec = st.getVariableSymbol(firstChild);
                if (symbolArrayDec == null){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, arrayNode, "Undeclared variable: " + firstChild.get("name")));
                    return null;
                }

                if (!symbolArrayDec.getType().isArray()) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, arrayNode, "Access on non-array variable"));
                }
            }
            else {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, arrayNode, "Access on non-array variable"));
            }

            // Verify if the index of the access array is an integer
            JmmNode secondChild = arrayNode.getChildren().get(1);
            if (secondChild.getKind().equals("Ident")) {
                Symbol symbolArrayInd = st.getVariableSymbol(secondChild);
                if (symbolArrayInd == null){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, secondChild, "Undeclared variable: " + secondChild.get("name")));
                    return null;
                }

                if (!symbolArrayInd.getType().getName().equals("int")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, secondChild, "Access to an array with a non-integer"));
                }
            }
            else if (secondChild.getKind().equals("Literal")) {
                if (!secondChild.get("type").equals("int"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, secondChild, "Access to an array with a non-integer"));
            }
        }
        return null;
    }

    private Void dealWithMethodCall(JmmNode node, List<Report> reports) {
        String methodName = node.get("name");
        MethodSymbols method = this.st.getMethod(node);

        // checking if it's not a defined method in this class or in one of the imports
        if (method == null && !this.st.getImports().contains(methodName)) {

            JmmNode methodIdent = node.getChildren().get(0);

            // class identifier
            String nodeKind = methodIdent.getKind();
            switch (nodeKind) {
                case "Ident":
                    if (!st.getImports().contains(methodIdent.get("name")) && st.getSuper().isBlank())
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, methodIdent, "Couldn't resolve method call: " + methodName));
                    break;
                case "New":  // check if class is this. or an imported one
                    if (!st.getImports().contains(node.get("name")))
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, methodIdent, "Couldn't resolve method call: " + methodName +
                                " from " + methodIdent.get("name")));
                    break;
                case "This":
                    // verifying if method is from super class
                    if (st.getSuper().isBlank())
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, methodIdent, "Couldn't resolve method call: " + methodName));
                    break;
            }

        }
        else {
            //CHECKS
            List<Symbol> parameters = method.getParameters();
            List<JmmNode> args = node.getChildren().get(1).getChildren();

            if (parameters.size() != args.size()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Arguments don't match parameters in method: " + methodName));
                return null;
            }
            else {

                for (int i = 0; i < parameters.size(); i++) {

                    if (args.get(i).getKind().equals("Ident")) {
                        if (st.getVariableSymbol(args.get(i)) != null) {
                            if (!st.getVariableSymbol(args.get(i)).getType().getName().equals(parameters.get(i).getType().getName()))
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Arguments type don't match in method " + methodName));
                        }
                        else
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Undeclared variable at " + methodName));
                    }
                }
            }
        }
        return null;
    }


    private Void dealWithIfWhileStatement (JmmNode node, List<Report> reports){

        JmmNode scope = Utils.findScope(node);

        JmmNode mustbool = node.getChildren().get(0);
        String nodeName = node.getKind();
        Map<String, Symbol> variables = st.getVariables(scope);

        String varKind = mustbool.getKind();
        switch(varKind){
            case "Smaller" :
            case "And":
            case "Negation":
                break;

            case "Literal":
                if (!mustbool.get("type").equals("boolean"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Variable at " + nodeName + " is of wrong type"));
                break;

            case "Ident":
                if (variables.get(mustbool.get("name")) == null)
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "No declaration available for variable " + mustbool.getKind() ));
                else if (!st.getVariableSymbol(mustbool).getType().getName().equals("boolean"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Variable at " + nodeName + " statement is of wrong type"));
                break;

            case "MethodCall":
                MethodSymbols method = st.getMethod(mustbool);
                if(!method.getReturnType().getName().equals("boolean"))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Method return at "+ nodeName+" is of wrong type"));
                break;

            default:
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, nodeName+ " statement is not of type boolean" ));

        }

        return null;
    }

    private Void dealWithBool (JmmNode node, List<Report> reports){

        //for ! and &&
        JmmNode scope = Utils.findScope(node);
        List<JmmNode> opChildren = node.getChildren();
        JmmNode child;

        switch (node.getKind()){
            case "Negation":
                child = opChildren.get(0);
                switch (child.getKind()){
                    case "LiteralBool":
                        break;

                    case "Ident":
                        Map<String, Symbol> getVariables = st.getVariables(scope);
                        if (getVariables.get(child.get("name")) == null)
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child, "Undeclared variable: " + child.getKind() ));
                        else if(!getVariables.get(child.get("name")).getType().getName().equals("boolean"))
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child, "Negation done with wrong type"));
                        break;

                    case "MethodCall":
                        MethodSymbols method = st.getMethod(child);
                        if(!method.getReturnType().getName().equals("boolean"))
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child, "Invalid method call at negation"));
                        break;

                    default:
                        if (!Utils.isConditionalOperator(child))
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child, "Invalid conditional expression at negation"));
                        break;
                }
                break;
            case "And":
                for(JmmNode currentChild : opChildren){

                    if (currentChild.getKind().equals("Ident")) {
                        Map<String, Symbol> getVariables = st.getVariables(scope);
                        // Check if variable IDENTIFIER is declared
                        if (getVariables.get(currentChild.get("name")) == null)
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, currentChild, "No declaration available for variable " + currentChild.getKind()));
                        // Check if variable IDENTIFIER is a boolean
                        else if (!getVariables.get(currentChild.get("name")).getType().getName().equals("boolean")) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, currentChild, "logic AND done with wrong type"));
                            break;
                        }
                    }
                    else if(currentChild.getKind().equals("MethodCall")){
                        MethodSymbols method = st.getMethod(currentChild);

                        // Check if the return type of the METHODCALL is boolean
                        if(!method.getReturnType().getName().equals("boolean")){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, currentChild, "Invalid method call at negation"));
                            break;
                        }
                    }
                    else if (currentChild.getKind().equals("Literal")){
                        // Check if LITERAL is an boolean
                        if (!currentChild.get("type").equals("boolean")){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, currentChild, "logic AND done with wrong type"));
                            break;
                        }
                    }
                    else if (!Utils.isConditionalOperator(node)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, currentChild, "logic AND done with wrong type"));
                        break;
                    }
                }
                break;
            default:
                break;
        }
        return null;
    }


    private Void dealWithAssignment(JmmNode node, List<Report> reports) {
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
            lhsSymb = st.getVariableSymbol(lhs);
            if (lhsSymb  == null){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Undeclared variable: " + lhs.get("name")));
                return null;
            }
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
            Symbol rhsSymbol = st.getVariableSymbol(expr);
            if (rhsSymbol == null) // variable wasn't declared
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, expr, "Undeclared variable " + expr.get("name")));
            else {

                Type rhsType = rhsSymbol.getType();

                if (!lhsType.equals(rhsType))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, expr, "Unmatched data types in assignment: "  + lhsType.getName() + " and " + rhsType.getName()));
            }
        }
        else if (kind.equals("Literal")) { // int or boolean

            // checking lhs and rhs have the same data type
            if (!lhsType.getName().equals(expr.get("type")) || lhsType.isArray())
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, expr,
                            "Unmatched data types in assignment: "
                                    + lhsType.getName() + (lhsType.isArray() ? "[]" : "")
                                    + " and " + expr.get("type")));
        }
        else if (kind.equals("Array")) {
            JmmNode arrayIdent = Utils.getChildOfKind(expr, "Ident");
            Symbol rhsSymbol = st.getVariableSymbol(arrayIdent);

            // variable wasn't declared (handled in another function)
            if (rhsSymbol == null)
                return reports;

            Type rhsType = rhsSymbol.getType();
            if (!lhsType.getName().equals(rhsType.getName())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, arrayIdent,
                            "Unmatched data types in assignment: "  + lhsType.getName() + " and " + rhsType.getName()));
            }
        }
        else if (Utils.isOperator(expr)) {
           Type t = findOperationReturnType(expr);

           if (t == null)
               reports.add(new Report(ReportType.WARNING, Stage.SEMANTIC, expr,
                       "Couldn't determine expression result type "));
           else {
               if (!lhsType.equals(t))
                   reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, expr,
                           "Unmatched data types in assignment after expression: "  + lhsType.getName() + " and " + t.getName()));
           }

        }
        else if (kind.equals("NewArray")) {
            // assuming right side is correct (validated elsewhere)
            if (!lhsType.isArray())
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, expr,
                        "Invalid variable assignment: "  + lhsType.getName() + " = int[]"));
        }
        else if (kind.equals("Length")) {

            if (!(lhsType.getName().equals("int") && !lhsType.isArray())) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, expr,
                        "Unmatched data types in assignment after expression: "  + lhsType.getName() + " and int"));
            }
        }

        return reports;
    }

    /**
     * Determines the return type of an expression
     * (Assumes operation is correct)
     * @param oper operator node
     * @return operation type
     */
    public Type findOperationReturnType(JmmNode oper) {
        for (JmmNode child : oper.getChildren()) {
            if (!Utils.isOperator(child)) {
                if (child.getKind().equals("Literal"))
                    return new Type(child.get("type"), false);
                else if (child.getKind().equals("Ident")){
                    Symbol symb = st.getVariableSymbol(child);
                    return symb == null ? null : symb.getType();
                }
                else if (child.getKind().equals("Array")) {
                    return new Type("int", false); // assuming it's correct
                }
                else if (child.getKind().equals("MethodCall"))
                    return this.determineMethodReturnType(child, null);
            }
        }
        return null;
    }

    /**
     * Receives a "MethodCall" node and determines it's return type
     * Looks for locally declared methods and imported ones
     * If the method does not exist in this scope adds a new report
     * @param methodNode - "MethodCall" node
     * @param reports - list with reports
     * @return Type - method's return type
     */
    public Type determineMethodReturnType(JmmNode methodNode, List<Report> reports) {
        MethodSymbols method = this.st.getMethod(methodNode);

        if (method != null)
            return method.getReturnType();
        else { // method is not declared in this class
            if (!this.st.getImports().contains(methodNode.get("name"))) // method is not in one of the imports
                if (reports != null)
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, methodNode,
                        "Couldn't determine method return type for " + methodNode.get("name") +" in operation"));
        }
        return null;
    }

    
    /**
     * Verify whether the arithmetic operation is valid
     * @param node visited node to evaluate
     * @param reports list of existing reports
     * @return list of reports with possible added reports, if necessary
     */
    private Void dealWithOperations(JmmNode node, List<Report> reports) {
        
        List<Type> types = new ArrayList<>();
        this.getExpressionTypes(node, types, reports);

        // Verify if operation is made by operator with the same type
        if (!verifySameTypes(types)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Arithmetic Operation made by variables with different types"));
            return null;
        }

        // Verify if the type of the List<Type> is of arrays
        if (types.get(0).isArray())
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Arithmetic Operation invalid with arrays"));
        if(types.get(0).getName().equals("boolean"))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Arithmetic Operation invalid with boolean"));

        return null;
    }

    /**
     * Get all types of operands
     * @param operNode visited node to evaluate
     * @param types list of existing type
     * @param reports list of existing reports
     */
    private void getExpressionTypes(JmmNode operNode, List<Type> types, List<Report> reports) {
        List<JmmNode> opChildren = operNode.getChildren();
        JmmNode scope = Utils.findScope(operNode);

        Map<String, Symbol> variables;
        if (scope.getKind().equals("Class"))
            variables = st.getField();
        else
            variables = st.getVariables(scope);

        for (JmmNode children : opChildren) {
            
            if (Utils.isOperator(children))
                getExpressionTypes(children, types, reports);;

            if (children.getKind().equals("Literal")) {
                types.add(new Type(children.get("type"), false));
            }
            else if (children.getKind().equals("Ident")) {
                Symbol symbol = variables.get(children.get("name"));

                if (symbol == null) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,  children, "Variable " + children.get("name") + " not declared"));
                    continue;
                }
                types.add(symbol.getType());
            }
            else if (children.getKind().equals("MethodCall")) {
                Type methodType = this.determineMethodReturnType(children, reports);

                if (methodType != null)
                    types.add(methodType);
            }
            else if (children.getKind().equals("Array")) { // array access
                types.add(new Type("int", false));
            }
        }
    }

    private Void dealWithVarDecl(JmmNode node, List<Report> reports) {
        //int i =0;
        String varName = node.get("name");
        // Verify if variable was already declared
        if (!node.getParent().getKind().equals("Parameters") && visitedVariables.contains(varName)) {
            System.out.println(visitedVariables);
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Variable " + varName + " already declared"));
        }
        else
            visitedVariables.add(varName);

        // Verify if variable type exists
        JmmNode varChild = node.getChildren().get(0);
        String type = varChild.get("type");

        if (!verifyType(type))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, node, "Unrecognized type " + type));

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

    private Void defaultVisit(JmmNode node, List<Report> reports) {
        return null;
    }

    @Override
    public Void visit(JmmNode jmmNode, List<Report> reports) {
        SpecsCheck.checkNotNull(jmmNode, () -> "Node should not be null");

        var visit = getVisit(jmmNode.getKind());

        // Postorder: 1st visit each children
        for (var child : jmmNode.getChildren())
            visit(child, reports);

        // Postorder: then, visit the node
        var nodeResult = visit.apply(jmmNode, reports);
        return null;
    }

}
