package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.Locale;

public class UnaryOpInstructionHandler implements IntructionHandler{
    private UnaryOpInstruction unaryOpInstruction;

    public UnaryOpInstructionHandler(Instruction unary) {
        this.unaryOpInstruction = (UnaryOpInstruction) unary;
    }

    public String handleInstruction(ClassUnit classUnit, Method method) {
        StringBuilder string = new StringBuilder();
        Element rop = unaryOpInstruction.getRightOperand();

        MyJasminUtils.loadElement(method, string, rop);

        // Case
        if (unaryOpInstruction.getUnaryOperation().getOpType() == OperationType.NOTB)
            string.append("\tldc 1\n");

        string.append("\t"+ MyJasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));

        string.append(MyJasminUtils.parseOperationType(unaryOpInstruction.getUnaryOperation().getOpType())+"\n");

        return string.toString();
    }
}
