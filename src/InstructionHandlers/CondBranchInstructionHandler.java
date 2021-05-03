package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class CondBranchInstructionHandler implements IntructionHandler{
    private CondBranchInstruction condBranchInstruction;

    public CondBranchInstructionHandler(Instruction branchInstruction) {

        this.condBranchInstruction = (CondBranchInstruction) branchInstruction;
    }

    public String handleInstruction(String className, Method method) {
        HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);
        String label=condBranchInstruction.getLabel();
        StringBuilder string = new StringBuilder();

        Element lop = condBranchInstruction.getLeftOperand();
        Element rop = condBranchInstruction.getRightOperand();

        if (rop.isLiteral()){
            LiteralElement literal = (LiteralElement) rop;
            string.append("\tldc "+ literal.getLiteral()+"\n");

        }else{
            string.append("\t"+JasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            Operand variable = (Operand) rop;
            Descriptor d = vars.get(variable.getName());
            string.append("load "+ d.getVirtualReg()+"\n");
        }

        if (lop.isLiteral()){
            LiteralElement literal = (LiteralElement) lop;
            string.append("\tldc "+ literal.getLiteral() +"\n");
        }else{
            string.append("\t"+JasminUtils.parseType(lop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            Operand variable = (Operand) lop;
            Descriptor d = vars.get(variable.getName());
            string.append("load "+ d.getVirtualReg()+"\n");
        }

        if (lop.getType().getTypeOfElement()==ElementType.INT32){
            string.append("\tisub\n");
        }else{
           string.append("\t"+JasminUtils.parseType(lop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT)+"cmp\n");
        }

        string.append("\tif");
        string.append(JasminUtils.parseOperationType(condBranchInstruction.getCondOperation().getOpType()));
        string.append(" "+ label +"\n");

        return string.toString();
    }
    
}
