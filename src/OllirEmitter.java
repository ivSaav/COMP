import java.util.ArrayList;
import java.util.List;
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
        // addVisit("If", this::dealWithIf);
        // addVisit("While", this::dealWithWhile);
        // addVisit("Plus", this::dealWithLogicalOp);
        // addVisit("And", this::dealWithArithmeticOp);
    }



    public String defaultVisit(JmmNode node, Void unused) {
        return "";
    }
    
    private String dealWithClass(JmmNode classNode, Void unused) {
        StringBuilder stringBuilder = new StringBuilder(classNode.get("class"));
        
        stringBuilder.append("{\n");
        String classConstructor = "\t.constructor " + classNode.get("class") +"().V {\n"
                                +       "\t\tinvokespecial(this, \"<init>\").V;\n"
                                + "\t}";
        stringBuilder.append(classConstructor);
                                
        return stringBuilder.toString();
    }

    private String dealWithMethod(JmmNode methodNode, Void unused) {
        StringBuilder stringBuilder = new StringBuilder();

        MethodSymbols methodSymbols = this.st.getMethod(methodNode.get("name"));

        String methodDec = "\t.method public " + methodSymbols.getName();
        String methodParam = "(";
        
        for (int i = 0; i < methodSymbols.getParameters().size(); i++) {
            Symbol symbol = methodSymbols.getParameters().get(i);

            // Last iteraction
            if (i == methodSymbols.getParameters().size() - 1) {
                methodParam += Utils.getOllirVar(symbol) + ")";
                continue;
            }
            
            methodParam += Utils.getOllirVar(symbol) + ",";
        }

        Type retType = methodSymbols.getReturnType();
        String methodRet = "." + Utils.getOllirType(retType) + " {\n";

        stringBuilder.append(methodDec);
        stringBuilder.append(methodParam);
        stringBuilder.append(methodRet);

        return stringBuilder.toString();
    }

    private String dealWithEquals(JmmNode equalNode, Void unused) {

        StringBuilder assign = new StringBuilder("\t");

        JmmNode lhs = equalNode.getChildren().get(0);
        JmmNode rhs = equalNode.getChildren().get(1);

        assign.append(this.resolveVariableIdentifier(lhs));

        String assignmentType = ".i32 "; // special case where destination is array access
        if (!lhs.getKind().equals("Array")) {
            Symbol lhsSymbol = this.st.getVariableSymbol(lhs);
             assignmentType = "." +  Utils.getOllirType(lhsSymbol.getType()) + " "; // if not array access, then fetch type
        }
        assign.append(" :=").append(assignmentType);

        assign.append(this.handleRhsAssign(rhs)).append(";");
        return assign.toString();
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

    private String handleRhsAssign(JmmNode rhs) {
        StringBuilder rhsBuilder = new StringBuilder();
        switch (rhs.getKind()) {
            case "Literal":
                rhsBuilder.append(Utils.getOllirLiteral(rhs));
                break;
            case "Ident":
            case "Array":
                rhsBuilder.append(this.resolveVariableIdentifier(rhs));
                break;
            default:
                if (Utils.isOperator(rhs))
                    // TODO call operation
                System.out.println("Olha morri _________________________");
        }
        return rhsBuilder.toString();
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
