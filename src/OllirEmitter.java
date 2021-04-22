import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.specs.util.SpecsCheck;

public class OllirEmitter extends AJmmVisitor<String, String> {

    private SymbolsTable st;

    public OllirEmitter(SymbolsTable st) {
        this.st = st;

        setDefaultVisit(this::defaultVisit);

        addVisit("Class", this::dealWithClass);
        addVisit("Method", this::dealWithMethod);
    }

    public String defaultVisit(JmmNode node, String indent) {
        return "";
    }

    /**
     * Pass the content of the class to Ollir's notation
     * @param classNode node to visit referring a class
     * @param indent
     * @return
     */
    private String dealWithClass(JmmNode classNode, String indent) {
        StringBuilder stringBuilder = new StringBuilder(classNode.get("class"));

        // handling class fields
        StringBuilder fieldParams = new StringBuilder();
        StringBuilder inits = new StringBuilder();
        int cnt = 1;
        for (Symbol field : st.getFields()) {
            String varType = Utils.getOllirType(field.getType());
            String paramName = "f" + cnt + "." + varType; // constructor arguments
            fieldParams.append(paramName).append(",");
            inits.append("\t\t" + "putfield(this, ").append(field.getName()).append(".").append(varType).append(",").append(paramName).append(").V;\n");
            cnt++;
        }

        if (fieldParams.length() > 0)
            fieldParams.setLength(fieldParams.length() - 1); // remove last comma

        stringBuilder.append("{\n");
        String classConstructor = "\t.constructor " + classNode.get("class") +"(" + fieldParams + ").V {\n"
                +       "\t\tinvokespecial(this, \"<init>\").V;\n" + inits;


        stringBuilder.append(classConstructor);
        stringBuilder.append( "\t}");

        //TODO insert class variables into constructor;
        return stringBuilder.toString();
    }


    /**
     * Pass the content of the method to Ollir's notation
     * @param methodNode node to visit referring a method
     * @param indent
     * @return
     */
    private String dealWithMethod(JmmNode methodNode, String indent) {

        StringBuilder stringBuilder = new StringBuilder();

        // Get the contents of the method from SymbolsTable
        MethodSymbols methodSymbols = this.st.getMethod(methodNode.get("name"));

        // Method declaration
        String methodDec = "\t.method public " + methodSymbols.getName();

        // Method parameters
        String methodParam = "(";
        for (int i = 0; i < methodSymbols.getParameters().size(); i++) {
            // Get the parameters of the method from SymbolsTable
            Symbol symbol = methodSymbols.getParameters().get(i);

            // Last iteraction
            if (i == methodSymbols.getParameters().size() - 1) {
                methodParam += Utils.getOllirVar(symbol) + ")";
                continue;
            }

            methodParam += Utils.getOllirVar(symbol) + ",";
        }

        // Method return
        Type retType = methodSymbols.getReturnType();
        String methodRet = "." + Utils.getOllirType(retType) + " {\n";

        JmmNode methodBody = Utils.getChildOfKind(methodNode, "MethodBody");
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(this.dealWithStatementBody(methodBody, "\t\t"));
        stringBuilder.append(methodDec).append(methodParam).append(methodRet).append(bodyBuilder);

        stringBuilder.append("\t}\n");

        return stringBuilder.toString();
    }

    private String handleReturn(JmmNode retNode, String indent) {
        StringBuilder retBuilder = new StringBuilder(indent + "End:\n");


        JmmNode methodNode = retNode.getAncestor("Method").get();

        MethodSymbols method = this.st.getMethod(methodNode.get("name"));
        Type type = method.getReturnType();
        retBuilder.append(indent + "\t"+ "ret." + Utils.getOllirType(type) + " ");

        List<String> expressions = new ArrayList<>();
        retBuilder.append(this.dealWithExpression(retNode.getChildren().get(0), 0, expressions, null, indent) + ";\n");

        return retBuilder.toString();
    }

