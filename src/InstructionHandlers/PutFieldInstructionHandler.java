package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class PutFieldInstructionHandler implements IntructionHandler{
    private PutFieldInstruction put;

    public PutFieldInstructionHandler(Instruction putFieldInstruction) {
        this.put = (PutFieldInstruction) putFieldInstruction;
    }

    @Override
    public String handleInstruction(String className,Method method) {
        HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);
        StringBuilder string = new StringBuilder();
        Element third = put.getThirdOperand();

        if (third.isLiteral()){
            string.append("\tldc "+ ((LiteralElement)third).getLiteral() +"\n");

        }else {
            string.append("\t"+JasminUtils.parseType(third.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            Descriptor d = vars.get(((Operand)third).getName());
            string.append("load "+ d.getVirtualReg()+"\n");

        }


        string.append("\tputfield ");

        String first = "";
        if(put.getFirstOperand().isLiteral()){
            LiteralElement literal  =  (LiteralElement) put.getFirstOperand();
            first = literal.getLiteral();
        }else {
            Operand op1= (Operand) put.getFirstOperand();
            first = op1.getName();
        }

        if (first.equals("this")) first= className;

        String second = "";
        if(put.getSecondOperand().isLiteral()){
            LiteralElement literal  =  (LiteralElement) put.getSecondOperand();
            second = literal.getLiteral();
        }else {
            Operand op1= (Operand) put.getSecondOperand();
            second = op1.getName();
        }



        string.append(first +"/"+second +" "+ JasminUtils.parseType(put.getSecondOperand().getType().getTypeOfElement()));

        return string.toString()+"\n";
    }

}
