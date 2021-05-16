package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.JmmNode;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AssignInstructionHandler implements IntructionHandler{

    private AssignInstruction instruction;

    public AssignInstructionHandler(Instruction instruction) {
        this.instruction = (AssignInstruction) instruction;
    }

    @Override
    public String handleInstruction(ClassUnit classUnit, Method method) {
        String className = classUnit.getClassName();

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
        String rhss = rhs.allocateAndHandle(instruction.getRhs(), classUnit, method);

        // don't store if successor instruction is a putfield
        // auxiliary expression for putfield call
        if (this.precedesPutFieldCall(variable.getName())) {
            string.append("\taload_0\n"); // 'this'
            string.append(rhss); // putfield args
            return string.toString();
        }

        string.append(rhss);

        // store LHS
        ElementType destType = destDesc.getVarType().getTypeOfElement();
//        System.out.println("DEST " + destType + " " + MyJasminUtils.getElementName(this.instruction.getDest()));
        if (destType == ElementType.OBJECTREF){
            string.append("\ta");
        }
        else if (destType == ElementType.ARRAYREF) {

            // if lhs is array access call iastore -> arrayref, index, value
            if (lhsArrayAccess) {

                // check if rhs is an array access          dest[i] = rhs[j]
                Descriptor rhsDesc = this.getRhsElemDescriptor(vars, instruction);
                if (rhsDesc != null && rhsDesc.getVarType().getTypeOfElement() == ElementType.ARRAYREF)// rhs array access
                        string.append("\tiaload\n"); // load rhs array access

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

    /**
     * Determines if the next instruction after this one is a putfield call
     * @return
     */
    private boolean precedesPutFieldCall(String varName) {
        Instruction succ = (Instruction) instruction.getSucc1();
        // successor is a putfield call (abort store)
        if (succ.getInstType() == InstructionType.PUTFIELD) {
            PutFieldInstruction putCall = (PutFieldInstruction) succ;
            String name = MyJasminUtils.getElementName(putCall.getThirdOperand()) ;
            System.out.println("VAERS " + varName + " " + name);
            return varName.equals(name);
//            return true;
        }
        return false;
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
