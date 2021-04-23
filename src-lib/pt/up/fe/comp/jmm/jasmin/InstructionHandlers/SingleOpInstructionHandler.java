package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;

import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.UnaryOpInstruction;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class SingleOpInstructionHandler implements IntructionHandler{

    private SingleOpInstruction singleOpInstruction;

    public SingleOpInstructionHandler(Instruction single) {

        this.singleOpInstruction = (SingleOpInstruction) single;
    }

    public String handleInstruction(Method method) {
        StringBuilder string = new StringBuilder();
        Element op = singleOpInstruction.getSingleOperand();

        if (op.isLiteral()){
            LiteralElement literal = (LiteralElement) op;
            string.append("\tldc "+literal.getLiteral()+" \n");

        }else{
            HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);
            string.append(JasminUtils.parseType(op.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            Operand variable = (Operand) op;
            Descriptor d = vars.get(variable.getName());
            string.append("load "+ d.getVirtualReg()+"\n");
        }

        return string.toString();
    }
}
