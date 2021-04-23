package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.CondBranchInstruction;
import org.specs.comp.ollir.GetFieldInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;

public class BranchInstructionHandler implements IntructionHandler{
    private CondBranchInstruction condBranchInstruction;

    public BranchInstructionHandler(Instruction branchInstruction) {

        this.condBranchInstruction = (CondBranchInstruction) branchInstruction;
    }


    public String handleInstruction(Method method) {

        //Operation operation = condBranchInstruction.getCondOperation();

        //TODO CAST
        //Operand operand = (Operand) condBranchInstruction.getLeftOperand();
        //LiteralElement literalElement = (LiteralElement) condBranchInstruction.getRightOperand();

        //TODO CHECK IF OPERANDS

        //jasminCode.append("\tif" + parseOperationType(operation.getOpType())+" "+ condBranchInstruction.getLabel()+"\n");
        return "";
    }
    
}
