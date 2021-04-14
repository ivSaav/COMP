import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public MethodSymbols getMethod(String methodName) {
        return methods.get(methodName);
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void addMethod(MethodSymbols symbols) {
        this.methods.put(symbols.getName(), symbols);
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
            MethodSymbols symbols = this.methods.get(parent.get("name"));
            symbols.addLocalVar(name, symbol);
        }
    }

    public Map<String, Symbol> getVariables(String method) {
        Map<String, Symbol> allVariables = new HashMap<>();

        // Put all global variables
        allVariables.putAll(this.fields);

        // Put all local variables from the method
        MethodSymbols methodSymbols = this.methods.get(method);
        allVariables.putAll(methodSymbols.getLocalVar());
        allVariables.putAll(methodSymbols.getParameter());

        return allVariables;
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
