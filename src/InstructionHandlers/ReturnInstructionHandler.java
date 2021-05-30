package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.Locale;

public class ReturnInstructionHandler implements IntructionHandler {

    private final ReturnInstruction returnInstruction;

    public ReturnInstructionHandler(Instruction returnInstruction) {
        this.returnInstruction = (ReturnInstruction) returnInstruction;
    }

    @Override
    public String handleInstruction(ClassUnit classUnit, Method method) {

        StringBuilder string = new StringBuilder();

        ElementType returnType = method.getReturnType().getTypeOfElement();

        if (returnInstruction.hasReturnValue()) {
            Element rop = returnInstruction.getOperand();
                MyJasminUtils.loadElement(method, string, rop);
        }

        string.append("\t");
        if(returnType != null && returnType!=ElementType.VOID){
            if (returnType == ElementType.ARRAYREF)
                string.append("a");
            else if (returnType == ElementType.BOOLEAN)
                string.append("i");
            else
                string.append(MyJasminUtils.parseType(returnType).toLowerCase(Locale.ROOT));
        }
        string.append("return \n");

        return string.toString();
    }
}
