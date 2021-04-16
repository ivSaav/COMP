import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.specs.util.SpecsCheck;

public class OllirEmitter extends AJmmVisitor<Void, String> {

    private SymbolsTable st;

    public OllirEmitter(SymbolsTable st) {
        this.st = st;

        setDefaultVisit(this::defaultVisit);

        addVisit("Class", this::dealWithClass);
        addVisit("Method", this::dealWithMethod);
        addVisit("Equal", this::dealWithEquals);
        addVisit("If", this::dealWithIf);
        // addVisit("While", this::dealWithWhile);
    }

    public String defaultVisit(JmmNode node, Void unused) {
        return "";
    }

    /**
     * Pass the content of the class to Ollir's notation
     * @param classNode node to visit referring a class
     * @param unused
     * @return
     */
    private String dealWithClass(JmmNode classNode, Void unused) {
        StringBuilder stringBuilder = new StringBuilder(classNode.get("class"));
        
        stringBuilder.append("{\n");
        String classConstructor = "\t.constructor " + classNode.get("class") +"().V {\n"
                                +       "\t\tinvokespecial(this, \"<init>\").V;\n"
                                + "\t}";
        stringBuilder.append(classConstructor);
                                
        return stringBuilder.toString();
    }

    /**
     * Pass the content of the method to Ollir's notation
     * @param methodNode node to visit referring a method
     * @param unused
     * @return
     */
    private String dealWithMethod(JmmNode methodNode, Void unused) {

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

        stringBuilder.append(methodDec).append(methodParam).append(methodRet);

        return stringBuilder.toString();
    }

    /**
     * Pass the content of when an assignment is made to Ollir's notation
     * @param equalNode node to visit referring an assignment
     * @param unused
     * @return
     */
    private String dealWithEquals(JmmNode equalNode, Void unused) {

        StringBuilder assign = new StringBuilder("\t");


        JmmNode lhs = equalNode.getChildren().get(0);
        JmmNode rhs = equalNode.getChildren().get(1);

        // For the variable that stores the value
        assign.append(this.resolveVariableIdentifier(lhs));

        String assignmentType = ".i32 "; // special case where destination is array access
        if (!lhs.getKind().equals("Array")) {
            Symbol lhsSymbol = this.st.getVariableSymbol(lhs);
            assignmentType = "." +  Utils.getOllirType(lhsSymbol.getType()) + " "; // if not array access, then fetch type
        }
        assign.append(" :=").append(assignmentType);

        // For the value to be saved
        assign.append(this.handleRhsAssign(rhs, assign)).append(";");

        return assign.toString();
    }

