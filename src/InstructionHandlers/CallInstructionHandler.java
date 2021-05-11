package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.List;
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
                // load method reference
                string.append("\taload ").append(vars.get(first).getVirtualReg()).append("\n");
                first = ((ClassType) callInstruction.getFirstArg().getType()).getName();
            }
            else{ // static | new | arraylength (don't need load)
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
                List<Node> pred = callInstruction.getPred();

                // checking if the var in method call's parameters has been initialized
                // if there is a predecessor assign instruction then the variable is already in the stack
                // TODO maybe review for other cases (check if the name of the variable is the same as assign dest)
                boolean initializedVar = false;
                for (Node n : pred) {
                    InstructionType predInstrType = ((Instruction) n).getInstType();
                    // param variable has already been involved in an assignment
                    if (predInstrType == InstructionType.ASSIGN) {
                        Element predAssign = ((AssignInstruction) n).getDest();
                        String name = MyJasminUtils.getElementName(predAssign);

                        String paramVarName = MyJasminUtils.getElementName(element);
                        initializedVar = paramVarName.equals(name); // checking if predecessor is the
                        if (initializedVar) // found var initialization in pred assigns (stop)
                            break;
                    }
                }

                // if parameter is not in the stack (load it)
                if (!initializedVar)
                    MyJasminUtils.checkLiteralOrOperand(method, string, element);
                build.append(MyJasminUtils.parseType(element.getType().getTypeOfElement()));
            }
        }

        CallType callType = OllirAccesser.getCallInvocation(callInstruction);
        // new declaration
        if(callType == CallType.NEW) {
            if(first.equals("array")){
                string.append("\t" + callType.toString().toLowerCase(Locale.ROOT) + first);
                string.append(" int\n");
            }else{
                string.append("\t" + callType.toString().toLowerCase(Locale.ROOT) + " " +first+"\n");
            }

            return string.toString();
        }
        else if(callType == CallType.arraylength){
            // load array reference into stack
            Operand arrayVar = (Operand) callInstruction.getFirstArg();
            String arrayName = MyJasminUtils.getElementName(arrayVar);
            string.append("\taload " + vars.get(arrayName).getVirtualReg() + "\n");

            // arrayref → length
            string.append("\tarraylength \n");
            return string.toString();
        }else{
            string.append("\t"+ callType.toString().toLowerCase(Locale.ROOT) + " " + first);
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