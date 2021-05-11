package InstructionHandlers;

import org.specs.comp.ollir.*;

import org.specs.comp.ollir.Instruction;

public class SingleOpInstructionHandler implements IntructionHandler{

    private final SingleOpInstruction singleOpInstruction;

    public SingleOpInstructionHandler(Instruction single) {

        this.singleOpInstruction = (SingleOpInstruction) single;
    }

    public String handleInstruction(String className, Method method) {
        StringBuilder string = new StringBuilder();
        Element op = singleOpInstruction.getSingleOperand();

        MyJasminUtils.checkLiteralOrOperand(method, string, op);

        return string.toString();
    }


}
