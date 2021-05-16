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

        Element rop = instruction.getRightOperand();
        Element lop = instruction.getLeftOperand();

        //load or lcd operands to stack
        if (!MyJasminUtils.isLoaded(rop, this.instruction.getPred()))
            MyJasminUtils.loadElement(method, string, lop);
        if (!MyJasminUtils.isLoaded(lop, this.instruction.getPred()))
            MyJasminUtils.loadElement(method, string, rop);

        if (instruction.getUnaryOperation().getOpType() == OperationType.LTH) {
            string.append("\tisub\n");
        }

        System.out.println("METHOD " + method.getMethodName() + " OP " + instruction.getUnaryOperation().getOpType());

        string.append("\t"+ MyJasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
        string.append(MyJasminUtils.parseIInstruction(instruction.getUnaryOperation().getOpType())+"\n");

        return string.toString();
    }

  
}
