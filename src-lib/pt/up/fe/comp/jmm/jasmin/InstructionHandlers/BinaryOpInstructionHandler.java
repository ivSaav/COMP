package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.BinaryOpInstruction;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.Locale;

public class BinaryOpInstructionHandler implements IntructionHandler{

    private BinaryOpInstruction instruction;

    public BinaryOpInstructionHandler(Instruction instruction) {
        this.instruction = (BinaryOpInstruction) instruction;
    }


    @Override
    public String handleInstruction() {
        StringBuilder string = new StringBuilder();

        Element rop = instruction.getRightOperand();
        Element lop = instruction.getLeftOperand();

        if (rop.isLiteral()){
            string.append("\tldc literal \n");
            //string.append(op.);
        }else{
            string.append("\t"+JasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            string.append("load variable \n");
        }

        if (lop.isLiteral()){
            string.append("\tldc literal \n");
            //string.append(op.);
        }else{
            string.append("\t"+JasminUtils.parseType(lop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            string.append("load variable \n");
        }


        //System.out.println(instruction.getUnaryOperation().getOpType());
        if (instruction.getUnaryOperation().getOpType()!=null)
        string.append(JasminUtils.parseType(rop.getType().getTypeOfElement())+JasminUtils.parseOperationType(instruction.getUnaryOperation().getOpType())+"\n");

        return string.toString();
    }
}
