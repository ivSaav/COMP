package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.ReturnInstruction;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.Locale;

public class ReturnInstructionHandler implements IntructionHandler {

    private ReturnInstruction returnInstruction;
    private ElementType returnType;

    public ReturnInstructionHandler(Instruction returnInstruction, ElementType returnType) {
        this.returnInstruction = (ReturnInstruction) returnInstruction;
        this.returnType = returnType;
    }

    @Override
    public String handleInstruction() {

        StringBuffer string = new StringBuffer();

        if(returnType != null && returnType!=ElementType.VOID){
            string.append("\t" + JasminUtils.parseType(returnType).toLowerCase(Locale.ROOT));
        }

        string.append("return ");
        string.append("\n");

        return string.toString();
    }
}
