package InstructionHandlers;

import org.specs.comp.ollir.GotoInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;

public class GoToInstructionHandler implements IntructionHandler{

    private GotoInstruction instruction;

    public GoToInstructionHandler(Instruction instruction) {
        this.instruction = (GotoInstruction) instruction;
    }

    @Override
    public String handleInstruction(String className,Method method) {
        return "\tgoto " + instruction.getLabel()+"\n";
    }




}
