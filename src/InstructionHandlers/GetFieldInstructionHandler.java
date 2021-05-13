package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

public class GetFieldInstructionHandler implements IntructionHandler{

    private GetFieldInstruction getFieldInstruction;

    public GetFieldInstructionHandler(Instruction getFieldInstruction) {
        this.getFieldInstruction = (GetFieldInstruction) getFieldInstruction;
    }


    public String handleInstruction(ClassUnit classUnit,Method method) {
        String className = classUnit.getClassName();
        StringBuilder string = new StringBuilder();

        string.append("\taload 0\n");
        string.append("\tgetfield ");

        String first = MyJasminUtils.getElementName(getFieldInstruction.getFirstOperand());
        if (first.equals("this")) first= className;

        String second = MyJasminUtils.getElementName(getFieldInstruction.getSecondOperand());

        string.append(first +"/"+second +" "+ MyJasminUtils.parseType(getFieldInstruction.getSecondOperand().getType().getTypeOfElement()));

        return string+"\n";
    }


}
