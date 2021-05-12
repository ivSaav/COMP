package InstructionHandlers;

import org.specs.comp.ollir.*;

public class PutFieldInstructionHandler implements IntructionHandler{
    private PutFieldInstruction put;

    public PutFieldInstructionHandler(Instruction putFieldInstruction) {
        this.put = (PutFieldInstruction) putFieldInstruction;
    }

    @Override
    public String handleInstruction(String className,Method method) {

        StringBuilder string = new StringBuilder();
//        Element third = put.getThirdOperand();

        String first = MyJasminUtils.getElementName(put.getFirstOperand());
        String second = MyJasminUtils.getElementName(put.getSecondOperand());
        String third = MyJasminUtils.getElementName(put.getThirdOperand());

        System.out.println("PUTFIELD ===\n " + first + " " + second + " " + third);

//        if (!MyJasminUtils.isLoaded(put.getSecondOperand(), this.put.getPred()))
            MyJasminUtils.loadElement(method, string, put.getSecondOperand());
        if (!MyJasminUtils.isLoaded(put.getThirdOperand(), this.put.getPred()))
        MyJasminUtils.loadElement(method, string, put.getThirdOperand());

        string.append("\tputfield ");

        first = MyJasminUtils.getElementName(put.getFirstOperand());
        if (first.equals("this")) first= className;

        second = MyJasminUtils.getElementName(put.getSecondOperand());


        string.append(first +"/"+second +" "+ MyJasminUtils.parseType(put.getSecondOperand().getType().getTypeOfElement()));

        return string.toString()+"\n";
    }

}
