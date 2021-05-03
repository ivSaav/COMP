package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class CallInstructionHandler implements IntructionHandler{

    private CallInstruction callInstruction;

    public CallInstructionHandler(Instruction callInstruction) {
        this.callInstruction = (CallInstruction) callInstruction;
    }


    @Override
    public String handleInstruction(String className,Method method) {

        StringBuilder string = new StringBuilder();
        HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);

        String first = "";
        if (callInstruction.getFirstArg().isLiteral()){
            LiteralElement literal = (LiteralElement) callInstruction.getFirstArg();
            first = literal.getLiteral();
        }
        else {
            Operand classOperand = (Operand) callInstruction.getFirstArg();
            first = classOperand.getName();

            if (callInstruction.getInvocationType() == CallType.invokespecial || callInstruction.getInvocationType() == CallType.invokevirtual) {
                string.append("\taload ").append(vars.get(first).getVirtualReg()).append("\n");
                first = ((ClassType) callInstruction.getFirstArg().getType()).getName();
            }
            else{
                first = ((Operand) callInstruction.getFirstArg()).getName();
            }


            if(classOperand.getName().equals("this")) {
                if (method.isConstructMethod()) {
                    first = "java/lang/Object"; // TODO
                } else {
                    first = ((ClassType) callInstruction.getFirstArg().getType()).getName();
                }
            }


        }



        StringBuilder build = new StringBuilder();
        if (callInstruction.getListOfOperands()!=null) {
            int count = 0;

            for (Element element : callInstruction.getListOfOperands()) {

                if (element.isLiteral()) {
                    LiteralElement literal = (LiteralElement) element;
                    string.append("\tldc "+literal.getLiteral()+" \n");
                    build.append(((LiteralElement) element).getLiteral());
                } else {

                    if (element.getType().getTypeOfElement() == ElementType.OBJECTREF) {
                        string.append("\t a");
                    }
                    else
                        string.append("\t"+JasminUtils.parseType(element.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));

                    Operand variable = (Operand) element;
                    Descriptor d = vars.get(variable.getName());
                    string.append("load "+ d.getVirtualReg()+"\n");

                    build.append(JasminUtils.parseType(variable.getType().getTypeOfElement()));
                }
                count++;

            }
        }

        string.append("\t"+ OllirAccesser.getCallInvocation(callInstruction).toString().toLowerCase(Locale.ROOT) + " " + first);

        if (callInstruction.getSecondArg()!= null) {
            LiteralElement methodLiteral = (LiteralElement) callInstruction.getSecondArg();
            String methodName = methodLiteral.getLiteral().substring(1, methodLiteral.getLiteral().length() - 1);
            string.append("/" +methodName);
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
