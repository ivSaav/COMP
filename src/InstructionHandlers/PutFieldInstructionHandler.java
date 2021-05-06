package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

public class PutFieldInstructionHandler implements IntructionHandler{
    private PutFieldInstruction put;

    public PutFieldInstructionHandler(Instruction putFieldInstruction) {
        this.put = (PutFieldInstruction) putFieldInstruction;
    }

    @Override
    public String handleInstruction(String className,Method method) {

        StringBuilder string = new StringBuilder();
        Element third = put.getThirdOperand();

        MyJasminUtils.checkLiteralOrOperand(method, string, third);

        string.append("\tputfield ");

        String first = MyJasminUtils.getElementName(put.getFirstOperand());
        if (first.equals("this")) first= className;

        String second = MyJasminUtils.getElementName(put.getSecondOperand());


        string.append(first +"/"+second +" "+ MyJasminUtils.parseType(put.getSecondOperand().getType().getTypeOfElement()));

        return string.toString()+"\n";
    }

}
