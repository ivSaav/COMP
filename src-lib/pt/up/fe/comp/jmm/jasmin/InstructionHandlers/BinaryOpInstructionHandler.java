package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class BinaryOpInstructionHandler implements IntructionHandler{

    private BinaryOpInstruction instruction;

    public BinaryOpInstructionHandler(Instruction instruction) {
        this.instruction = (BinaryOpInstruction) instruction;
    }


    @Override
    public String handleInstruction(Method method) {
        StringBuilder string = new StringBuilder();
        HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);

        Element rop = instruction.getRightOperand();
        Element lop = instruction.getLeftOperand();

        if (rop.isLiteral()){
            LiteralElement literal = (LiteralElement) rop;
            string.append("\tldc "+ literal.getLiteral()+"\n");

        }else{
            string.append("\t"+JasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            Operand variable = (Operand) instruction.getRightOperand();
            Descriptor d = vars.get(variable.getName());
            string.append("load "+ d.getVirtualReg()+"\n");
        }

        if (lop.isLiteral()){
            LiteralElement literal = (LiteralElement) lop;
            string.append("\tldc "+ literal.getLiteral() +"\n");
            //string.append(op.);
        }else{
            string.append("\t"+JasminUtils.parseType(lop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            Operand variable = (Operand) lop;
            Descriptor d = vars.get(variable.getName()); //d is null?
            string.append("load "+ d.getVirtualReg()+"\n");
        }

        string.append("OP:" + instruction.getUnaryOperation().getOpType() + "\n");




        return string.toString();
    }

  
}