import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.specs.util.SpecsCheck;

public class OllirEmitter extends AJmmVisitor<String, String> {

    private final SymbolsTable st;
    private int idCounter = 1;
    private int labelCounter = 1;

    public OllirEmitter(SymbolsTable st) {
        this.st = st;

        setDefaultVisit(this::defaultVisit);

        addVisit("Class", this::handleClass);
        addVisit("Method", this::handleMethod);
    }

    public String defaultVisit(JmmNode node, String indent) {
        return "";
    }

    public void clearLocals() {
        this.idCounter = 1;
        this.labelCounter = 1;
    }

    /**
     * Pass the content of the class to Ollir's notation
     * @param classNode node to visit referring a class
     * @param indent indentation level
     * @return String - class constructor and fields
     */
    private String handleClass(JmmNode classNode, String indent) {
        StringBuilder stringBuilder = new StringBuilder(classNode.get("class"));

        stringBuilder.append(" {\n");

        // handling class fields
        for (Symbol field : st.getFields()) {
            String fieldVar = Utils.getOllirVar(field);
            String paramName = "\t.field private " + fieldVar + ";\n"; // constructor arguments
            stringBuilder.append(paramName);
        }

        String classConstructor = "\n\t.construct " + classNode.get("class") +"().V {\n"
                +       "\t\tinvokespecial(this, \"<init>\").V;\n" + "\t" +
                "\tret.V;\n} \n";
        stringBuilder.append(classConstructor);

        return stringBuilder.toString();
    }

    /**
     * Pass the content of the method to Ollir's notation
     * @param methodNode node to visit referring a method
     * @param indent indentation level
     * @return String - method and statements
     */
    private String handleMethod(JmmNode methodNode, String indent) {

        this.clearLocals();

        StringBuilder stringBuilder = new StringBuilder();

        // Get the contents of the method from SymbolsTable
        MethodSymbols methodSymbols = this.st.getMethod(methodNode);

        // Method declaration
        String methodDec = "\t.method public ";

        if(methodSymbols.getName().equals("main"))
            methodDec += "static ";

        methodDec += methodSymbols.getName();

        // Method parameters
        StringBuilder methodParam = new StringBuilder("(");
        for (int i = 0; i < methodSymbols.getParameters().size(); i++) {
            // Get the parameters of the method from SymbolsTable
            Symbol symbol = methodSymbols.getParameters().get(i);

            // Last iteration
            if (i == methodSymbols.getParameters().size() - 1) {
                methodParam.append(Utils.getOllirVar(symbol));
                continue;
            }

            methodParam.append(Utils.getOllirVar(symbol)).append(",");
        }
        methodParam.append(")");

        // Method's return type
        Type retType = methodSymbols.getReturnType();
        String methodRet = "." + Utils.getOllirType(retType) + " {\n";

        // Method's body
        JmmNode methodBody = Utils.getChildOfKind(methodNode, "MethodBody");
        if (methodBody == null)
            throw new NullPointerException("Undefined method body for " + methodNode);
        stringBuilder.append(methodDec).append(methodParam).append(methodRet).append(this.handleStatementBody(methodBody, "\t\t"));

        // add return statement for void type methods
        if (retType.getName().equals("void"))
            stringBuilder.append("\t\tret.V;\n");

        stringBuilder.append("\t}\n");

        return stringBuilder.toString();
    }

    /**
     * Converts return node into ollir code
     * @param retNode - ret node
     * @param indent - indentation level
     * @return String
     */
    private String handleReturn(JmmNode retNode, String indent) {

        StringBuilder retBuilder = new StringBuilder();

        JmmNode methodNode = retNode.getAncestor("Method").get();

        // get method return type
        MethodSymbols method = this.st.getMethod(methodNode);
        Type type = method.getReturnType();

        List<String> expressions = new ArrayList<>(); // auxiliary expressions
        String lastExpr = this.handleExpression(retNode.getChildren().get(0), false, false, expressions);
        String ret = String.format(indent + "ret.%s %s;\n", Utils.getOllirType(type), lastExpr);
        retBuilder.append(ret);
        this.insertAuxiliarExpressions(retBuilder, expressions, false, indent);

        return retBuilder.toString();
    }


