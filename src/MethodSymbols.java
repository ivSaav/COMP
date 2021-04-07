import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodSymbols {
    private String name;
    private Type returnType;
    private Map<String, Symbol> parameters; //name --> symbol
    private Map<String, Symbol> localVars; // name --> symbol

    public MethodSymbols(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = new HashMap<>();
        this.localVars = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return new ArrayList<Symbol>(this.parameters.values());
    }

    public List<Symbol> getLocalVars() {
        return new ArrayList<Symbol>(this.localVars.values());
    }

    public void addParameter(String name, Symbol symbol) {
        this.parameters.put(name, symbol);
    }

    public void addLocalVar(String name, Symbol symbol) {
        this.localVars.put(name, symbol);
    }
}