    /**
     * Pass the content of when an assignment is made to Ollir's notation
     * @param rhs node to visit referring the content that follows the equal sign
     * @param builder an object to add content with Ollir's notation
     * @return
     */
    private String handleRhsAssign(JmmNode rhs, StringBuilder builder) {
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
                String methodCall = this.handleMethodCall(rhs, auxExpressions);
                
                // add on top
                if (auxExpressions.size() > 1) {

                    for (int i = 0; i < auxExpressions.size()-1; i++) {
                        builder.insert(i, "\t");
                        builder.insert(i, auxExpressions.get(i) + ";\n");
                    }

                }

                rhsBuilder.append(methodCall);
                break;
            default:
                // In the case it is an expression
                if (Utils.isOperator(rhs)) {
                    List<String> expr = new ArrayList<>();
                   String rhsExpr =  this.dealWithExpression(rhs, 0, expr, null);

                    if (!expr.isEmpty()) {

                        for (int i = 0; i < expr.size()-1; i++) {
                            builder.insert(i, "\t");
                            builder.insert(i, expr.get(i) + ";\n");
                        }

                        String lastExpr = expr.get(expr.size()-1);
                        lastExpr = lastExpr.split("=")[1];
                        rhsExpr = lastExpr;
                    }

                    rhsBuilder.append(rhsExpr);
                }
                
                    // TODO call operation
                System.out.println("Olha morri _________________________");
        }
        return rhsBuilder.toString();
    }

    /**
     * Pass the content of when an if logical condition is made to Ollir's notation
     * @param ifNode node to visit referring an if logical condition
     * @param unused
     * @return
     */
    private String dealWithIf(JmmNode ifNode, Void unused) {

        StringBuilder ifBuilder = new StringBuilder();

        List<String> expr = new ArrayList<>();
        this.dealWithExpression(ifNode.getChildren().get(0), 0, expr, null);

        System.out.println(expr);
        
        for (int i = 0; i < expr.size()-1; i++) {
            ifBuilder.append("\t");
            ifBuilder.append(expr.get(i)).append(";").append("\n");
        }
        ifBuilder.append("\t if (");

        String lastExpr = expr.get(expr.size()-1);
        lastExpr = lastExpr.split("=")[1];

        ifBuilder.append(lastExpr).append(") got to else;");

        return ifBuilder.toString();
    }


    /**
     * Saves the content of when an expression is made with the Ollir's notation
     * @param expr node to visit referring an expression
     * @param level to track the current level of the expression tree
     * @param expressions list of strings that stores in Ollir's notation possible auxiliary variables of the main expression
     * @param createdVars auxiliar list
     * @return
     */
    // TODO: at lower levels create auxiliar variables
    private String dealWithExpression(JmmNode expr, int level, List<String> expressions, Map<String, String> createdVars) {

        if (createdVars == null)
            createdVars = new HashMap<>();

        if (Utils.isOperator(expr)) {

            // Special case where operator is unary
            if (expr.getKind().equals("Negation")) {

                JmmNode child = expr.getChildren().get(0);

                // Go to the next level in the tree
                level++;
                String innerNegation = dealWithExpression(child, level, expressions, createdVars);

                String t = "", ident = "";
                if (level > 1) {
                    ident = "t" + (createdVars.size() + 1) + Utils.getOllirExpReturnType(expr.getKind());
                    t = ident + " = ";
                    createdVars.put(ident, "....");
                }
                 
                expressions.add(t + Utils.getOllirOp(expr.getKind()) + " " + innerNegation); // TODO revese expression
                return ident; 
            }
            else {
                JmmNode lhsNode = expr.getChildren().get(0);
                JmmNode rhsNode = expr.getChildren().get(1);

                level++;
                String lhsExpr = dealWithExpression(lhsNode, level, expressions, createdVars);
                level++;
                String rhsExpr = dealWithExpression(rhsNode, level, expressions, createdVars);

                String t = "", ident = "";
                if (level > 1) {
                    ident = "t" + (createdVars.size() + 1) + Utils.getOllirExpReturnType(expr.getKind());
                    t = ident + " = ";
                    createdVars.put(ident, "....");
                }

                expressions.add(t + lhsExpr + " " + Utils.getOllirOp(expr.getKind()) + " " + rhsExpr); // TODO determmine type of operator

                return ident;
            }
        }
        // The remaining options are terminal
        else if (expr.getKind().equals("Literal")) {
            return Utils.getOllirLiteral(expr);
        }
        else if (expr.getKind().equals("MethodCall")) {
        //    this.handleMethodCall(expr);
        }
        else {
            return resolveVariableIdentifier(expr);
        }

        return "";
    }

    public String handleMethodCall(JmmNode methodNode, List<String> auxExpressions) {

        StringBuilder methodBuilder = new StringBuilder();

        JmmNode firstChild = methodNode.getChildren().get(0);
        JmmNode secondChild = methodNode.getChildren().get(1);

        
        switch (firstChild.getKind()) {
            case "This":
                methodBuilder.append("invokevirtual(this, ").append('"' + methodNode.get("name")  + '"');
                methodBuilder.append(", ").append(this.handleMethodParameters(secondChild, auxExpressions)).append(")"); //Arguments
                break;
            case "Ident":
                methodBuilder.append("invokestatic(").append('"' + firstChild.get("name")  + '"').append('"' + methodNode.get("name") + '"');
                methodBuilder.append(", ").append(this.handleMethodParameters(secondChild, auxExpressions)).append(")"); //Arguments
                break;
            default:
                break;
        }
    
        return methodBuilder.toString();
    }

    public String handleMethodParameters(JmmNode paramsNode, List<String> auxExpressions) {

        StringBuilder paramsBuilder = new StringBuilder();

        for (JmmNode child : paramsNode.getChildren()) {
            List<String> expr = new ArrayList<>();
            String param = this.dealWithExpression(child, 0, expr, null); 
            
            if (expr.size() > 1) {

                for (int i = 0; i < expr.size()-1; i++) {
                    String aux = "\t" + expr.get(i) + ";\n";
                    auxExpressions.add(aux);
                }

                param = expr.get(expr.size()-1);
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

    private static String reduce(JmmNode node, String nodeResult, List<String> childrenResults) {
        var content = new StringBuilder();

        boolean validNode = !nodeResult.isBlank(); // don't add newlines to ignored nodes

        if (validNode)
            content.append("\n");
        content.append(nodeResult);

        for (String childResult : childrenResults) {
            content.append(childResult);
        }

        String tab = "";
        if (node.getKind().equals("Method"))
            tab = "\n\t}\n";
        else if (node.getKind().equals("If")) {

        }

        return content + tab;
    }

    @Override
    public String visit(JmmNode jmmNode, Void unused) {
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
