package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.Instruction;

public class AssignInstructionHandler implements IntructionHandler{

    private AssignInstruction instruction;

    public AssignInstructionHandler(Instruction instruction) {
        this.instruction = (AssignInstruction) instruction;
    }

    @Override
    public String handleInstruction() {
        /*
                jasminCode.append("\t" + instruction.toString()+"\n");

                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                Stack stack = new Stack();
                for (String var : varTable.keySet()){
                    stack.push(var);
                }

                generate((Node) assignInstruction, jasminCode, stack);*/

        return "";
    }
}
