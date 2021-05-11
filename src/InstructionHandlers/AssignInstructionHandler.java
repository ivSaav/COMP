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

        // handle lhs
        if (instruction.getDest().getType().getTypeOfElement() == ElementType.ARRAYREF) {
//            String name = MyJasminUtils.getElementName(instruction.getDest());
//            string.append("\taload " + vars.get(name).getVirtualReg() + "\n");

//            MyJasminUtils.checkLiteralOrOperand(method, string, instruction.getDest());

        }

        //Call instruction allocator to handle right part of assignment
        String rhss = rhs.allocateAndHandle(instruction.getRhs(), className, method);
        string.append(rhss);


        Operand variable = (Operand) instruction.getDest();
        Descriptor d = vars.get(variable.getName());

        //check variable type


        System.out.println("DEST " + variable.getName());
        System.out.println("RHSS " + rhss);
        MyJasminUtils.printVarTable(vars);

        ElementType destType = d.getVarType().getTypeOfElement();
        ElementType rhsType = this.getElemType(vars, instruction.getRhs());

        System.out.println("RHSType " + rhsType + " " + destType);
//         Array access verification
        if (rhsType == ElementType.ARRAYREF && destType == ElementType.INT32) { // lhs is iteger in array access
                return string.toString();
        }

        if (destType == ElementType.OBJECTREF || destType == ElementType.ARRAYREF){
            string.append("\ta");

        }
        else {
            string.append("\t");
            string.append(MyJasminUtils.parseType(d.getVarType().getTypeOfElement()).toLowerCase(Locale.ROOT));


            // check if rhs is an array
            Descriptor desc = this.getRhsElemDescriptor(vars, instruction);

            if (desc != null) {
//                System.out.println("VAR T: " + desc.));
                if (desc.getVarType().getTypeOfElement() == ElementType.ARRAYREF)
                    return string.append("astore\n").toString();
            }
        }
        string.append("store " + d.getVirtualReg() + "\n");

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
        System.out.println(inst.getInstType());
        if (inst.getInstType() == InstructionType.NOPER) {
            Element op = ((SingleOpInstruction) inst).getSingleOperand();
            System.out.println("OPOPOPO : " + op.getType());
            String name = MyJasminUtils.getElementName(op);
            return vars.get(name);
        }
        else if (inst.getInstType() == InstructionType.ASSIGN) {
            Instruction op = ((Instruction) ((AssignInstruction) inst).getRhs());
            return getRhsElemDescriptor(vars, op);
        }
        return null;
    }
}
