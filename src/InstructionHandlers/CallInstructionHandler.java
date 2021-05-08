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

            //invokestatic doesnt need load
            if (callInstruction.getInvocationType() == CallType.invokespecial || callInstruction.getInvocationType() == CallType.invokevirtual) {
                string.append("\taload ").append(vars.get(first).getVirtualReg()).append("\n");
                first = ((ClassType) callInstruction.getFirstArg().getType()).getName();
            }
            else{
                first = ((Operand) callInstruction.getFirstArg()).getName();
            }

            if(classOperand.getName().equals("this")) {
                if (method.isConstructMethod()) {
                    first = "java/lang/Object";
                    // TODO VERIFY SUPER
                } else {
                    first = ((ClassType) callInstruction.getFirstArg().getType()).getName();
                }
            }
        }

        StringBuilder build = new StringBuilder();
        if (callInstruction.getListOfOperands()!=null) {
            for (Element element : callInstruction.getListOfOperands()) {
                MyJasminUtils.checkLiteralOrOperand(method, string, element);
                build.append(MyJasminUtils.parseType(element.getType().getTypeOfElement()));
            }
        }

        if(OllirAccesser.getCallInvocation(callInstruction) == CallType.NEW) {
            string.append("\t" + OllirAccesser.getCallInvocation(callInstruction).toString().toLowerCase(Locale.ROOT) + first);
            string.append(" int\n");
            return string.toString();
        }
        else if(OllirAccesser.getCallInvocation(callInstruction) == CallType.arraylength){
            string.append("\t" + OllirAccesser.getCallInvocation(callInstruction).toString().toLowerCase(Locale.ROOT)+"\n");
            return string.toString();
        }else{
            string.append("\t"+ OllirAccesser.getCallInvocation(callInstruction).toString().toLowerCase(Locale.ROOT) + " " + first);
        }


        if (callInstruction.getSecondArg()!= null) {
            LiteralElement methodLiteral = (LiteralElement) callInstruction.getSecondArg();
            String methodName = methodLiteral.getLiteral().substring(1, methodLiteral.getLiteral().length() - 1);
            string.append("/" +methodName);
        }


        //operands of method calls (NEW and arraylenght dont have args)
        string.append("(");
        string.append(build);
        string.append(")");
        string.append(MyJasminUtils.parseType(callInstruction.getReturnType().getTypeOfElement()));


        return string+"\n";
    }
}
