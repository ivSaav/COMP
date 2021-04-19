package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.GetFieldInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.UnaryOpInstruction;

public class UnaryOpInstructionHandler implements IntructionHandler{
    private UnaryOpInstruction unaryOpInstruction;

    public UnaryOpInstructionHandler(Instruction unary) {

        this.unaryOpInstruction = (UnaryOpInstruction) unary;
    }


    public String handleInstruction() {
        return "";
    }
}
