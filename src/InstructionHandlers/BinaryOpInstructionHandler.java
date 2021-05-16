package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.Locale;

public class BinaryOpInstructionHandler implements IntructionHandler{

    private BinaryOpInstruction instruction;

    public BinaryOpInstructionHandler(Instruction instruction) {
        this.instruction = (BinaryOpInstruction) instruction;
    }


    @Override
    public String handleInstruction(ClassUnit classUnit ,Method method) {
        StringBuilder string = new StringBuilder();

        OperationType opType = instruction.getUnaryOperation().getOpType();
        Element rop = instruction.getRightOperand();
        Element lop = instruction.getLeftOperand();

        //load or lcd operands to stack
        if (!MyJasminUtils.isLoaded(rop, this.instruction.getPred()))
            MyJasminUtils.loadElement(method, string, lop);
        if (!MyJasminUtils.isLoaded(lop, this.instruction.getPred()))
            MyJasminUtils.loadElement(method, string, rop);

        if (opType == OperationType.LTH) {
            string.append("\tisub\n");
        }

        else {
            string.append("\t" + MyJasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            string.append(MyJasminUtils.parseInstruction(opType) + "\n");
        }

        return string.toString();
    }

  
}
