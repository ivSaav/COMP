package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.Locale;

public class UnaryOpInstructionHandler implements IntructionHandler{
    private UnaryOpInstruction unaryOpInstruction;

    public UnaryOpInstructionHandler(Instruction unary) {
        this.unaryOpInstruction = (UnaryOpInstruction) unary;
    }

    public String handleInstruction(String className, Method method) {
        StringBuilder string = new StringBuilder();
        Element rop = unaryOpInstruction.getRightOperand();

        MyJasminUtils.loadElement(method, string, rop);

        string.append("\t"+ MyJasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
        string.append(MyJasminUtils.parseOperationType(unaryOpInstruction.getUnaryOperation().getOpType())+"\n");

        return string.toString();
    }
}
