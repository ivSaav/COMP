package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.ReturnInstruction;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.Locale;

public class ReturnInstructionHandler implements IntructionHandler {

    private ReturnInstruction returnInstruction;

    public ReturnInstructionHandler(Instruction returnInstruction) {
        this.returnInstruction = (ReturnInstruction) returnInstruction;
    }

    @Override
    public String handleInstruction(Method method) {

        StringBuffer string = new StringBuffer();

        ElementType returnType = method.getReturnType().getTypeOfElement();

        if(returnType != null && returnType!=ElementType.VOID){
            string.append("\t" + JasminUtils.parseType(returnType).toLowerCase(Locale.ROOT));
        }

        string.append("return ");
        string.append("\n");

        return string.toString();
    }
}
