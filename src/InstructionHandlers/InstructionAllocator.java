package InstructionHandlers;

import org.specs.comp.ollir.*;

public class InstructionAllocator {

    public InstructionAllocator() {}

    public String allocateAndHandle(Instruction instruction,String className, Method method){
        switch (instruction.getInstType()){
            case ASSIGN:
                return new AssignInstructionHandler(instruction).handleInstruction(className,method);
            case CALL:
                return new CallInstructionHandler(instruction).handleInstruction(className,method);
            case RETURN:
                return new ReturnInstructionHandler(instruction).handleInstruction(className, method);
            case GETFIELD:
                return new GetFieldInstructionHandler(instruction).handleInstruction(className,method);
            case PUTFIELD:
                return new PutFieldInstructionHandler(instruction).handleInstruction(className,method);
            case GOTO:
                return new GoToInstructionHandler(instruction).handleInstruction(className,method);
            case BRANCH:
                return new CondBranchInstructionHandler(instruction).handleInstruction(className, method);
            case UNARYOPER:
                return new UnaryOpInstructionHandler(instruction).handleInstruction(className, method);
            case BINARYOPER:
                return new BinaryOpInstructionHandler(instruction).handleInstruction(className, method);
            case NOPER:
                return new SingleOpInstructionHandler(instruction).handleInstruction(className, method);
            default:
                return "\t" + instruction.toString()+"\n";
        }
    }

}
