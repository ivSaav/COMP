package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.Locale;

public class BinaryOpInstructionHandler implements IntructionHandler{

    private BinaryOpInstruction instruction;

    public BinaryOpInstructionHandler(Instruction instruction) {
        this.instruction = (BinaryOpInstruction) instruction;
    }


    @Override
    public String handleInstruction(String className,Method method) {
        StringBuilder string = new StringBuilder();

        Element rop = instruction.getRightOperand();
        Element lop = instruction.getLeftOperand();

        //load or lcd operands to stack
        MyJasminUtils.checkLiteralOrOperand(method, string, rop);
        MyJasminUtils.checkLiteralOrOperand(method, string, lop);

        string.append("\t"+ MyJasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
        string.append(MyJasminUtils.parseOperationType(instruction.getUnaryOperation().getOpType())+"\n");

        return string.toString();
    }

  
}
