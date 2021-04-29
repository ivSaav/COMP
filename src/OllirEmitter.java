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

        stringBuilder.append(" {\n");

        // handling class fields
        for (Symbol field : st.getFields()) {
            String varType = Utils.getOllirType(field.getType());
            String paramName = "\t.field private " + field.getName() + "." + varType + ";\n"; // constructor arguments
            stringBuilder.append(paramName);
        }

        String classConstructor = "\n\t.construct " + classNode.get("class") +"().V {\n"
                +       "\t\tinvokespecial(this, \"<init>\").V;\n" + "\t}\n";
        stringBuilder.append(classConstructor);

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
        MethodSymbols methodSymbols = this.st.getMethod(methodNode);

        // Method declaration
        String methodDec = "\t.method public " + methodSymbols.getName();

        // Method parameters
        String methodParam = "(";
        for (int i = 0; i < methodSymbols.getParameters().size(); i++) {
            // Get the parameters of the method from SymbolsTable
            Symbol symbol = methodSymbols.getParameters().get(i);

            // Last iteration
            if (i == methodSymbols.getParameters().size() - 1) {
                methodParam += Utils.getOllirVar(symbol);
                continue;
            }

            methodParam += Utils.getOllirVar(symbol) + ",";
        }

        methodParam += ")";

        // Method return
        Type retType = methodSymbols.getReturnType();
        String methodRet = "." + Utils.getOllirType(retType) + " {\n";

        JmmNode methodBody = Utils.getChildOfKind(methodNode, "MethodBody");
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(this.dealWithStatementBody(methodBody, "\t\t"));
        stringBuilder.append(methodDec).append(methodParam).append(methodRet).append(bodyBuilder);

        if (retType.getName().equals("void"))
            stringBuilder.append("\t ret.V void.V;\n");

        stringBuilder.append("\t}\n");

        return stringBuilder.toString();
    }

    private String handleReturn(JmmNode retNode, String indent) {
        StringBuilder retBuilder = new StringBuilder(indent + "End:\n");

        JmmNode methodNode = retNode.getAncestor("Method").get();

        MethodSymbols method = this.st.getMethod(methodNode);
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
        String varType = "";
        String varName = "";
        if (varNode.getKind().equals("Array")) {//array access
            varName = "a.array.i32";//this.handleArrayAccess(varNode);
            varType = "array.i32";
        }
        else {
            varName = this.resolveVariableIdentifier(varNode, false);
            varType = varName.substring(varName.indexOf(".")+1);
        }

        String auxId = "t" + this.idCounter++ + "." + varType;

        String getField = String.format("%s :=.%s getfield(this, %s).%s",
                auxId, varType, varName, varType);
        return new String[] {auxId, getField};
    }

    /**
     * Called when the value of a class field variable is needed
     * @param destNode
     * @return the identifier of the created expression and the created expression
     */
    private String handlePutFieldCall(JmmNode destNode, JmmNode rhsExpr, String indent) {
        String varName = "";
        StringBuilder builder = new StringBuilder();

        // TODO not sure if it's supposed to be like this
        // save value in an array class field
        // getfield --> array
        // t --> array access
        // putfield(this, t, value)
        if (destNode.getKind().equals("Array")) {
            List<String> auxExpr = new ArrayList<>();
            varName = this.handleVariable(destNode, auxExpr);
            this.insertAuxiliarExpressions(builder, auxExpr, false, indent);
        }
        else {
            varName = this.resolveVariableIdentifier(destNode, false);
        }

        String rhsVarName = this.handleRhsAssign(rhsExpr, builder, indent, false);
        String putField = String.format(indent + "putfield(this, %s, %s).V;\n",
                varName, rhsVarName);
        builder.append(putField);
        return builder.toString();
    }

    private String createLengthExpression(JmmNode lengthNode, List<String> auxExpr) {
        String auxId = "t" + this.idCounter++ + ".i32";
        String lengthExpr = auxId + " :=.i32 arraylength("+ this.resolveVariableIdentifier(lengthNode.getChildren().get(0), false) + ").i32";
        auxExpr.add(lengthExpr);
        return auxId;
    }

    /**
     * Pass the content of when an assignment is made to Ollir's notation
     * @param equalNode node to visit referring an assignment
     * @param indent
     * @return
     */
    private String dealWithEquals(JmmNode equalNode, String indent) {

        StringBuilder assign = new StringBuilder();
        List<String> expressions = new ArrayList<>();

        JmmNode lhs = equalNode.getChildren().get(0);
        JmmNode rhs = equalNode.getChildren().get(1);

        if (st.isGlobalVar(lhs)) {
            return this.handlePutFieldCall(lhs, rhs, indent);
        }

        // For the variable that stores the value
        assign.append(indent + this.handleVariable(lhs, expressions));

        this.insertAuxiliarExpressions(assign, expressions, true, indent);

        String assignmentType = ".i32 "; // special case where destination is array access
        if (!lhs.getKind().equals("Array")) {
            Symbol lhsSymbol = this.st.getVariableSymbol(lhs);
            assignmentType = "." +  Utils.getOllirType(lhsSymbol.getType()) + " "; // if not array access, then fetch type
        }

        if(rhs.getKind().equals("NewArray")) {
            assignmentType = ".array.i32 ";
        }
        assign.append(" :=").append(assignmentType);

        // For the value to be saved
        assign.append(this.handleRhsAssign(rhs, assign, indent, true)).append(";\n");

        return assign.toString();
    }

    /**
     * Pass the content of when an assignment is made to Ollir's notation
     * @param rhs node to visit referring the content that follows the equal sign
     * @param builder an object to add content with Ollir's notation
     * @return
     */
    private String handleRhsAssign(JmmNode rhs, StringBuilder builder, String indent, boolean allowComplexExpr) {
        StringBuilder rhsBuilder = new StringBuilder();

        List<String> auxExpressions = new ArrayList<>();
        switch (rhs.getKind()) {
            case "Literal":
                rhsBuilder.append(Utils.getOllirLiteral(rhs));
                break;
            case "Ident":
            case "Array":
                rhsBuilder.append(this.handleVariable(rhs, auxExpressions));
                this.insertAuxiliarExpressions(builder, auxExpressions, false, indent);

                break;
            case "MethodCall": // In the case of a call to a method
                String methodCall = this.handleMethodCall(rhs, 1, auxExpressions, "");
                rhsBuilder.append(methodCall);
                this.insertAuxiliarExpressions(builder, auxExpressions, false, indent);
                break;
            case "New":
                String ident = "new(" + rhs.get("name") + ")." + rhs.get("name") + ";\n";

                JmmNode equalNode = rhs.getParent();
                JmmNode lhsNode = equalNode.getChildren().get(0);

                String invoke = indent + "invokespecial(" + lhsNode.get("name") + "." + rhs.get("name")  + ", \"<init>\").V";

                rhsBuilder.append(ident).append(invoke);
                break;

            case "Length":
                String lengthIdent = this.createLengthExpression(rhs, auxExpressions);
                this.insertAuxiliarExpressions(builder, auxExpressions, false, indent);
                rhsBuilder.append(lengthIdent);
                break;

            case "NewArray":
                String id = "";
                String varName = "";

                String newInit = "new(array" + this.handleMethodParameters(rhs, auxExpressions) + ").array.i32";
                if (!allowComplexExpr) { // create auxiliary variable
                    varName = "t" + this.idCounter++ + ".array.i32";
                    id = varName;
                    auxExpressions.add(varName + " :=.array.i32 " + newInit);
                }
                else // put whole expression
                    id = newInit;

                for (int i = 0; i < auxExpressions.size(); i++) {
                    builder.insert(0, indent + auxExpressions.get(i) + ";\n");
                }

                rhsBuilder.append(id);
                break;

            default:
                // In the case it is an expression
                if (Utils.isOperator(rhs)) {
                    List<String> expr = new ArrayList<>();

                    // level determines the kind o expression which is created
                    // level 0 --> a + b
                    // level > 0 ---> t1 = a + b
                    int level = allowComplexExpr ? 0 : 1;
                    String rhsExpr =  this.dealWithExpression(rhs, level, expr, indent);

                    if (allowComplexExpr)
                        rhsExpr = this.insertAuxiliarExpressions(builder, expr, true, indent);
                    else
                        this.insertAuxiliarExpressions(builder, expr, false, indent);

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
        String auxExp = this.dealWithExpression(ifNode.getChildren().get(0), 0, expr, "");

        if (!expr.isEmpty())
            auxExp = this.insertAuxiliarExpressions(ifBuilder, expr, true, indent);
        else
            auxExp += " &&.bool true.bool";

        ifBuilder.append(auxExp).append(") goto Then;\n");

        JmmNode elseNode = Utils.getChildOfKind(ifNode, "Else");
        ifBuilder.append(this.dealWithStatementBody(elseNode, indent + "\t"));
        ifBuilder.append(indent + "\tgoto endif;\n");
        ifBuilder.append(indent + "Then:\n");
        ifBuilder.append(this.dealWithStatementBody(ifNode.getChildren().get(1), indent + "\t"));
        
        ifBuilder.append(indent + "endif:\n");
        return ifBuilder.toString();
    }

    private String dealWithWhile(JmmNode whileNode, String indent) {
        StringBuilder whileBuilder = new StringBuilder(indent + "Loop:\n");

        List<String> expr = new ArrayList<>();

        StringBuilder conditionBuilder = new StringBuilder(indent + "\t" + "if (");
        String auxExp = this.dealWithExpression(whileNode.getChildren().get(0), 0, expr, "");

        if (!expr.isEmpty())
            auxExp = this.insertAuxiliarExpressions(whileBuilder, expr, true, indent);
        else
            auxExp += " &&.bool true.bool";

        conditionBuilder.append(auxExp).append(") goto Body;\n").append(indent).append(indent + "goto EndLoop;\n");

        whileBuilder.append(conditionBuilder);

        StringBuilder bodyBuilder = new StringBuilder(indent + "Body:\n");

        bodyBuilder.append(this.dealWithStatementBody(whileNode.getChildren().get(1), indent + "\t"));

        whileBuilder.append(bodyBuilder);
        whileBuilder.append(indent).append("\tgoto Loop;\n");
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
            MethodSymbols methodSymbols = st.getMethod(expr);

            String varName = "t" + this.idCounter++ + "." + Utils.getOllirType(methodSymbols.getReturnType());

            List<String> auxExpr = new ArrayList<>();
            String methodCall = varName + " :=.i32 " +  this.handleMethodCall(expr, 0, auxExpr,"");
            expressions.addAll(auxExpr);
            expressions.add(methodCall);
            return varName;
        }
        else if (expr.getKind().equals("Length")) {
            return this.createLengthExpression(expr, expressions);
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

                builder.append(indent).append("invokestatic(");
                String retType = this.determineMethodReturnType(methodCall);
                builder.append(firstChild.get("name") + ", \"" + methodCall.get("name") + "\"" + args + ").").append(retType); // TODO not always void

                if (level > 0) {
                    String id = "aux" + this.idCounter + "." + retType;
                    this.idCounter++;

                    String call = String.format("%s :=.%s %s", id, retType, builder);
                    auxExpressions.add(call);
                    return id;
                }

            }
            else if (firstChild.getKind().equals("This")) { // static
                String retType = this.determineMethodReturnType(methodCall);

                String args = this.handleMethodParameters(arguments, auxExpressions);

                String auxExpr = indent + "invokevirtual(this, \"" + methodCall.get("name") + "\"";


                auxExpr += args + ")." + retType;

                builder.append(auxExpr);

                if (level > 0) {
                    String id = "aux" + this.idCounter + "." + retType;
                    this.idCounter++;

                    String call = String.format("%s :=.%s %s", id, retType, builder);
                    auxExpressions.add(call);
                    return id;
                }
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
                    System.out.println("METHOD " + methodCall);
                    MethodSymbols methodSymbols = st.getMethod(methodCall);

                    String id = "aux" + this.idCounter + "." + Utils.getOllirType(methodSymbols.getReturnType());
                    this.idCounter++;

                    String call = id + " :=." + Utils.getOllirType(methodSymbols.getReturnType()) + " invokevirtual("+ auxName + ", \"" +  methodCall.get("name") + "\"" + args + ")." + Utils.getOllirType(methodSymbols.getReturnType());

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

            MethodSymbols method = st.getMethod(methodCall);
            String ret = Utils.getOllirType(method.getReturnType());
            builder.append(firstChild.get("name") + ", \"" + methodCall.get("name") + "\"" + args + ")." + ret);
        }
        else if (methodCall.getKind().equals("Literal")) {
            String value = methodCall.get("value");
            builder.append(value);
        }

        return builder.toString();
    }

    private String determineMethodReturnType(JmmNode methodNode) {
        MethodSymbols methodSymb = st.getMethod(methodNode);

        if (methodSymb == null) {// not a defined method in this class
            JmmNode parent = methodNode.getParent();

            switch (parent.getKind()) {
                case "Equal":
                    JmmNode lhs = parent.getChildren().get(0);

                    // get array identifier
                    if (lhs.getKind().equals("Array"))
                        lhs = lhs.getChildren().get(0);

                    // determine type of lhs variable
                    Symbol varSymb = this.st.getVariableSymbol(lhs);
                    return Utils.getOllirType(varSymb.getType());

            }
           return "i32"; // TODO recursively determine parent's type
        }
        else
            return Utils.getOllirType(methodSymb.getReturnType());

    }

    public String handleMethodParameters(JmmNode paramsNode, List<String> auxExpressions) {

        StringBuilder paramsBuilder = new StringBuilder();
        for (JmmNode child : paramsNode.getChildren()) {
            List<String> expr = new ArrayList<>();

            String param = "";
            if (child.getKind().equals("MethodCall") || child.getKind().equals("New")) {
                param = this.handleMethodCall(child, 1, expr, "");
            }
            else
                param = this.dealWithExpression(child, 1, expr, "");


            auxExpressions.addAll(expr);

            paramsBuilder.append(", ").append(param);
        }

        if (paramsNode.getNumChildren() > 1) {
            String aux = paramsBuilder.toString();
            String ret = aux.substring(0, aux.length()-2);
            return ret;
        }

        return paramsBuilder.toString();

    }

    private String handleVariable(JmmNode varNode, List<String> auxExpr) {
        String kind = varNode.getKind();

        switch (kind) {
            case "Array":
                JmmNode identNode = varNode.getChildren().get(0);
                if (st.isGlobalVar(identNode)) {
                    String auxVarName = "t" + this.idCounter; // t3
                    String[] res = this.handleGetFieldCall(varNode, auxExpr);

                    auxExpr.add(res[1]); // t3.array.i32 :=.array.i32 getfield(this, a.array.i32).array.i32;

                    String newVarName = "t" + this.idCounter++ + ".i32"; // t4.32

                    JmmNode accessNode = varNode.getChildren().get(1);
                    // handle inner access of array
                    // if literal creates a new auxiliary expression
                    String inner = this.handleInnerAcess(accessNode, auxExpr);
                    String arrayAccessExpr = String.format("%s :=.i32 %s%s", newVarName, auxVarName, inner); // t4.i32 :=.i32 t3[i.i32].i32.i32;
                    auxExpr.add(arrayAccessExpr);

                    return newVarName;
                }
                return this.handleArrayAccess(varNode, auxExpr); // array is not a class field
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

    private String handleArrayAccess(JmmNode arrayNode, List<String> auxExpressions) {
        JmmNode arrayIdent = arrayNode.getChildren().get(0);

        String arrayName = resolveVariableIdentifier(arrayIdent, true);

        JmmNode accessNode = arrayNode.getChildren().get(1);
        String innerAccess = this.handleInnerAcess(accessNode, auxExpressions);

        return arrayName + innerAccess;
    }

    private String handleInnerAcess(JmmNode accessNode, List<String> auxExpressions) {
        String innerAccess = "[";
        if (accessNode.getKind().equals("Literal")) {// A[0]
            String auxVar = "t" + this.idCounter++ + ".i32";
            String auxExpr = String.format("%s :=.i32 %s.i32", auxVar, accessNode.get("value"));
            auxExpressions.add(auxExpr);
            innerAccess += auxVar;
        }
        else if (accessNode.getKind().equals("Array")) {

            String arrayExpr = this.handleArrayAccess(accessNode, auxExpressions);

            String auxExpr = String.format("t%d :=.i32 %s", this.idCounter++, arrayExpr);

        }
        else // array access is an identifier A[b]
            innerAccess += resolveVariableIdentifier(accessNode, false);
        innerAccess += "].i32";
        return innerAccess;
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
        Symbol identSymbol = this.st.getVariableSymbol(node);

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
            MethodSymbols method = this.st.getMethod(methodNode);
            return method.getParameterIndex(varSymbol.getName());
        }
        return -1;
    }

    private String insertAuxiliarExpressions(StringBuilder builder, List<String> auxExpressions, boolean removeAssign, String indent) {
        String lastExpr = "";
        if (!auxExpressions.isEmpty()) {

            StringBuilder auxiliary = new StringBuilder();

            int j = removeAssign ? auxExpressions.size()-1 : auxExpressions.size();

            for (int i = 0; i < j ; i++) {
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
