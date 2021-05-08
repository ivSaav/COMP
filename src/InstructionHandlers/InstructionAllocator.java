package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class InstructionAllocator {

    public InstructionAllocator() {

    }

    public String allocateAndHandle(Instruction instruction,String className, Method method){

        StringBuilder string = new StringBuilder();


        List<String> labels = method.getLabels(instruction);

        for (String label : labels){
            string.insert(0,label+":\n");
        }

        switch (instruction.getInstType()){
            case ASSIGN:
                string.append(new AssignInstructionHandler(instruction).handleInstruction(className,method));
                break;
            case CALL:
                string.append(new CallInstructionHandler(instruction).handleInstruction(className,method));
                break;
            case RETURN:
                string.append(new ReturnInstructionHandler(instruction).handleInstruction(className, method));
                break;
            case GETFIELD:
                string.append(new GetFieldInstructionHandler(instruction).handleInstruction(className,method));
                break;
            case PUTFIELD:
                string.append(new PutFieldInstructionHandler(instruction).handleInstruction(className,method));
                break;
            case GOTO:
                string.append(new GoToInstructionHandler(instruction).handleInstruction(className,method));
                break;
            case BRANCH:
                string.append(new CondBranchInstructionHandler(instruction).handleInstruction(className, method));
                break;
            case UNARYOPER:
                string.append(new UnaryOpInstructionHandler(instruction).handleInstruction(className, method));
                break;
            case BINARYOPER:
                string.append(new BinaryOpInstructionHandler(instruction).handleInstruction(className, method));
                break;
            case NOPER:
                string.append(new SingleOpInstructionHandler(instruction).handleInstruction(className, method));
                break;
            default:
                string.append("\t" + instruction.toString()+"\n");
        }

        return string.toString();
    }

}
