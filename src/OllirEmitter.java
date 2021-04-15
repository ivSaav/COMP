import java.util.ArrayList;
import java.util.List;

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
        // addVisit("Equal", this::dealWithEquals);
        // addVisit("If", this::dealWithIf);
        // addVisit("While", this::dealWithWhile);
        // addVisit("Plus", this::dealWithLogicalOp);
        // addVisit("And", this::dealWithArithmeticOp);
    }

    public String defaultVisit(JmmNode node, Void unused) {
        return "";
    }
    
    public String dealWithClass(JmmNode classNode, Void unused) {
        StringBuilder stringBuilder = new StringBuilder(classNode.get("class"));
        
        stringBuilder.append("{\n");
        String classConstructor = "\t.constructor " + classNode.get("class") +"().V {\n"
                                +       "\t\tinvokespecial(this, \"<init>\").V;\n"
                                + "\t}";
        stringBuilder.append(classConstructor);
                                
        return stringBuilder.toString();
    }

    public String dealWithMethod(JmmNode methodNode, Void unused) {
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
    
    

    private static String reduce(String nodeResult, List<String> childrenResults) {
        var content = new StringBuilder();

        if (!nodeResult.isBlank()) 
            content.append("\n");
        content.append(nodeResult);

        for (String childResult : childrenResults) {
            content.append(childResult);
        }

        return content.toString();
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

        return reduce(nodeResult, childrenResults);
    }
}
