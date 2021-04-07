import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolsTable implements SymbolTable {

    private List<String> imports;
    private String className;
    private String superclass;
    private Map<String, Symbol> fields;
    private Map<String, MethodSymbols> methods;

    public SymbolsTable(List<String> imports, String className, String superclass) {
        this.imports = imports;
        this.className = className;
        this.superclass = superclass;
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

    @Override
    public List<String> getMethods() {
        return new ArrayList<>(this.methods.keySet());
    }

    @Override
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
}
