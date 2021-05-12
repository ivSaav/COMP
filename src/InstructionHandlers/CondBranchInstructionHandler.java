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

        for (Node n : this.condBranchInstruction.getPred()) {
            System.out.println("PRED " + ((Instruction) n).getInstType());
        }

        //load or lcd operands to stack
//        if (!MyJasminUtils.isLoaded(rop, this.condBranchInstruction.getPred()))
            MyJasminUtils.loadElement(method, string, rop);
//        if (!MyJasminUtils.isLoaded(lop, this.condBranchInstruction.getPred()))
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
