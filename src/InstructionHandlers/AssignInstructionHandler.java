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
        Descriptor d = vars.get(variable.getName());

        // destination variable is an array access
        boolean lhsArrayAccess = d.getVarType().getTypeOfElement() == ElementType.ARRAYREF &&
                                variable.getType().getTypeOfElement() == ElementType.INT32;
        // handle lhs if is an array access -> aload array; iload index
        if (lhsArrayAccess)
            MyJasminUtils.checkLiteralOrOperand(method, string, instruction.getDest());

        //Call instruction allocator to handle right part of assignment
        String rhss = rhs.allocateAndHandle(instruction.getRhs(), className, method);
        string.append(rhss);

        ElementType destType = d.getVarType().getTypeOfElement();
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
            Descriptor desc = this.getElemDescriptor(vars, instruction);
            if (desc != null) {
                if (desc.getVarType().getTypeOfElement() == ElementType.ARRAYREF && d.getVarType().getTypeOfElement() == ElementType.INT32)
                    return string.append(""/*"\tiaload\n"*/).toString();
            }

            string.append("\t");
            string.append(MyJasminUtils.parseType(d.getVarType().getTypeOfElement()).toLowerCase(Locale.ROOT));
        }
        string.append("store " + d.getVirtualReg() + "\n");

        return string.toString();
    }

    private ElementType getElemType(Map<String, Descriptor> vars, Instruction inst) {
        if (instruction.getRhs().getInstType() == InstructionType.NOPER) {
            // get variable descriptor
            Descriptor des = this.getElemDescriptor(vars, inst);

            return des != null ? des.getVarType().getTypeOfElement() : null;
        }
        return null;
    }

    private Descriptor getElemDescriptor(Map<String, Descriptor> vars, Instruction inst) {
        if (inst.getInstType() == InstructionType.NOPER) {
            Element op = ((SingleOpInstruction) inst).getSingleOperand();
            String name = MyJasminUtils.getElementName(op);
            return vars.get(name);
        }
        else if (inst.getInstType() == InstructionType.ASSIGN) {
            Instruction op = ((Instruction) ((AssignInstruction) inst).getRhs());
            return getElemDescriptor(vars, op);
        }
        return null;
    }
}
