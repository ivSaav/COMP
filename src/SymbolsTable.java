import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SymbolsTable implements SymbolTable {

    private final List<String> imports;
    private String className;
    private String superclass;
    private final Map<String, Symbol> fields; //name --> class attributes
    private final Map<String, MethodSymbols> methods; // name --> MethodSymbols

    public SymbolsTable() {
        this.imports = new ArrayList<>();
        this.className = "";
        this.superclass = "";
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();
    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.superclass;
    }

    @Override
    public List<Symbol> getFields() {
        return new ArrayList<>(this.fields.values());
    }

    public Map<String, Symbol> getField() { return this.fields; }

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(this.methods.keySet());
    }

    public Type getReturnType(String methodName) {
        MethodSymbols ms = this.methods.get(methodName);
        return ms != null ? ms.getReturnType() : null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        MethodSymbols ms = this.methods.get(methodName);
        return ms != null ? ms.getParameters() : null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        MethodSymbols ms = this.methods.get(methodName);
        return ms != null ? ms.getLocalVars() : null;
    }

    public Symbol getGlobalVariable(String varName) {
        return this.fields.get(varName);
    }

    public MethodSymbols getMethod(JmmNode methodNode) {
        String key = this.createMethodKey(methodNode);
        return methods.get(key);
    }



    public void setClassName(String className) {
        this.className = className;
    }

    public void addMethod(MethodSymbols symbols) {
        String key = symbols.getName() + symbols.getNumParams();
        this.methods.put(key, symbols);
    }

    public void setSuperclass(String superclass) {
        this.superclass = superclass;
    }

    public void addImport(String className) {
        this.imports.add(className);
    }

    public void addVariable(JmmNode parent, String name, Symbol symbol) {

        if (parent.getKind().equals("Class"))
            this.fields.put(name, symbol);
        else if (parent.getKind().equals("Method")){

            MethodSymbols symbols = this.methods.get(this.createMethodKey(parent));
            symbols.addLocalVar(name, symbol);
        }
    }

    public Map<String, Symbol> getVariables(JmmNode method) {

        String key = this.createMethodKey(method);
        Map<String, Symbol> allVariables = new HashMap<>();

        // Put all global variables
        allVariables.putAll(this.fields);

        // Put all local variables from the method
        MethodSymbols methodSymbols = this.methods.get(key);
        allVariables.putAll(methodSymbols.getLocalVar());
        allVariables.putAll(methodSymbols.getParameterMap());

        return allVariables;
    }

    public String createMethodKey(JmmNode method) {
        JmmNode params = Utils.getChildOfKind(method, "Parameters");
        if (params == null)
            params = Utils.getChildOfKind(method, "Arguments");

        return method.get("name") + params.getNumChildren();
    }

    /**
     * Fetches variable's symbol from local or global variables
     * @param var
     * @return
     */
    public Symbol getVariableSymbol(JmmNode var) {
        String name = var.get("name");

        // Check in global variables
        Symbol varSymbol = this.getGlobalVariable(name);

        // Wasn't in global variables
        if (varSymbol == null) {
            JmmNode scope = Utils.findScope(var); // determine method where variable is declared
            if (scope != null) // fetch symbol from method (local or in parameters)
                varSymbol = this.getMethod(scope).getVariable(var.get("name"));
        }

        return varSymbol;
    }

    public boolean isGlobalVar(String varIdent) {
        return this.fields.containsKey(varIdent);
    }

    public boolean isGlobalVar(JmmNode varNode) {
        String varName = "";
        if (varNode.getKind().equals("Array")) {
            JmmNode arrayIdent = varNode.getChildren().get(0);
            varName = arrayIdent.get("name");
        }
        else
            varName = varNode.get("name");

        return this.fields.containsKey(varName);
    }

    public boolean isClassMethod(String methodName, int numArgs) {
        return this.methods.containsKey(methodName + numArgs);
    }

    @Override
    public String print() {
        StringBuilder start = new StringBuilder(SymbolTable.super.print());

        for (MethodSymbols methodSymbols : this.methods.values()) {
            start.append("\n==== ");
            start.append(methodSymbols.getName());
            start.append(" ====");

            start.append("\nLocals: \n");

            for (Symbol symb : methodSymbols.getLocalVars()) {
                String repr = symb.getType().print() + " " + symb.getName();
                start.append(" - ").append(repr).append("\n");
            }

        }

        return start.toString();
    }

    @Override
    public String toString() {
        return "SymbolsTable{" +
                "imports=" + imports +
                ", className='" + className + '\'' +
                ", superclass='" + superclass + '\'' +
                ", fields=" + fields +
                ",\n methods=" + methods +
                '}';
    }

}