    /**a
     * Pass the content of when an assignment is made to Ollir's notation
     * @param equalNode node to visit referring an assignment
     * @param indent
     * @return
     */
    private String dealWithEquals(JmmNode equalNode, String indent) {

        StringBuilder assign = new StringBuilder();


        JmmNode lhs = equalNode.getChildren().get(0);
        JmmNode rhs = equalNode.getChildren().get(1);

        // For the variable that stores the value
        assign.append(indent + this.resolveVariableIdentifier(lhs));

        String assignmentType = ".i32 "; // special case where destination is array access
        if (!lhs.getKind().equals("Array")) {
            Symbol lhsSymbol = this.st.getVariableSymbol(lhs);
            assignmentType = "." +  Utils.getOllirType(lhsSymbol.getType()) + " "; // if not array access, then fetch type
        }
        assign.append(" :=").append(assignmentType);

        // For the value to be saved
        assign.append(this.handleRhsAssign(rhs, assign, indent)).append(";\n");

        return assign.toString();
    }

    /**
     * Pass the content of when an assignment is made to Ollir's notation
     * @param rhs node to visit referring the content that follows the equal sign
     * @param builder an object to add content with Ollir's notation
     * @return
     */
    private String handleRhsAssign(JmmNode rhs, StringBuilder builder, String indent) {
        StringBuilder rhsBuilder = new StringBuilder();

        switch (rhs.getKind()) {
            case "Literal":
                rhsBuilder.append(Utils.getOllirLiteral(rhs));
                break;
            case "Ident":
            case "Array":
                rhsBuilder.append(this.resolveVariableIdentifier(rhs));
                break;
            case "MethodCall": // In the case of a call to a method
                List<String> auxExpressions = new ArrayList<>();

                String methodCall = this.handleMethodCall(rhs, 0, auxExpressions, 0, "");

                for (String aux : auxExpressions)
                    builder.insert(0, aux + ";\n");

                rhsBuilder.append(methodCall);

                break;
            case "New":
                String ident = "new(" + rhs.get("name") + ")." + rhs.get("name") + ";\n";

                JmmNode equalNode = rhs.getParent();
                JmmNode lhsNode = equalNode.getChildren().get(0);

                String invoke = "invokespecial(" + lhsNode.get("name") + "." + rhs.get("name")  + ", \"<init>\").V";

                rhsBuilder.append(ident).append(invoke);
                break;

            default:
                // In the case it is an expression
                if (Utils.isOperator(rhs)) {
                    List<String> expr = new ArrayList<>();
                    String rhsExpr =  this.dealWithExpression(rhs, 0, expr, null, "");

                    rhsExpr = this.insertAuxiliarExpressions(builder, expr, true, indent);

                    rhsBuilder.append(rhsExpr);
                }
        }
        return rhsBuilder.toString();
    }

    /**
     * Pass the content of when an if logical condition is made to Ollir's notation
     * @param ifNode node to visit referring an if logical condition
     * @param indent
     * @return
     */
    private String dealWithIf(JmmNode ifNode,String indent) {
        StringBuilder ifBuilder = new StringBuilder(indent + "if (");

        List<String> expr = new ArrayList<>();
        this.dealWithExpression(ifNode.getChildren().get(0), 0, expr, null, "");

        String auxExp = this.insertAuxiliarExpressions(ifBuilder, expr, true, indent);

        ifBuilder.append(auxExp).append(") got to else;\n");

        ifBuilder.append(this.dealWithStatementBody(ifNode.getChildren().get(1), indent + "\t"));

        JmmNode elseNode = Utils.getChildOfKind(ifNode, "Else");
        ifBuilder.append(indent + "else:\n");
        ifBuilder.append(this.dealWithStatementBody(elseNode, indent + "\t"));
        // TODO endif
        return ifBuilder.toString();
    }

