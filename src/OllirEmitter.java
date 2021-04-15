import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

public class OllirEmitter extends AJmmVisitor {

    private SymbolTable st;

    public OllirEmitter(SymbolTable st) {
        this.st = st;
    }
}
