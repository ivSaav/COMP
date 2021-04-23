package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.Stack;

public class InstructionAllocator {

    public InstructionAllocator() {}

    public String allocateAndHandle(Instruction instruction, Method method){
        switch (instruction.getInstType()){
            case CALL:
                return new CallInstructionHandler(instruction).handleInstruction(method);
            case RETURN:
                return new ReturnInstructionHandler(instruction).handleInstruction(method);
            case GETFIELD:
                //TODO
                return new GetFieldInstructionHandler(instruction).handleInstruction(method);
            case PUTFIELD:
                //TODO
                return new PutFieldInstructionHandler(instruction).handleInstruction(method);
            case ASSIGN:
                //TODO
                return new AssignInstructionHandler(instruction).handleInstruction(method);
            case GOTO:
                //TODO
                return new GoToInstructionHandler(instruction).handleInstruction(method);
            case BRANCH:
                //TODO
                return new BranchInstructionHandler(instruction).handleInstruction(method);
            case UNARYOPER:
                //seems to be negation etc
                //TODO
                return new UnaryOpInstructionHandler(instruction).handleInstruction(method);
            case BINARYOPER:
                //TODO
                return new BinaryOpInstructionHandler(instruction).handleInstruction(method);
            case NOPER:
                //TODO
                return new SingleOpInstructionHandler(instruction).handleInstruction(method);
            default:
                return "\t" + instruction.toString()+"\n";

        }
    }

}
