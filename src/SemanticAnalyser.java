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
        addVisit("Smaller", this::dealWithOperations);
        addVisit("Negation", this::dealWithBool);
        addVisit("If", this:: dealWithIfWhileStatement);
        addVisit("While", this:: dealWithIfWhileStatement);
        addVisit("And", this::dealWithBool);
        //addVisit("Equal", this::dealWithAssignment);
        addVisit("MethodCall", this::dealWithMethodCall);
//        addVisit("Method", this::dealWithMethod);
//        addVisit("MainMethod", this::dealWithMainMethod);

        setDefaultVisit(this::defaultVisit);

    }

    private List<Report> dealWithMethodCall(JmmNode node, List<Report> reports) {
        String methodName = node.get("name");
        MethodSymbols method = this.st.getMethod(methodName);


        // checking if it's a defined method in this class
        if (method == null) {
            JmmNode tmp = node.getChildren().get(0);

            // class identifier
            String nodeKind = tmp.getKind();
            if (nodeKind.equals("Ident")) {

                if (!st.getImports().contains(nodeKind))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Couldn't resolve method call " + methodName));
            }
        }else{
            //CHECKS

            List<Symbol> parameters = method.getParameters();
            List<JmmNode> args = node.getChildren().get(1).getChildren();

            if(parameters.size()!=args.size()){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Arguments doesn't not match paramenters in method " + methodName));
                return null;
            }else{


                for(int i=0; i<parameters.size(); i++){
                    System.out.println("PARAMETER " + parameters.get(i).getType().getName());
                    System.out.println("ARG " + args.get(i).getKind());

                    switch (args.get(i).getKind()){
                        case "Ident":
                            if (getVariableSymbol(args.get(i))!= null){
                                if (!getVariableSymbol(args.get(i)).getType().getName().equals(parameters.get(i).getType().getName())){
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Arguments type don't match in method " + methodName));
                                }
                                break;
                            }
                            else{
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "No declaration available for variable at " + methodName ));
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








        // TODO verify if return type is valid
        return null;
    }


    private List<Report> dealWithIfWhileStatement (JmmNode node, List<Report> reports){

        JmmNode mustbool = node.getChildren().get(0);
        String nodeName = node.getKind();


        //im confused onto how to know if its smaller/larger/equal....
        String varKind = mustbool.getKind();
        switch(varKind){
            case "Smaller" :
            case "LiteralBool":
            case "And":
            case "Negation":
                break;
            case "Ident":
                if (getVariableSymbol(mustbool) == null){
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

                        if (!getVariables.get(currentChild.get("name")).getType().getName().equals("boolean")) {

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

        // Checking if variable as been declared before assignment
        System.out.println(lhs.get("name") + "===============================");
        if (!this.visitedVariables.contains(lhs.get("name"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Undeclared variable " + lhs.get("name")));
        }

//        Symbol lhsSymb = this.getVariableSymbol(lhs);
//        Type lhsType = lhsSymb.getType();
//        Type rhsType = Utils.determineType(rhs);
//        // Checking if lhs and rhs have the same type
//        if (!lhsType.equals(rhsType) && lhsType.isArray() || rhsType.isArray()) {
//            reports.add(
//                    new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Conflicting types in assignment " + lhsType.getName() + " and " + rhsType.getName())
//            );
//        }


        // TODO: operations ???
        return null;
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

        switch (type) {
            case "int":
                break;
            case"boolean":
                break;
            default:
                if (!verifyType(type))
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Unrecognized type " + type));
        }

        return null;
    }

    /**
     * Verifies if the type exists
     * @param type name of type
     * @return
     */
    private boolean verifyType(String type) {
        return st.getImports().contains(type) || st.getClassName().equals(type) || st.getSuper().equals(type);
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
        JmmNode scope = Utils.findScope(var);

        String name = var.get("name");

        if (scope.getKind().equals("Class")) {
            return st.getGlobalVariable(name);
        }

        return st.getMethod(scope.get("name")).getVariable(var.get("name"));
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