    /**
     * Called when the value of a class field variable is needed
     * @param varNode variable's node
     * @return String[] - the identifier of the created expression and the expression
     */
    private String[] handleGetFieldCall(JmmNode varNode, List<String> auxExpressions) {
        String varType;
        String varName;
        if (varNode.getKind().equals("Array")) { // array access
            JmmNode arrayIdent = varNode.getChildren().get(0);
            varName = arrayIdent.get("name") + ".array.i32";
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
     * @param destNode destination var node for putfield call
     * @return String -  the identifier of the created expression and the created expression
     */
    private String handlePutFieldCall(JmmNode destNode, JmmNode rhsExpr, String indent) {
        String varName = "";
        StringBuilder builder = new StringBuilder();

        // save value in an array class field
        // getfield --> array
        // t --> array access
        // putfield(this, t, value)
        if (destNode.getKind().equals("Array")) {
            List<String> auxExpr = new ArrayList<>();
            varName = this.handleVariable(destNode, auxExpr);
            this.insertAuxiliarExpressions(builder, auxExpr, false, indent);
        }
        else
            varName = this.resolveVariableIdentifier(destNode, false);

        String rhsVarName = this.handleRhsAssign(rhsExpr, builder, indent, false);
        String putField = String.format(indent + "putfield(this, %s, %s).V;\n",
                varName, rhsVarName);
        builder.append(putField);
        return builder.toString();
    }

    /**
     * Receives a length node for .length expressions ands creates an auxiliary expression
     * @param lengthNode - lenght node
     * @param auxExpr - list of intermediate expressions
     * @return length expression identifier
     */
    private String createLengthExpression(JmmNode lengthNode, List<String> auxExpr) {
        String auxId = "t" + this.idCounter++ + ".i32";

        JmmNode arrayIdent = lengthNode.getChildren().get(0);
        String lengthExpr = String.format("%s :=.i32 arraylength(%s).i32", auxId, this.resolveVariableIdentifier(arrayIdent, false));
        auxExpr.add(lengthExpr);
        return auxId;
    }

    /**
     * Pass the content of when an assignment is made to Ollir's notation
     * @param equalNode node to visit referring an assignment
     * @param indent - indentation level
     * @return assignment expression
     */
    private String handleEquals(JmmNode equalNode, String indent) {

        StringBuilder assign = new StringBuilder();
        List<String> expressions = new ArrayList<>();

        JmmNode lhs = equalNode.getChildren().get(0);
        JmmNode rhs = equalNode.getChildren().get(1);

        if (st.isGlobalVar(lhs)) {
            return this.handlePutFieldCall(lhs, rhs, indent);
        }

        // For the variable that stores the value
        assign.append(indent).append(this.handleVariable(lhs, expressions));

        this.insertAuxiliarExpressions(assign, expressions, false, indent);

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
     * @param indent - indentation level
     * @param allowComplexExpr - if rhs can or not be an expression
     * @return String - rhs expression or identifier
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
                String methodCall = this.handleMethodCall(rhs, allowComplexExpr, auxExpressions, "");
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
                String id;
                String varName;

                String newInit = "new(array" + this.handleMethodParameters(rhs, auxExpressions) + ").array.i32";
                if (!allowComplexExpr) { // create auxiliary variable
                    varName = "t" + this.idCounter++ + ".array.i32";
                    id = varName;
                    auxExpressions.add(varName + " :=.array.i32 " + newInit);
                }
                else // put whole expression
                    id = newInit;

                for (String auxExpression : auxExpressions)
                    builder.insert(0, indent + auxExpression + ";\n");

                rhsBuilder.append(id);
                break;

            default:
                // In the case it is an expression
                if (Utils.isOperator(rhs)) {
                    List<String> expr = new ArrayList<>();

                    // level determines the kind o expression which is created
                    // level 0 --> a + b
                    // level > 0 ---> t1 = a + b
                    String rhsExpr =  this.handleExpression(rhs, allowComplexExpr, false, expr);

                    if (allowComplexExpr)
                        this.insertAuxiliarExpressions(builder, expr, false, indent);
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
     * @param indent - indentation level
     * @return String - ollir version of if statement
     */
    private String handleIfStatement(JmmNode ifNode, String indent) {

        int labelID = this.labelCounter++;

        StringBuilder ifBuilder = new StringBuilder(indent + "if (");

        JmmNode exprNode = ifNode.getChildren().get(0);
        List<String> expr = new ArrayList<>();
        String auxExp = this.handleExpression(exprNode, true, true, expr);

        // ollir doesn't accept unary conditional operations
        auxExp = this.forceBinaryExpression(exprNode, auxExp, expr);

        if (!expr.isEmpty())
            this.insertAuxiliarExpressions(ifBuilder, expr, false, indent);

        ifBuilder.append(auxExp).append(") goto Else_" + labelID + ";\n");
        // handle Then
        ifBuilder.append(this.handleStatementBody(ifNode.getChildren().get(1), indent + "\t")); // Then
        ifBuilder.append(indent).append("\tgoto EndIf_" + labelID + ";\n");

        ifBuilder.append(indent).append("Else_" + labelID + ":\n");
        JmmNode elseNode = Utils.getChildOfKind(ifNode, "Else");
        ifBuilder.append(this.handleStatementBody(elseNode, indent + "\t"));

        ifBuilder.append(indent).append("EndIf_" + labelID + ":\n");
        return ifBuilder.toString();
    }

    private String handleWhileStatement(JmmNode whileNode, String indent) {

        int labelId = this.labelCounter++;

        StringBuilder whileBuilder = new StringBuilder(indent + "Loop_" + labelId + ":\n");

        List<String> expr = new ArrayList<>();

        JmmNode exprNode = whileNode.getChildren().get(0);
        StringBuilder conditionBuilder = new StringBuilder(indent + "\t" + "if (");
        String auxExp = this.handleExpression(exprNode, true, false, expr);

        // ollir doesn't accept unary conditional operations
        auxExp = this.forceBinaryExpression(exprNode, auxExp, expr);

        if (!expr.isEmpty())
            this.insertAuxiliarExpressions(conditionBuilder, expr, false, indent + "\t");

        conditionBuilder.append(auxExp).append(") goto Body_" + labelId + ";\n").append(indent).append(indent).append("goto EndLoop_"+ labelId + ";\n");

        whileBuilder.append(conditionBuilder);

        // statement body
        whileBuilder.append(indent).append("Body_" + labelId + ":\n").append(this.handleStatementBody(whileNode.getChildren().get(1), indent + "\t"));
        whileBuilder.append(indent).append("\tgoto Loop_"+ labelId + ";\n");
        whileBuilder.append(indent).append("EndLoop_" + labelId + ":\n");

        return whileBuilder.toString();
    }

    private String forceBinaryExpression(JmmNode exprNode, String auxExp, List<String> expr) {
        String kind = exprNode.getKind();
        if (kind.equals("Literal") || kind.equals("MethodCall")) {
           return auxExp + " &&.bool true.bool";
        }
        else if (kind.equals("Negation")) {
            JmmNode innerExpr = exprNode.getChildren().get(0);
            if (innerExpr.getNumChildren() < 2 || innerExpr.getKind().equals("MethodCall")) {
                expr.add("t" + this.idCounter++ + ".bool :=.bool " + auxExp);
                return "t" + this.idCounter + ".bool &&.bool true.bool";
            }
        }
        else if (exprNode.getNumChildren() < 2)
               return auxExp + " &&.bool true.bool";

       return auxExp;
    }


    /**
     * Coverts a statement's ody to ollir code
     * @param statement - statement node
     * @param indent - indentation level
     * @return String - statement body
     */
    private String handleStatementBody(JmmNode statement, String indent) {
        StringBuilder stmBuilder = new StringBuilder();

        for (JmmNode child : statement.getChildren()) {

            switch (child.getKind()) {
                case "Equal":
                    stmBuilder.append(this.handleEquals(child, indent));
                    break;
                case "If":
                    stmBuilder.append(this.handleIfStatement(child, indent ));
                    break;
                case "While":
                    stmBuilder.append(this.handleWhileStatement(child, indent));
                    break;
                case "ret":
                    stmBuilder.append(this.handleReturn(child, indent));
                    break;
                case "Body":
                    stmBuilder.append(this.handleStatementBody(child, indent));
                case "MethodCall":

                    List<String> auxExpressions = new ArrayList<>();

                    StringBuilder methodBuilder = new StringBuilder();
                    methodBuilder.append(this.handleMethodCall(child, true, auxExpressions, indent));

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
     * @param allowComplex if complex expressions (a + b) are allowed
     * @param expressions list of strings that stores in Ollir's notation possible auxiliary variables of the main expression
     * @return
     */
    private String handleExpression(JmmNode expr, boolean allowComplex, boolean reverse, List<String> expressions) {

        if (Utils.isOperator(expr)) {

            String expression = "", auxVar = "";
            if (!allowComplex) { // create auxiliary variable if complex expressions aren't allowed
                String type = Utils.getOllirExpReturnType(expr.getKind());
                auxVar = "t" + this.idCounter++ + type;
                expression = auxVar + " :=" + type + " ";
            }

            // Special case where operator is unary
            if (expr.getKind().equals("Negation")) {

                JmmNode child = expr.getChildren().get(0);

                // Go to the next level in the tree
                String innerNegation = handleExpression(child, allowComplex, false, expressions); // TODO fix for negation in while loops (reverse)

                // negation is cancelled of inside 'if' condition
                expression += (reverse ? "" : "!.bool ") + innerNegation;

                if (allowComplex)
                    return expression;

                expressions.add(expression); // TODO reverse expression
                return auxVar;
            }
            else {
                JmmNode lhsNode = expr.getChildren().get(0);
                JmmNode rhsNode = expr.getChildren().get(1);

                String lhsExpr = handleExpression(lhsNode, false, reverse, expressions);
                String rhsExpr = handleExpression(rhsNode, false, reverse, expressions);

                String operator;
                if (reverse)
                    operator = Utils.reverseOperatorOllit(expr.getKind());
                else
                    operator = Utils.getOllirOp(expr.getKind());

                expression += lhsExpr + " " + operator + " " + rhsExpr;
                if (allowComplex)
                    return expression;

                expressions.add(expression);
                return auxVar;
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
            String methodCall = varName + " :=.i32 " +  this.handleMethodCall(expr, true, auxExpr,"");
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

    private String handleMethodCall(JmmNode methodCall, boolean allowComplex, List<String> auxExpressions, String indent) {

        JmmNode firstChild = null;
        JmmNode arguments;
        StringBuilder builder = new StringBuilder();

        switch (methodCall.getKind()) {
            case "MethodCall" -> {
                firstChild = methodCall.getChildren().get(0);
                arguments = methodCall.getChildren().get(1);
                switch (firstChild.getKind()) {
                    case "Ident" -> { // static or virtual

                        String args = this.handleMethodParameters(arguments, auxExpressions);

                        String retType = this.determineMethodReturnType(methodCall);

                        String callName = "invokestatic";
                        String firstArg = firstChild.get("name");

                        Symbol varSymb = this.st.getVariableSymbol(firstChild);
                        if (varSymb != null) { // Obj a;   a.method();
                            callName = "invokevirtual";
                            firstArg += "." + Utils.getOllirType(varSymb.getType());
                        }

                        String stMethod = String.format(indent + "%s(%s, \"%s\"%s).%s", callName, firstArg, methodCall.get("name"), args, retType);
                        builder.append(stMethod);

                        if (!allowComplex) {
                            String id = "aux" + this.idCounter++ + "." + retType;
                            String call = String.format("%s :=.%s %s", id, retType, builder);
                            auxExpressions.add(call);
                            return id;
                        }
                    }
                    case "This" -> { // virtual
                        String retType = this.determineMethodReturnType(methodCall);

                        String args = this.handleMethodParameters(arguments, auxExpressions);

                        String auxExpr = indent + "invokevirtual(this, \"" + methodCall.get("name") + "\"";
                        auxExpr += args + ")." + retType;

                        builder.append(auxExpr);

                        if (!allowComplex) {
                            String id = "aux" + this.idCounter++ + "." + retType;
                            String call = String.format("%s :=.%s %s", id, retType, builder);
                            auxExpressions.add(call);
                            return id;
                        }
                    }
                    case "New" -> {

                        builder.append(indent).append("invokespecial").append(firstChild.get("name"));
                        String name = firstChild.get("name");
                        String auxName = "aux" + this.idCounter + "." + name;
                        this.idCounter++;
                        String ident = auxName + " :=." + name + " new(" + name + ")." + name;
                        auxExpressions.add(ident);
                        auxExpressions.add(indent + "invokespecial(" + auxName + ", \"<init>\").V");

                        allowComplex &= arguments.getNumChildren() <= 0;
                        String args = this.handleMethodParameters(arguments, auxExpressions);
                        if (!allowComplex) {
                            System.out.println("METHOD " + methodCall);
                            MethodSymbols methodSymbols = st.getMethod(methodCall);

                            String id = "aux" + this.idCounter + "." + Utils.getOllirType(methodSymbols.getReturnType());
                            this.idCounter++;

                            String retType = Utils.getOllirType(methodSymbols.getReturnType());

                            String call = String.format("%s :=.%s invokevirtual(%s, \"%s\"%s).%s",
                                                        id, retType, auxName, methodCall.get("name"), args, retType);
                            auxExpressions.add(indent + call);
                            return id;
                        }
                    }
                }
            }
            case "New" -> {
                builder.append(this.createInitExpression(methodCall, auxExpressions, indent));
            }
            case "Ident" -> {
                builder.append(this.createInitExpression(methodCall, auxExpressions, indent));
                arguments = methodCall.getChildren().get(1);
                String args = this.handleMethodParameters(arguments, auxExpressions);
                MethodSymbols method = st.getMethod(methodCall);
                String ret = Utils.getOllirType(method.getReturnType());
                String call = String.format("invokestatic(%s, \"%s\"%s).%s",
                                                firstChild.get("name"), methodCall.get("name"), args, ret);
                builder.append(call);
            }
            case "Literal" -> {
                String value = methodCall.get("value");
                builder.append(value);
            }
        }
        return builder.toString();
    }

    /**
     * Creates an auxiliary "new" expression for object initialization
     * @param methodCall "new" method call
     * @param auxExpressions list of auxiliary expressions
     * @param indent indentation level
     * @return String auxiliary variable identifier
     */
    private String createInitExpression(JmmNode methodCall, List<String> auxExpressions, String indent) {
        String name = methodCall.get("name");
        String varName = "aux" + this.idCounter++ + "." + name;
        String ident = varName + " :=." + name + " new(" + name + ")." + name;
        auxExpressions.add(indent + ident);
        auxExpressions.add(indent + "invokespecial(" + varName + ", \"<init>\").V");
        return varName;
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

                    // TODO more cases (Parameters ,Arguments ...)

            }
           return "V"; // TODO recursively determine parent's type
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
                param = this.handleMethodCall(child, false, expr, "");
            }
            else
                param = this.handleExpression(child, false, false, expr);

            auxExpressions.addAll(expr);
            paramsBuilder.append(", ").append(param);
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

                String vName = "t" + this.idCounter++ + ".i32";
                auxExpr.add(vName + " :=.i32 " +  this.handleArrayAccess(varNode, auxExpr)); // array is not a class field
                return vName;
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

            String auxVar = "t" + this.idCounter + ".i32";

            String auxExpr = String.format("t%d.i32 :=.i32 %s", this.idCounter, arrayExpr);
            auxExpressions.add(auxExpr);

            return "[" + auxVar + "].i32";
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

    private String insertAuxiliarExpressions(StringBuilder builder, List<String> auxExpressions, boolean returnLast, String indent) {
        String lastExpr = "";
        if (!auxExpressions.isEmpty()) {

            StringBuilder auxiliary = new StringBuilder();

            int j = returnLast ? auxExpressions.size()-1 : auxExpressions.size();

            for (int i = 0; i < j ; i++) {
                auxiliary.append(indent).append(auxExpressions.get(i)).append(";\n");
            }

            builder.insert(0, auxiliary);

            if (returnLast) {
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
