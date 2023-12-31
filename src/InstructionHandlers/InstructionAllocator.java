package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.List;

public class InstructionAllocator {

    public InstructionAllocator() {

    }

    public String allocateAndHandle(Instruction instruction, ClassUnit classUnit, Method method){

        StringBuilder string = new StringBuilder();

        List<String> labels = method.getLabels(instruction);

        for (String label : labels){
            string.insert(0,label+":\n");
        }

        switch (instruction.getInstType()) {
            case ASSIGN -> string.append(new AssignInstructionHandler(instruction).handleInstruction(classUnit, method));
            case CALL -> string.append(new CallInstructionHandler(instruction).handleInstruction(classUnit, method));
            case RETURN -> string.append(new ReturnInstructionHandler(instruction).handleInstruction(classUnit, method));
            case GETFIELD -> string.append(new GetFieldInstructionHandler(instruction).handleInstruction(classUnit, method));
            case PUTFIELD -> string.append(new PutFieldInstructionHandler(instruction).handleInstruction(classUnit, method));
            case GOTO -> string.append(new GoToInstructionHandler(instruction).handleInstruction(classUnit, method));
            case BRANCH -> string.append(new CondBranchInstructionHandler(instruction).handleInstruction(classUnit, method));
            case UNARYOPER -> string.append(new UnaryOpInstructionHandler(instruction).handleInstruction(classUnit, method));
            case BINARYOPER -> string.append(new BinaryOpInstructionHandler(instruction).handleInstruction(classUnit, method));
            case NOPER -> string.append(new SingleOpInstructionHandler(instruction).handleInstruction(classUnit, method));
            default -> string.append("\t" + instruction + "\n");
        }

        return string.toString();
    }

}
