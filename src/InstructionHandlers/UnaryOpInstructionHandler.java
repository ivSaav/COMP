package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class UnaryOpInstructionHandler implements IntructionHandler{
    private UnaryOpInstruction unaryOpInstruction;

    public UnaryOpInstructionHandler(Instruction unary) {

        this.unaryOpInstruction = (UnaryOpInstruction) unary;

    }


    public String handleInstruction(String className, Method method) {
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
            if (rop.getType().getTypeOfElement() == ElementType.OBJECTREF)
                string.append("\t a");
            else
                string.append("\t"+JasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            string.append("load " + d.getVirtualReg() + "\n");
        }

        //string.append("\nTYPE:\n"+unaryOpInstruction.getUnaryOperation().getOpType());

        string.append("\t"+ JasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
        string.append(JasminUtils.parseOperationType(unaryOpInstruction.getUnaryOperation().getOpType())+"\n");


        return string.toString();
    }
}
