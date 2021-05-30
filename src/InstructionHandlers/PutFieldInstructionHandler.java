package InstructionHandlers;

import org.specs.comp.ollir.*;

public class PutFieldInstructionHandler implements IntructionHandler{
    private PutFieldInstruction put;

    public PutFieldInstructionHandler(Instruction putFieldInstruction) {
        this.put = (PutFieldInstruction) putFieldInstruction;
    }

    @Override
    public String handleInstruction(ClassUnit classUnit ,Method method) {
        String className = classUnit.getClassName();
        StringBuilder string = new StringBuilder();

        String first, second;

        if (!this.wasLoaded()) {
            string.append("\taload_0\n");
            MyJasminUtils.loadElement(method, string, put.getThirdOperand());
        }

        string.append("\tputfield ");

        first = MyJasminUtils.getElementName(put.getFirstOperand());
        if (first.equals("this")) first= className;

        second = MyJasminUtils.getElementName(put.getSecondOperand());


        string.append(first +"/"+second +" "+ MyJasminUtils.parseType(put.getSecondOperand().getType().getTypeOfElement()));

        return string + "\n";
    }

    private boolean wasLoaded() {
        if (put.getThirdOperand().isLiteral()) // putfield(this, var, 0)
            return false;

        for (Node pred : this.put.getPred()) {
            // ignore method definition
            if (pred.getNodeType() == NodeType.BEGIN)
                return false;
            Instruction inst = (Instruction) pred;
            if (inst.getInstType() != InstructionType.ASSIGN)
                return false;
        }
        return true;
    }

}
