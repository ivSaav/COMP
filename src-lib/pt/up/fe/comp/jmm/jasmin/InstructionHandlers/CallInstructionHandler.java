package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

public class CallInstructionHandler implements IntructionHandler{

    private CallInstruction callInstruction;

    public CallInstructionHandler(Instruction callInstruction) {
        this.callInstruction = (CallInstruction) callInstruction;
    }


    @Override
    public String handleInstruction(Method method) {

        StringBuilder string = new StringBuilder();

        Operand classOperand = (Operand) callInstruction.getFirstArg();
        String className = classOperand.getName();


        if(className.equals("this"))
            className = "java/lang/Object";
        if (callInstruction.getSecondArg()!= null) {

            LiteralElement methodLiteral = (LiteralElement) callInstruction.getSecondArg();
            String methodName = methodLiteral.getLiteral().substring(1, methodLiteral.getLiteral().length() - 1);
            string.append("\t"+ OllirAccesser.getCallInvocation(callInstruction) + " " + className+"/" +methodName+ "(");
        }


        for(Element element: callInstruction.getListOfOperands()){
            if (element.isLiteral()){
                string.append("literal");

            }
            else {
                Operand operand1 = (Operand) element;

                string.append(JasminUtils.parseType(operand1.getType().getTypeOfElement()));
            }
        }


        string.append(")");

        string.append(JasminUtils.parseType(callInstruction.getReturnType().getTypeOfElement())+"\n");
        return string.toString();
    }
}
