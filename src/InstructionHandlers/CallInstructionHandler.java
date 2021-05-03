package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.Locale;

public class CallInstructionHandler implements IntructionHandler{

    private CallInstruction callInstruction;

    public CallInstructionHandler(Instruction callInstruction) {
        this.callInstruction = (CallInstruction) callInstruction;
    }


    @Override
    public String handleInstruction(String className,Method method) {

        StringBuilder string = new StringBuilder();

        String first = "";
        if (callInstruction.getFirstArg().isLiteral()){
            LiteralElement literal = (LiteralElement) callInstruction.getFirstArg();
            first = literal.getLiteral();
        }
        else {
            Operand classOperand = (Operand) callInstruction.getFirstArg();
            first = classOperand.getName();
        }

        if(first.equals("this"))
            if (method.isConstructMethod()){
                first = "java/lang/Object";
            }else{
                first = className;
            }

        string.append("\t"+ OllirAccesser.getCallInvocation(callInstruction).toString().toLowerCase(Locale.ROOT) + " " + first);

        if (callInstruction.getSecondArg()!= null) {
            LiteralElement methodLiteral = (LiteralElement) callInstruction.getSecondArg();
            String methodName = methodLiteral.getLiteral().substring(1, methodLiteral.getLiteral().length() - 1);
            string.append("/" +methodName);
        }


        StringBuilder build = new StringBuilder();
        if (callInstruction.getListOfOperands()!=null) {
            int count = 0;

            for (Element element : callInstruction.getListOfOperands()) {
                if (count!=0){build.append(";");}

                if (element.isLiteral()) {
                    build.append(((LiteralElement) element).getLiteral());
                } else {
                    Operand operand1 = (Operand) element;
                    build.append(JasminUtils.parseType(operand1.getType().getTypeOfElement()));
                }
                count++;

            }
        }
        if (build.toString().equals("")){
            if (OllirAccesser.getCallInvocation(callInstruction) != CallType.NEW){
                string.append("()");
            }

        }else {
            string.append("(");
            string.append(build);
            string.append(")");
        }

        if (OllirAccesser.getCallInvocation(callInstruction) != CallType.NEW)
            string.append(JasminUtils.parseType(callInstruction.getReturnType().getTypeOfElement()));

        return string.toString()+"\n";
    }
}
