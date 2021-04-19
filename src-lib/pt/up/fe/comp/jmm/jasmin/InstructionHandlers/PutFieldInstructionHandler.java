package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;

public class PutFieldInstructionHandler implements IntructionHandler{
    private PutFieldInstruction put;

    public PutFieldInstructionHandler(Instruction putFieldInstruction) {
        this.put = (PutFieldInstruction) putFieldInstruction;
    }

    @Override
    public String handleInstruction() {
        return "\tputfield";
    }

}