    private String dealWithWhile(JmmNode whileNode, String indent) {
        StringBuilder whileBuilder = new StringBuilder(indent + "Loop:\n");

        List<String> expr = new ArrayList<>();

        StringBuilder conditionBuilder = new StringBuilder(indent + "\t" + "if (");
        this.dealWithExpression(whileNode.getChildren().get(0), 0, expr, null, "");

        String aux = "";
        for (int i = 0; i < expr.size()-1; i++) {
            aux += indent +"\t" + expr.get(i) + ";\n";
        }

        conditionBuilder.insert(0, aux);

        String auxExp = expr.get(expr.size()-1);

        conditionBuilder.append(auxExp).append(") goto Body;\n").append(indent).append(indent + "goto EndLoop;\n");

        whileBuilder.append(conditionBuilder);

        StringBuilder bodyBuilder = new StringBuilder(indent + "Body:\n");

        bodyBuilder.append(this.dealWithStatementBody(whileNode.getChildren().get(1), indent + "\t"));

        whileBuilder.append(bodyBuilder);

        whileBuilder.append(indent + "EndLoop:\n");

        return whileBuilder.toString();
    }

    private String dealWithStatementBody(JmmNode statement, String indent) {
        StringBuilder stmBuilder = new StringBuilder();

        for (JmmNode child : statement.getChildren()) {

            switch (child.getKind()) {
                case "Equal":
                    stmBuilder.append(this.dealWithEquals(child, indent));
                    break;
                case "If":
                    stmBuilder.append(this.dealWithIf(child, indent ));
                    break;
                case "While":
                    stmBuilder.append(this.dealWithWhile(child, indent));
                    break;
                case "ret":
                    stmBuilder.append(this.handleReturn(child, indent));
                    break;
                case "Body":
                    stmBuilder.append(this.dealWithStatementBody(child, indent));
                case "MethodCall":

                    List<String> auxExpressions = new ArrayList<>();

                    StringBuilder methodBuilder = new StringBuilder();
                    methodBuilder.append(this.handleMethodCall(child, 0, auxExpressions, 1, indent));

                    String aux = "";
                    for (int i = 0; i < auxExpressions.size(); i++) {
                        aux += indent + auxExpressions.get(i) + ";\n";
                    }

                    methodBuilder.insert(0, aux);
                    stmBuilder.append(methodBuilder.toString() + ";\n");
                    break;
            }
        }

        return stmBuilder.toString();
    }


    /**
     * Saves the content of when an expression is made with the Ollir's notation
     * @param expr node to visit referring an expression
     * @param level to track the current level of the expression tree
     * @param expressions list of strings that stores in Ollir's notation possible auxiliary variables of the main expression
     * @param createdVars auxiliar list
     * @return
     */
    private String dealWithExpression(JmmNode expr, int level, List<String> expressions, Map<String, String> createdVars, String indent) {

        if (createdVars == null)
            createdVars = new HashMap<>();

        if (Utils.isOperator(expr)) {

            // Special case where operator is unary
            if (expr.getKind().equals("Negation")) {

                JmmNode child = expr.getChildren().get(0);

                // Go to the next level in the tree
                level++;
                String innerNegation = dealWithExpression(child, level, expressions, createdVars, "");

                String t = "", ident = "";
                if (level > 1) {
                    ident = "t" + (createdVars.size() + 1) + Utils.getOllirExpReturnType(expr.getKind());
                    t = ident + " = ";
                    createdVars.put(ident, "....");
                }

                expressions.add(t + Utils.getOllirOp(expr.getKind()) + " " + innerNegation); // TODO reverse expression
                return ident;
            }
            else {
                JmmNode lhsNode = expr.getChildren().get(0);
                JmmNode rhsNode = expr.getChildren().get(1);

                level++;
                String lhsExpr = dealWithExpression(lhsNode, level, expressions, createdVars, "");
                String rhsExpr = dealWithExpression(rhsNode, level, expressions, createdVars, "");

                String t = "", ident = "";
                if (level > 1) {
                    ident = "t" + (createdVars.size() + 1) + Utils.getOllirExpReturnType(expr.getKind());
                    t = ident + " = ";
                    createdVars.put(ident, "....");
                }

                expressions.add(t + lhsExpr + " " + Utils.getOllirOp(expr.getKind()) + " " + rhsExpr);

                return ident;
            }
        }
        // The remaining options are terminal
        else if (expr.getKind().equals("Literal")) {
            return Utils.getOllirLiteral(expr);
        }
        else if (expr.getKind().equals("MethodCall")) {
            List<String> auxExpr = new ArrayList<>();
            return this.handleMethodCall(expr, 0, auxExpr, 1, "");
        }
        else if (expr.getKind().equals("Array")) {

            String varName = "t" + (createdVars.size() + 1) + Utils.getOllirExpReturnType(expr.getKind());
            createdVars.put(varName, "....");
            expressions.add(varName + " =.i32 " +  this.resolveVariableIdentifier(expr));
            return varName;
        }
        else {
            return resolveVariableIdentifier(expr);
        }
    }

