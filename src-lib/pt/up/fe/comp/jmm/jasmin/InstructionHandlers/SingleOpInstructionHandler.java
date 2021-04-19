package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;

import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.UnaryOpInstruction;

public class SingleOpInstructionHandler implements IntructionHandler{

    private SingleOpInstruction singleOpInstruction;

    public SingleOpInstructionHandler(Instruction single) {

        this.singleOpInstruction = (SingleOpInstruction) single;
    }


    public String handleInstruction() {
        return "";
    }
}
