package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CallInstructionHandler implements IntructionHandler{

    private CallInstruction callInstruction;

    public CallInstructionHandler(Instruction callInstruction) {
        this.callInstruction = (CallInstruction) callInstruction;
    }


    @Override
    public String handleInstruction(ClassUnit classUnit,Method method) {

        StringBuilder string = new StringBuilder();
        HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);

        String first = "";
        String stringPop = "";
        /*if (callInstruction.getFirstArg().isLiteral()){
            LiteralElement literal = (LiteralElement) callInstruction.getFirstArg();
            first = literal.getLiteral();
        }
        else {*/
            Operand classOperand = (Operand) callInstruction.getFirstArg();
            first = classOperand.getName();

            //invokestatic doesnt need load
            if (callInstruction.getInvocationType() == CallType.invokespecial || callInstruction.getInvocationType() == CallType.invokevirtual) {
                // load method reference

                string.append("\taload ").append(vars.get(first).getVirtualReg()).append("\n");
                first = ((ClassType) callInstruction.getFirstArg().getType()).getName();

                if (method.getInstructions().contains(this.callInstruction)) {
                    if (callInstruction.getReturnType().getTypeOfElement() != ElementType.VOID) {
                        stringPop = "\tpop\n";
                    }
                }
            }
            else{ // static | new | arraylength (don't need load)
                first = ((Operand) callInstruction.getFirstArg()).getName();
            }

            if(classOperand.getName().equals("this")) {
                if (method.isConstructMethod()) {
                    if (classUnit.getSuperClass() != null)
                        first =  classUnit.getSuperClass();
                    else
                        first = "java/lang/Object";
                } else {
                    first = ((ClassType) callInstruction.getFirstArg().getType()).getName();
                }
            }
        //}

        StringBuilder build = new StringBuilder();
        if (callInstruction.getListOfOperands()!=null) {
            for (Element element : callInstruction.getListOfOperands()) {
                List<Node> pred = callInstruction.getPred();

                System.out.println("============================" + method.getMethodName());

                // TODO initialized variable verification
                MyJasminUtils.loadElement(method, string, element);
                build.append(MyJasminUtils.parseTypeForMethod(element.getType().getTypeOfElement()));
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
        string.append(MyJasminUtils.parseTypeForMethod(callInstruction.getReturnType().getTypeOfElement()) + "\n");
        string.append(stringPop);

        return string.toString();
    }

}