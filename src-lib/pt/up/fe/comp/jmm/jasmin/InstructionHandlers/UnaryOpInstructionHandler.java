package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;

public class UnaryOpInstructionHandler implements IntructionHandler{
    private UnaryOpInstruction unaryOpInstruction;

    public UnaryOpInstructionHandler(Instruction unary) {

        this.unaryOpInstruction = (UnaryOpInstruction) unary;

    }


    public String handleInstruction(Method method) {
        StringBuilder string = new StringBuilder();

        Element rop = unaryOpInstruction.getRightOperand();


        HashMap<String, Descriptor> varTable = OllirAccesser.getVarTable(method);

        if (rop.isLiteral()){
            LiteralElement literal = (LiteralElement) rop;
            string.append("\tldc " + literal.getLiteral() + "\n");
        }else{
            HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);
            Operand variable = (Operand) rop;
            Descriptor d = vars.get(variable.getName());
            string.append("\t"+JasminUtils.parseType(rop.getType().getTypeOfElement()));
            string.append("load " + d.getVirtualReg() + "\n");
        }

        string.append("\nTYPE:\n"+unaryOpInstruction.getUnaryOperation().getOpType());

        //string.append(JasminUtils.parseType(rop.getType().getTypeOfElement())+JasminUtils.parseOperationType(unaryOpInstruction.getUnaryOperation().getOpType())+"\n");

        return string.toString();
    }
}
