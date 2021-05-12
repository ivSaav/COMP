package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.Locale;

public class CondBranchInstructionHandler implements IntructionHandler{
    private CondBranchInstruction condBranchInstruction;

    public CondBranchInstructionHandler(Instruction branchInstruction) {

        this.condBranchInstruction = (CondBranchInstruction) branchInstruction;
    }

    public String handleInstruction(String className, Method method) {
        String label=condBranchInstruction.getLabel();
        StringBuilder string = new StringBuilder();

        Element lop = condBranchInstruction.getLeftOperand();
        Element rop = condBranchInstruction.getRightOperand();


        //load or lcd operands to stack
        MyJasminUtils.loadElement(method, string, rop);
        MyJasminUtils.loadElement(method, string, lop);

        //TODO
        if (lop.getType().getTypeOfElement()==ElementType.INT32){
            string.append("\tisub\n");
        }else{
           string.append("\t"+MyJasminUtils.parseType(lop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT)+"cmp\n");
        }

        string.append("\tif");
        string.append(MyJasminUtils.parseOperationType(condBranchInstruction.getCondOperation().getOpType()));
        string.append(" "+ label +"\n");

        return string.toString();
    }
    
}
