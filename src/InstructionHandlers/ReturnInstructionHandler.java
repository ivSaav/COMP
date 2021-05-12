package InstructionHandlers;

import org.specs.comp.ollir.*;

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
//            if (!MyJasminUtils.isLoaded(rop, this.returnInstruction.getPred()))
                MyJasminUtils.loadElement(method, string, rop);
        }

        string.append("\t");
        if(returnType != null && returnType!=ElementType.VOID){
            if (returnType == ElementType.ARRAYREF)
                string.append("a");
            else
                string.append(MyJasminUtils.parseType(returnType).toLowerCase(Locale.ROOT));
        }
        string.append("return \n");

        return string.toString();
    }
}