    private String handleMethodCall(JmmNode methodCall, int level, List<String> auxExpressions, int counter, String indent) {

        JmmNode firstChild = null;
        JmmNode arguments = null;
        StringBuilder builder = new StringBuilder();

        if (methodCall.getKind().equals("MethodCall")) {
            firstChild = methodCall.getChildren().get(0);
            arguments = methodCall.getChildren().get(1);

            if (firstChild.getKind().equals("Ident")) { // static

                String args = this.handleMethodParameters(arguments, auxExpressions);

                builder.append(indent + "invokestatic(");
                level++;

                builder.append(firstChild.get("name") + ", " + methodCall.get("name") + ", " + args + ")");
            }
            else if (firstChild.getKind().equals("This")) { // static


                String args = this.handleMethodParameters(arguments, auxExpressions);

                String call = indent + "invokevirtual(this, " + methodCall.get("name") + ", ";

                call += args + ")";

                builder.append(call);
            }
            else if (firstChild.getKind().equals("New")) {

                builder.append(indent + "invokespecial").append(firstChild.get("name"));
                String name = firstChild.get("name");
                String auxName = "aux" + counter + "." + name;
                String ident = auxName + " :=." + name + " new(" + name + ")." + name;
                counter++;
                auxExpressions.add(ident);
                auxExpressions.add(indent + "invokespecial(" + auxName + ", <init>).V");

                level += arguments.getNumChildren();

                String args = this.handleMethodParameters(arguments, auxExpressions);

                if (level > 0) {
                    String id = "aux" + counter;
                    String call = id + " = invokevirtual("+ auxName + ", " +  methodCall.get("name") + ", " + args + ")";

                    auxExpressions.add(indent + call);
                    counter++;
                    return id;
                }
            }
        }
        else if (methodCall.getKind().equals("New")) {
            String name = methodCall.get("name");
            String varName = "aux" + counter + "." + name;
            String ident = varName + " :=." + name + " new(" + name + ")." + name;
            counter++;
            auxExpressions.add(indent + ident);
            auxExpressions.add(indent + "invokespecial(" + varName + ", <init>).V");
            builder.append(varName);
        }
        else if (methodCall.getKind().equals("Literal")) {
            String value = methodCall.get("value");
            builder.append(value);
        }

        return builder.toString();

    }

    public String handleMethodParameters(JmmNode paramsNode, List<String> auxExpressions) {

        StringBuilder paramsBuilder = new StringBuilder();
        for (JmmNode child : paramsNode.getChildren()) {
            List<String> expr = new ArrayList<>();

            String param = "";
            if (child.getKind().equals("MethodCall") || child.getKind().equals("New")) {
                param = this.handleMethodCall(child, 0, expr, 0, "");
            }
            else
                param = this.dealWithExpression(child, 1, expr, null, "");

            if (!expr.isEmpty()) {

                for (int i = 0; i < expr.size(); i++) {
                    String aux = expr.get(i);
                    auxExpressions.add(aux);
                }
            }
            paramsBuilder.append(param).append(", "); // TODO get last
        }

        if (paramsNode.getNumChildren() >= 1) {
            String aux = paramsBuilder.toString();
            String ret = aux.substring(0, aux.length()-2);
            return ret;
        }

        return paramsBuilder.toString();

    }

