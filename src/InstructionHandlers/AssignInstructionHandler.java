package InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class AssignInstructionHandler implements IntructionHandler{

    private AssignInstruction instruction;

    public AssignInstructionHandler(Instruction instruction) {
        this.instruction = (AssignInstruction) instruction;
    }

    @Override
    public String handleInstruction(String className, Method method) {
        StringBuilder string = new StringBuilder();
        InstructionAllocator rhs = new InstructionAllocator();

        //Call instruction allocator to handle right part of assignment
        String rhss= rhs.allocateAndHandle(instruction.getRhs(),className,method);
        string.append(rhss);

        HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);
        Operand variable = (Operand) instruction.getDest();
        Descriptor d = vars.get(variable.getName());

        //global variable declaration
        string.append("\t");

        //check variable type
        if(d.getVarType().getTypeOfElement()==ElementType.OBJECTREF)
            string.append("a");
        else {
            string.append(MyJasminUtils.parseType(d.getVarType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            string.append("store " + d.getVirtualReg() + "\n");
        }

        return string.toString();
    }
}
