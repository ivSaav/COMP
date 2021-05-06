package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.Locale;

public class ReturnInstructionHandler implements IntructionHandler {

    private ReturnInstruction returnInstruction;

    public ReturnInstructionHandler(Instruction returnInstruction) {
        this.returnInstruction = (ReturnInstruction) returnInstruction;
    }

    @Override
    public String handleInstruction(String className, Method method) {

        StringBuilder string = new StringBuilder();

        ElementType returnType = method.getReturnType().getTypeOfElement();

        if (returnInstruction.hasReturnValue()) {
            Element rop = returnInstruction.getOperand();
            MyJasminUtils.checkLiteralOrOperand(method, string, rop);
        }

        string.append("\t");
        if(returnType != null && returnType!=ElementType.VOID){
            string.append(MyJasminUtils.parseType(returnType).toLowerCase(Locale.ROOT));
        }
        string.append("return \n");

        return string.toString();
    }
}
