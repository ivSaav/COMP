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
        addVisit("ret", this :: dealWithReturn);
        addVisit("Plus", this::dealWithOperations);
        addVisit("Smaller", this::dealWithOperations);
        addVisit("Negation", this::dealWithBool);
        addVisit("If", this:: dealWithIfWhileStatement);
        addVisit("While", this:: dealWithIfWhileStatement);
        addVisit("And", this::dealWithBool);
        //addVisit("Method", this::dealWithMethod);
        //addVisit("Equal", this::dealWithAssignment);
        addVisit("MethodCall", this::dealWithMethodCall);
//        addVisit("Method", this::dealWithMethod);
        addVisit("Array", this::dealWithArrayAccess);
        addVisit("NewArray", this::dealWithArrayInit);

        setDefaultVisit(this::defaultVisit);

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


    private List<Report> dealWithReturn(JmmNode node, List <Report> reports){


        JmmNode scope = Utils.findScope(node);
        MethodSymbols method = this.st.getMethod(scope.get("name"));

        String methodType = method.getReturnType().getName();
        String methodName = method.getName();



        JmmNode returnNode = node.getChildren().get(0);

        switch (returnNode.getKind()){
            case "Ident":
                if (getVariableSymbol(returnNode) != null) {
                    if (!getVariableSymbol(returnNode).getType().getName().equals(methodType)) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Unmatching return types at method " + methodName));
                    }
                    break;
                } else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "No declaration available for variable at method " + methodName));
                }
            case "Literal":
                if(!returnNode.get("type").equals(methodType))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Unmatching return types at method" + methodName));
                break;
            case "And":
            case "Smaller":
            case "Negation":
                if (!methodType.equals("boolean")){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Unmatching return types at method" + methodName));
                }
                break;
                /*
            case "Plus":
            case "Minus":
            case "Mult":
            case "Div":
                if (!methodType.equals("int")){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Unmatching return types" ));
                }
                break;*/

        }
        System.out.println("RET " + node.getChildren());
        return null;
    }



    private List<Report> dealWithArrayInit(JmmNode arrayNode, List<Report> reports) {
        JmmNode literal = Utils.getChildOfKind(arrayNode, "Literal");

        if (literal == null || !literal.get("type").equals("int"))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Invalid array initialization size: " +
                            (literal == null ? "" : literal.get("value"))));
        return null;
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
        MethodSymbols method = this.st.getMethod(methodName);


        JmmNode methodNode = null;
        // checking if it's not a defined method in this class or in one of the imports
        if (method == null && !this.st.getImports().contains(methodName)) {

            JmmNode tmp = node.getChildren().get(0);

            // class identifier
            String nodeKind = tmp.getKind();
            if (nodeKind.equals("Ident")) {
                if (!st.getImports().contains(tmp.get("name")))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Couldn't resolve method call: " + methodName));
            }
        }else {
            //CHECKS

            List<Symbol> parameters = method.getParameters();
            List<JmmNode> args = node.getChildren().get(1).getChildren();

            if (parameters.size() != args.size()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Arguments doesn't not match paramenters in method " + methodName));
                return null;
            } else {


                for (int i = 0; i < parameters.size(); i++) {
                    System.out.println("PARAMETER " + parameters.get(i).getType().getName());
                    System.out.println("ARG " + args.get(i).getKind());

                    switch (args.get(i).getKind()) {
                        case "Ident":
                            if (getVariableSymbol(args.get(i)) != null) {
                                if (!getVariableSymbol(args.get(i)).getType().getName().equals(parameters.get(i).getType().getName())) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Arguments type don't match in method " + methodName));
                                }
                                break;
                            } else {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "No declaration available for variable at " + methodName));
                            }
                        case "Int":
                        case "LiteralBool":
                            break;


                    }
                    /*if(!parameters.get(i).getType().getName().equals(args.get(i).get("type"))){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Arguments type don't match in method " + methodName));
                        return null;
                    }*/
                }

            }
            //System.out.println("METHOD:" + );

        }

        System.out.println("METHOD RETURN: " + method);
        return null;
    }


    private List<Report> dealWithIfWhileStatement (JmmNode node, List<Report> reports){

        JmmNode scope = Utils.findScope(node);

        JmmNode mustbool = node.getChildren().get(0);
        String nodeName = node.getKind();
        Map<String, Symbol> variables = st.getVariables(scope.get("name"));


        //im confused onto how to know if its smaller/larger/equal....
        String varKind = mustbool.getKind();
        switch(varKind){
            case "Smaller" :
            case "LiteralBool":
            case "And":
            case "Negation":
                break;
            case "Ident":

                if (variables.get(mustbool) == null){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "No declaration available for variable " + mustbool.getKind() ));

                }
                else if (!getVariableSymbol(mustbool).getType().getName().equals("boolean")){

                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Variable at " + nodeName + " statement is of wrong type"));
                }
                break;
            case "MethodCall":
                MethodSymbols method = st.getMethod(mustbool.get("name"));

                if(!method.getReturnType().getName().equals("boolean")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Method return at "+ nodeName+" is of wrong type"));
                }
                break;

            default:
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, nodeName+ " statement is not of type boolean" ));

        }

        return null;
    }

    private List<Report> dealWithBool (JmmNode node, List<Report> reports){

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
                            Map<String, Symbol> getVariables = st.getVariables(scope.get("name"));

                            if (getVariables.get(child.get("name")) == null){
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "No declaration available for variable " + child.getKind() ));

                            }

                            else if(!getVariables.get(child.get("name")).getType().getName().equals("boolean")){
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Negation done with wrong type"));
                            }
                            break;
                        case "MethodCall":
                            MethodSymbols method = st.getMethod(child.get("name"));

                            if(!method.getReturnType().getName().equals("boolean")) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid method call at negation"));
                            }
                            break;
                    }

                break;
            case "And":


                for(JmmNode currentChild : opChildren){

                    if (currentChild.getKind().equals("Ident")) {
                        Map<String, Symbol> getVariables = st.getVariables(scope.get("name"));
                        if (getVariables.get(currentChild) == null){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "No declaration available for variable " + currentChild.getKind() ));

                        }
                        else if (!getVariables.get(currentChild.get("name")).getType().getName().equals("boolean")) {

                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "logic AND done with wrong type"));
                            break;
                        }
                    } else if(currentChild.getKind().equals("MethodCall")){

                            MethodSymbols method = st.getMethod(currentChild.get("name"));

                            if(!method.getReturnType().getName().equals("boolean")) {
                                System.out.println("morreu aqui");
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid method call at negation"));
                                break;
                            }

                    }
                    else if(currentChild.getKind().equals("LiteralBool") || currentChild.getKind().equals("Negation") || currentChild.getKind().equals("And")){
                    }

                    else{
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "logic AND done with wrong type"));
                        break;
                    }
                }

                break;

            default:
                break;
        }
                return null;
    }



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

        //TODO VERIFY VaRIABLES OR INT LITERALS

        return null;
    }

    private List<Report> dealWithVarDecl(JmmNode node, List<Report> reports) {
        //int i =0;
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
