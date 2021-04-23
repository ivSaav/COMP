package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;

import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.UnaryOpInstruction;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.Locale;

public class SingleOpInstructionHandler implements IntructionHandler{

    private SingleOpInstruction singleOpInstruction;

    public SingleOpInstructionHandler(Instruction single) {

        this.singleOpInstruction = (SingleOpInstruction) single;
    }


    public String handleInstruction() {
        StringBuilder string = new StringBuilder();
        Element op = singleOpInstruction.getSingleOperand();

        if (op.isLiteral()){
            string.append("\tldc literal \n");
            //string.append(op.);
        }else{
            string.append(JasminUtils.parseType(op.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            string.append("\tload variable \n");
        }

        return string.toString();
    }
}
