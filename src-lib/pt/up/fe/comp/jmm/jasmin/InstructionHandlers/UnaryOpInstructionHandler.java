package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.GetFieldInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.UnaryOpInstruction;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

public class UnaryOpInstructionHandler implements IntructionHandler{
    private UnaryOpInstruction unaryOpInstruction;

    public UnaryOpInstructionHandler(Instruction unary) {

        this.unaryOpInstruction = (UnaryOpInstruction) unary;
    }


    public String handleInstruction() {
        StringBuilder string = new StringBuilder();

        Element rop = unaryOpInstruction.getRightOperand();

        if (rop.isLiteral()){
            string.append("\tldc literal \n");
            //string.append(op.);
        }else{
            string.append("\t"+JasminUtils.parseType(rop.getType().getTypeOfElement()));
            string.append("load variable \n");
        }

        string.append(JasminUtils.parseType(rop.getType().getTypeOfElement())+JasminUtils.parseOperationType(unaryOpInstruction.getUnaryOperation().getOpType())+"\n");

        return string.toString();
    }
}
