package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.BinaryOpInstruction;
import org.specs.comp.ollir.Instruction;

public class BinaryOpInstructionHandler implements IntructionHandler{

    private BinaryOpInstruction instruction;

    public BinaryOpInstructionHandler(Instruction instruction) {
        this.instruction = (BinaryOpInstruction) instruction;
    }


    @Override
    public String handleInstruction() {
        return null;
    }
}
