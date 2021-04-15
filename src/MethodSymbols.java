import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodSymbols {
    private final String name;
    private final Type returnType;
    private final Map<String, Symbol> parameters; //name --> symbol
    private final Map<String, Symbol> localVars; // name --> symbol

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

    public Map<String, Symbol> getParameter() { return this.parameters; }

    public List<Symbol> getLocalVars() {
        return new ArrayList<Symbol>(this.localVars.values());
    }

    public Map<String, Symbol> getLocalVar() { return this.localVars; }

    public void addParameter(String name, Symbol symbol) {
        this.parameters.put(name, symbol);
    }

    public void addLocalVar(String name, Symbol symbol) {
        this.localVars.put(name, symbol);
    }

    public void addParameters(JmmNode paramNode) {

        for (JmmNode param : paramNode.getChildren()) {
            JmmNode paramType = param.getChildren().get(0);
            if (paramType.getKind().equals("Type")) {
                Type type = new Type(paramType.get("type"), Boolean.parseBoolean(paramType.get("isArray")));
                Symbol symbol = new Symbol(type, param.get("name"));
                this.parameters.put(param.get("name"), symbol);
            }
        }
    }

    public boolean containsVariable(String varName) {
        return this.localVars.containsKey(varName);
    }

    public Symbol getVariable(String varName) {
        Symbol symbol = this.localVars.get(varName);
        return symbol == null ? this.parameters.get(varName) : symbol;
    }

    @Override
    public String toString() {
        return "\nMethodSymbols{" +
                "name='" + name + '\'' +
                ", returnType=" + returnType +
                ", parameters=" + parameters +
                ",\n\t localVars=" + localVars +
                '}';
    }
}
