package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AssignInstructionHandler implements IntructionHandler{

    private AssignInstruction instruction;

    public AssignInstructionHandler(Instruction instruction) {
        this.instruction = (AssignInstruction) instruction;
    }

    @Override
    public String handleInstruction(String className, Method method) {
        StringBuilder string = new StringBuilder();
        InstructionAllocator rhs = new InstructionAllocator();
        HashMap<String, Descriptor> vars = OllirAccesser.getVarTable(method);

        Operand variable = (Operand) instruction.getDest();
        Descriptor destDesc = vars.get(variable.getName());

        // destination variable is an array access
        boolean lhsArrayAccess = destDesc.getVarType().getTypeOfElement() == ElementType.ARRAYREF &&
                                variable.getType().getTypeOfElement() == ElementType.INT32;
        // handle lhs if is an array access -> aload array; iload index
        if (lhsArrayAccess)
            MyJasminUtils.loadElement(method, string, instruction.getDest());

//        System.out.println("ASSIGN ===\n" + method.getMethodName() + "\n" + instruction.getRhs().getInstType());

        // Call instruction allocator to handle right part of assignment
        String rhss = rhs.allocateAndHandle(instruction.getRhs(), className, method);
        string.append(rhss);

        // don't store if successor instruction is a putfield
        Instruction rhsInst = instruction.getRhs();
        if (rhsInst.getInstType() == InstructionType.CALL) {
            Instruction succ = (Instruction) instruction.getSucc1();
            // successor is a putfield call (abort store)
            if (succ.getInstType() == InstructionType.PUTFIELD)
                return string.toString();

////            TODO improvement
//            CallType callType = OllirAccesser.getCallInvocation((CallInstruction) rhsInst);
////             arralength -> value
//            if (callType == CallType.arraylength)
//                return string.toString();

        }
//        else if (rhsInst.getInstType() == InstructionType.GETFIELD) // don't store in getfield calls either
//            return string.toString();


        // store LHS
        ElementType destType = destDesc.getVarType().getTypeOfElement();
        if (destType == ElementType.OBJECTREF){
            string.append("\ta");
        }
        else if (destType == ElementType.ARRAYREF) {

            // if lhs is array access call iastore -> arrayref, index, value
            if (lhsArrayAccess) {
                return string.append("\tiastore\n").toString();
            }
            else // add reference modifier
                string.append("\ta");
        }
        else {

            // check if rhs is an array access (return before doing load)
            Descriptor rhsDesc = this.getRhsElemDescriptor(vars, instruction);
            if (rhsDesc != null) {
                if (rhsDesc.getVarType().getTypeOfElement() == ElementType.ARRAYREF
                    && destDesc.getVarType().getTypeOfElement() == ElementType.INT32) // rhs array access
                    string.append("\tiaload\n"); // load array access
                // TODO improvement (return on iaload if auxiliary variable)
            }

            string.append("\t");
            string.append(MyJasminUtils.parseType(destDesc.getVarType().getTypeOfElement()).toLowerCase(Locale.ROOT));
        }
        string.append("store " + destDesc.getVirtualReg() + "\n");

        return string.toString();
    }

    private ElementType getElemType(Map<String, Descriptor> vars, Instruction inst) {
        if (instruction.getRhs().getInstType() == InstructionType.NOPER) {
            // get variable descriptor
            Descriptor des = this.getRhsElemDescriptor(vars, inst);

            return des != null ? des.getVarType().getTypeOfElement() : null;
        }
        return null;
    }

    private Descriptor getRhsElemDescriptor(Map<String, Descriptor> vars, Instruction inst) {
        if (inst.getInstType() == InstructionType.NOPER) {
            Element op = ((SingleOpInstruction) inst).getSingleOperand();
            String name = MyJasminUtils.getElementName(op);
            return vars.get(name);
        }
        else if (inst.getInstType() == InstructionType.ASSIGN) {
            Instruction op = (((AssignInstruction) inst).getRhs());
            return getRhsElemDescriptor(vars, op);
        }
        return null;
    }
}
