package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

public class GetFieldInstructionHandler implements IntructionHandler{

    private GetFieldInstruction getFieldInstruction;

    public GetFieldInstructionHandler(Instruction getFieldInstruction) {
        //super(getFieldInstruction);
        this.getFieldInstruction = (GetFieldInstruction) getFieldInstruction;
    }


    public String handleInstruction(String className,Method method) {
        StringBuilder string = new StringBuilder();
        getFieldInstruction.getFirstOperand();

        string.append("\tgetfield ");

        String first = "";
        if(getFieldInstruction.getFirstOperand().isLiteral()){
            LiteralElement literal  =  (LiteralElement) getFieldInstruction.getFirstOperand();
            first = literal.getLiteral();
        }else {
            Operand op1= (Operand) getFieldInstruction.getFirstOperand();
            first = op1.getName();
        }

        if (first.equals("this")) first= className;

        String second = "";
        if(getFieldInstruction.getSecondOperand().isLiteral()){
            LiteralElement literal  =  (LiteralElement) getFieldInstruction.getSecondOperand();
            second = literal.getLiteral();
        }else {
            Operand op1= (Operand) getFieldInstruction.getSecondOperand();
            second = op1.getName();
        }

        string.append(first +"/"+second +" "+ JasminUtils.parseType(getFieldInstruction.getSecondOperand().getType().getTypeOfElement()));

        return string.toString()+"\n";
    }
}
