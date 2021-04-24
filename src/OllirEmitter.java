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
    private int idCounter = 1;

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

        stringBuilder.append(" {\n");
        String classConstructor = "\t.construct " + classNode.get("class") +"(" + fieldParams + ").V {\n"
                +       "\t\tinvokespecial(this, \"<init>\").V;\n" + inits;


        stringBuilder.append(classConstructor);
        stringBuilder.append( "\t}");

        return stringBuilder.toString();
    }


    /**
     * Pass the content of the method to Ollir's notation
     * @param methodNode node to visit referring a method
     * @param indent
     * @return
     */
    private String dealWithMethod(JmmNode methodNode, String indent) {

        this.idCounter = 1;

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
        retBuilder.append(this.dealWithExpression(retNode.getChildren().get(0), 0, expressions, indent) + ";\n");

        return retBuilder.toString();
    }


    /**
     * Called when the value of a class field variable is needed
     * @param varNode
     * @return the identifier of the created expression and the created expression
     */
    private String[] handleGetFieldCall(JmmNode varNode, List<String> auxExpressions) {
        Symbol varSymbol = null;// st.getGlobalVariable(varNode.get("name"));
        String varType = ""; // Utils.getOllirType(varSymbol.getType());

        String varName = "";
        if (varNode.getKind().equals("Array")) //array access
            varName = this.handleArrayAccess(varNode);
        else
            varName = this.resolveVariableIdentifier(varNode, false);

        varType = varName.substring(varName.indexOf(".")+1);

        String auxId = "t" + this.idCounter++ + "." + varType;

        String getField = String.format("%s :=.%s getfield(this, %s).%s",
                auxId, varType, varName, varType);
        return new String[] {auxId, getField};
    }

    /**
     * Called when the value of a class field variable is needed
     * @param varNode
     * @return the identifier of the created expression and the created expression
     */
    private String[] handlePutFieldCall(JmmNode varNode, String rhsVar) {
        Symbol varSymbol = st.getGlobalVariable(varNode.get("name"));
        String varType = Utils.getOllirType(varSymbol.getType());

        String auxId = "t" + this.idCounter++ + "." + varType;

        String getField = String.format("%s :=.%s putfield(this, %s).%s",
                auxId, varType, rhsVar, varType);
        return new String[] {auxId, getField};
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
        assign.append(indent + this.resolveVariableIdentifier(lhs, false));

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

        List<String> auxExpressions = new ArrayList<>();
        switch (rhs.getKind()) {
            case "Literal":
                rhsBuilder.append(Utils.getOllirLiteral(rhs));
                break;
            case "Ident":
            case "Array":
                rhsBuilder.append(this.handleVariable(rhs, auxExpressions));
                break;
            case "MethodCall": // In the case of a call to a method
                String methodCall = this.handleMethodCall(rhs, 0, auxExpressions, "");

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

            case "Length":
                // TODO
//                String auxId = "t" + this.idCounter++ + ".i32";
//                String lengthExpr = auxId + " :=.i32 arraylength("+ this.resolveVariableIdentifier(rhs.getChildren().get(0), auxExpressions) + ").i32;\n";
//                builder.insert(0, lengthExpr);
//                rhsBuilder.append(auxId);
                break;
            default:
                // In the case it is an expression
                if (Utils.isOperator(rhs)) {
                    List<String> expr = new ArrayList<>();
                    String rhsExpr =  this.dealWithExpression(rhs, 0, expr, indent);

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
        this.dealWithExpression(ifNode.getChildren().get(0), 0, expr, "");

        String auxExp = this.insertAuxiliarExpressions(ifBuilder, expr, true, indent);

        ifBuilder.append(auxExp).append(") goto Then;\n");

        JmmNode elseNode = Utils.getChildOfKind(ifNode, "Else");
        ifBuilder.append(this.dealWithStatementBody(elseNode, indent + "\t"));

        ifBuilder.append(indent + "Then:\n");
        ifBuilder.append(this.dealWithStatementBody(ifNode.getChildren().get(1), indent + "\t"));
        
        ifBuilder.append(indent + "endif:\n");
        return ifBuilder.toString();
    }

    private String dealWithWhile(JmmNode whileNode, String indent) {
        StringBuilder whileBuilder = new StringBuilder(indent + "Loop:\n");

        List<String> expr = new ArrayList<>();

        StringBuilder conditionBuilder = new StringBuilder(indent + "\t" + "if (");
        this.dealWithExpression(whileNode.getChildren().get(0), 0, expr, "");

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
                    methodBuilder.append(this.handleMethodCall(child, 0, auxExpressions, indent));

                    StringBuilder aux = new StringBuilder();
                    for (String auxExpression : auxExpressions) {
                        aux.append(indent).append(auxExpression).append(";\n");
                    }

                    methodBuilder.insert(0, aux);
                    stmBuilder.append(methodBuilder).append(";\n");
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
     * @return
     */
    private String dealWithExpression(JmmNode expr, int level, List<String> expressions, String indent) {


        if (Utils.isOperator(expr)) {

            // Special case where operator is unary
            if (expr.getKind().equals("Negation")) {

                JmmNode child = expr.getChildren().get(0);

                // Go to the next level in the tree
                level++;
                String innerNegation = dealWithExpression(child, level, expressions, "");

                String t = "", ident = "";
                if (level > 1) {
                    String type = Utils.getOllirExpReturnType(expr.getKind());
                    ident = "t" + this.idCounter + type;
                    t = ident + " :=" + type + " ";
                    this.idCounter++;
                }

                expressions.add(t + Utils.getOllirOp(expr.getKind()) + " " + innerNegation); // TODO reverse expression
                return ident;
            }
            else {
                JmmNode lhsNode = expr.getChildren().get(0);
                JmmNode rhsNode = expr.getChildren().get(1);

                level++;
                String lhsExpr = dealWithExpression(lhsNode, level, expressions, "");
                String rhsExpr = dealWithExpression(rhsNode, level, expressions, "");

                String t = "", ident = "";
                if (level > 1) {
                    String type = Utils.getOllirExpReturnType(expr.getKind());
                    ident = "t" + this.idCounter + type;
                    t = ident + " :=" + type + " ";
                    this.idCounter++;
                }

                expressions.add(t + lhsExpr + " " + Utils.getOllirOp(expr.getKind()) + " " + rhsExpr);

                return ident;
            }
        }
        // The remaining options are terminal
        // Case the terminal is a Literal
        else if (expr.getKind().equals("Literal")) {
            return Utils.getOllirLiteral(expr);
        }
        // Case the terminal is a MethodCall
        else if (expr.getKind().equals("MethodCall")) {

            // Get the return type from the method
            MethodSymbols methodSymbols = st.getMethod(expr.get("name"));
            methodSymbols.getReturnType();

            String varName = "t" + this.idCounter + "." + Utils.getOllirType(methodSymbols.getReturnType());
            this.idCounter++;

            List<String> auxExpr = new ArrayList<>();
            expressions.add(varName + " :=.i32 " +  this.handleMethodCall(expr, 0, auxExpr,""));

            for (int i = 0; i < auxExpr.size(); i++) {
                expressions.add(auxExpr.get(i));
            }

            return varName;
        }
        // Case the terminal is an Array
        else if (expr.getKind().equals("Array")) {

            String varName = "t" + this.idCounter + Utils.getOllirExpReturnType(expr.getKind());
            this.idCounter++;
            expressions.add(varName + " :=.i32 " +  this.handleArrayAccess(expr));
            return varName;
        }
        // Case the terminal is an Identifier
        else {
            return this.handleVariable(expr, expressions);
        }
    }

    private String handleMethodCall(JmmNode methodCall, int level, List<String> auxExpressions, String indent) {

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

                builder.append(firstChild.get("name") + ", \"" + methodCall.get("name") + "\", " + args + ").V");
            }
            else if (firstChild.getKind().equals("This")) { // static


                String args = this.handleMethodParameters(arguments, auxExpressions);

                String call = indent + "invokevirtual(this, \"" + methodCall.get("name") + "\", ";

                MethodSymbols methodSymbols = st.getMethod(methodCall.get("name"));

                call += args + ")." + Utils.getOllirType(methodSymbols.getReturnType());

                builder.append(call);
            }
            else if (firstChild.getKind().equals("New")) {

                builder.append(indent + "invokespecial").append(firstChild.get("name"));
                String name = firstChild.get("name");
                String auxName = "aux" + this.idCounter + "." + name;
                this.idCounter++;
                String ident = auxName + " :=." + name + " new(" + name + ")." + name;
                auxExpressions.add(ident);
                auxExpressions.add(indent + "invokespecial(" + auxName + ", \"<init>\").V");

                level += arguments.getNumChildren();

                String args = this.handleMethodParameters(arguments, auxExpressions);

                if (level > 0) {
                    MethodSymbols methodSymbols = st.getMethod(methodCall.get("name"));

                    String id = "aux" + this.idCounter + "." + Utils.getOllirType(methodSymbols.getReturnType());
                    this.idCounter++;

                    String call = id + " :=." + Utils.getOllirType(methodSymbols.getReturnType()) + " invokevirtual("+ auxName + ", \"" +  methodCall.get("name") + "\", " + args + ")." + Utils.getOllirType(methodSymbols.getReturnType());

                    auxExpressions.add(indent + call);
                    return id;
                }
            }
        }
        else if (methodCall.getKind().equals("New")) {
            String name = methodCall.get("name");
            String varName = "aux" + this.idCounter + "." + name;
            String ident = varName + " :=." + name + " new(" + name + ")." + name;
            this.idCounter++;
            auxExpressions.add(indent + ident);
            auxExpressions.add(indent + "invokespecial(" + varName + ", \"<init>\").V");
            builder.append(varName);
        }
        else if (methodCall.getKind().equals("Ident")) {
            String name = methodCall.get("name");
            String varName = "aux" + this.idCounter + "." + name;
            String ident = varName + " :=." + name + " new(" + name + ")." + name;
            this.idCounter++;
            auxExpressions.add(indent + ident);
            auxExpressions.add(indent + "invokespecial(" + varName + ", \"<init>\").V");
            builder.append(varName);

            String args = this.handleMethodParameters(arguments, auxExpressions);

            builder.append(indent + "invokestatic(");
            level++;

            MethodSymbols method = st.getMethod(methodCall.get("name"));
            String ret = Utils.getOllirType(method.getReturnType());
            builder.append(firstChild.get("name") + ", \"" + methodCall.get("name") + "\", " + args + ")." + ret);
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
                param = this.handleMethodCall(child, 0, expr, "");
            }
            else
                param = this.dealWithExpression(child, 1, expr, "");

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

//    /**
//     * Receives a variable identifier Node
//     * Determines if it is an array access an array or an identifier
//     * Checks if variable is part of a method's parameters
//     * @param node - variable node
//     * @return ollir version of variable
//     */
//    private String resolveVariableIdentifier(JmmNode node, List<String> auxExpr) {
//        StringBuilder identBuilder = new StringBuilder();
//        Symbol identSymbol = null;
//        boolean isArrayAccess = node.getKind().equals("Array");
//        if (isArrayAccess) {
//            JmmNode arrayIdent = node.getChildren().get(0);
//            identSymbol = this.st.getVariableSymbol(arrayIdent);
//        }
//        else { // node is identifier
//            if (auxExpr != null && node.getKind().equals("Length")) { // array.length
//                String auxId = "t" + this.idCounter++ + ".i32";
//                String lengthExpr = auxId + " :=.i32 arraylength("+ this.resolveVariableIdentifier(node.getChildren().get(0), auxExpr) + ").i32";
//                auxExpr.add(lengthExpr);
//                return auxId;
//            }
//
//            identSymbol = this.st.getVariableSymbol(node);
//        }
//        // TODO handle getfield calls here with auxiliary variable
//
//        // checking if variable is a parameter variable
//        int paramIndex = this.getArgVariableIndex(node, identSymbol);
//        if (paramIndex != -1)
//            identBuilder.append("$").append(paramIndex).append("."); // assignment with parameter variable
//
//        identBuilder.append(Utils.getOllirVar(identSymbol, isArrayAccess)); // append ollir version of variable
//
//        if (isArrayAccess) {
//            String innerAccess = "[";
//            JmmNode accessNode = node.getChildren().get(1);
//
//            if (accessNode.getKind().equals("Literal")) // A[0]
//                innerAccess += accessNode.get("value") + ".i32";
//            else // array access is an identifier A[b]
//                innerAccess += resolveVariableIdentifier(accessNode, auxExpr);
//            innerAccess += "].i32";
//
//            identBuilder.append(innerAccess);
//        }
//        return identBuilder.toString();
//    }

    private String handleVariable(JmmNode varNode, List<String> auxExpr) {
        String kind = varNode.getKind();

        switch (kind) {
            case "Array":
                String arrayIdent =  this.handleArrayAccess(varNode);
                JmmNode identNode = varNode.getChildren().get(0);
                if (st.isGlobalVar(identNode)) {
                    String[] res = this.handleGetFieldCall(varNode, auxExpr);
                    auxExpr.add(res[1]);
                    return res[0];
                }
                else {
                    return arrayIdent;
                }
            case "Ident":
                String varIdent = this.resolveVariableIdentifier(varNode, false);
                if (st.isGlobalVar(varNode)) {
                    String[] res = this.handleGetFieldCall(varNode, auxExpr);
                    auxExpr.add(res[1]);
                    return res[0];
                }
                else {
                    return varIdent;
                }
        }
        return "";
    }

    private String handleArrayAccess(JmmNode arrayNode) {
        JmmNode arrayIdent = arrayNode.getChildren().get(0);

        String arrayName = resolveVariableIdentifier(arrayIdent, true);
        String innerAccess = "[";
        JmmNode accessNode = arrayNode.getChildren().get(1);

        if (accessNode.getKind().equals("Literal")) // A[0]
            innerAccess += accessNode.get("value") + ".i32";
        else // array access is an identifier A[b]
            innerAccess += resolveVariableIdentifier(accessNode, false);
        innerAccess += "].i32";

        return arrayName + innerAccess;
    }

    /**
     * Receives a variable identifier Node
     * Determines if it is an array access an array or an identifier
     * Checks if variable is part of a method's parameters
     * @param node - variable node
     * @return ollir version of variable
     */
    private String resolveVariableIdentifier(JmmNode node, boolean isArrayAccess) {
        StringBuilder identBuilder = new StringBuilder();
        Symbol identSymbol = null;
        identSymbol = this.st.getVariableSymbol(node);

        // checking if variable is a parameter variable
        int paramIndex = this.getArgVariableIndex(node, identSymbol);
        if (paramIndex != -1)
            identBuilder.append("$").append(paramIndex).append("."); // assignment with parameter variable

        identBuilder.append(Utils.getOllirVar(identSymbol, isArrayAccess)); // append ollir version of variable

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

            System.out.println("AUXXXXXXX" + auxExpressions);
            StringBuilder auxiliary = new StringBuilder();
            for (int i = 0; i < auxExpressions.size() -1 ; i++) {
                auxiliary.append(indent).append(auxExpressions.get(i)).append(";\n");

            }

            builder.insert(0, auxiliary);

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

        if (node.getKind().equals("Class"))
            content.append("}");

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
