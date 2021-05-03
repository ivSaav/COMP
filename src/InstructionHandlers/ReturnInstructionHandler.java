package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class ReturnInstructionHandler implements IntructionHandler {

    private ReturnInstruction returnInstruction;

    public ReturnInstructionHandler(Instruction returnInstruction) {
        this.returnInstruction = (ReturnInstruction) returnInstruction;
    }

    @Override
    public String handleInstruction(String className, Method method) {

        StringBuffer string = new StringBuffer();

        ElementType returnType = method.getReturnType().getTypeOfElement();

        if (returnInstruction.hasReturnValue()) {
            Element rop = returnInstruction.getOperand();
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

        }
        if(returnType != null && returnType!=ElementType.VOID){
            string.append("\t" + JasminUtils.parseType(returnType).toLowerCase(Locale.ROOT));
        }
        else{
            string.append("\t");
        }

        string.append("return ");
        string.append("\n");

        return string.toString();
    }
}