    /**
     * Receives a variable identifier Node
     * Determines if it is an array access an array or an identifier
     * Checks if variable is part of a method's parameters
     * @param node - variable node
     * @return ollir version of variable
     */
    private String resolveVariableIdentifier(JmmNode node) {
        StringBuilder identBuilder = new StringBuilder();
        Symbol identSymbol = null;
        boolean isArrayAccess = node.getKind().equals("Array");
        if (isArrayAccess) {
            JmmNode arrayIdent = node.getChildren().get(0);
            identSymbol = this.st.getVariableSymbol(arrayIdent);
        }
        else // node is identifier
            identSymbol = this.st.getVariableSymbol(node);
        // TODO handle getfield calls here with auxiliary variable

        // checking if variable is a parameter variable
        int paramIndex = this.getArgVariableIndex(node, identSymbol);
        if (paramIndex != -1)
            identBuilder.append("$").append(paramIndex).append("."); // assignment with parameter variable

        identBuilder.append(Utils.getOllirVar(identSymbol, isArrayAccess)); // append ollir version of variable

        if (isArrayAccess) {
            String innerAccess = "[";
            JmmNode accessNode = node.getChildren().get(1);

            if (accessNode.getKind().equals("Literal")) // A[0]
                innerAccess += accessNode.get("value") + ".i32";
            else // array access is an identifier A[b]
                innerAccess += resolveVariableIdentifier(accessNode);
            innerAccess += "].i32";

            identBuilder.append(innerAccess);
        }
        return identBuilder.toString();
    }

    /**
     * Determines if a variable is part of a method's parameters
     * @param varNode - variable to be checked
     * @param varSymbol - symbol of the variable
     * @return variable index (if is a method parameter variable); -1 otherwise
     */
    private int getArgVariableIndex(JmmNode varNode, Symbol varSymbol) {

        Optional<JmmNode> methodOptional = varNode.getAncestor("Method");
        if (methodOptional.isPresent()) {// variable assignment inside method
            JmmNode methodNode = methodOptional.get();
            MethodSymbols method = this.st.getMethod(methodNode.get("name"));
            return method.getParameterIndex(varSymbol.getName());
        }
        return -1;
    }

    private String insertAuxiliarExpressions(StringBuilder builder, List<String> auxExpressions, boolean removeAssign, String indent) {
        String lastExpr = "";
        if (!auxExpressions.isEmpty()) {

            for (int i = 0; i < auxExpressions.size() -1 ; i++) {
                builder.insert(i, indent + auxExpressions.get(i) + ";\n");
            }

            if (removeAssign) {
                lastExpr = auxExpressions.get(auxExpressions.size()-1);

                if (lastExpr.contains("="))
                    lastExpr = lastExpr.split("=")[1];
            }
        }
        return lastExpr;
    }

    private static String reduce(JmmNode node, String nodeResult, List<String> childrenResults) {
        var content = new StringBuilder();

        boolean validNode = !nodeResult.isBlank(); // don't add newlines to ignored nodes

        if (validNode)
            content.append("\n");
        content.append(nodeResult);

        for (String childResult : childrenResults) {
            content.append(childResult);
        }

//        String tab = "";
//        if (node.getKind().equals("Method"))
//            tab = "}\n";

        return content.toString();
    }

    @Override
    public String visit(JmmNode jmmNode, String indent) {
        SpecsCheck.checkNotNull(jmmNode, () -> "Node should not be null");

        var visit = getVisit(jmmNode.getKind());

        // Preorder: 1st visit the node
        var nodeResult = visit.apply(jmmNode, null);

        // Preorder: then, visit each children
        List<String> childrenResults = new ArrayList<>();
        for (var child : jmmNode.getChildren()) {
            childrenResults.add(visit(child));
        }

        return reduce(jmmNode, nodeResult, childrenResults);
    }
}
