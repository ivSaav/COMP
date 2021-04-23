package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.GetFieldInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;

public class GetFieldInstructionHandler implements IntructionHandler{

    private GetFieldInstruction getFieldInstruction;

    public GetFieldInstructionHandler(Instruction getFieldInstruction) {
        //super(getFieldInstruction);
        this.getFieldInstruction = (GetFieldInstruction) getFieldInstruction;
    }


    public String handleInstruction(Method method) {
        return "\tgetfield ";
    }
}
