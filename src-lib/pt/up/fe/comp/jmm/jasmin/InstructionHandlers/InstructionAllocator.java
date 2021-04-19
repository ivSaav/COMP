package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.Stack;

public class InstructionAllocator {

    public InstructionAllocator() {}

    public String allocateAndHandle(Instruction instruction, Method method){
        switch (instruction.getInstType()){
            case CALL:
                return new CallInstructionHandler(instruction).handleInstruction();
            case RETURN:
                return new ReturnInstructionHandler(instruction, method.getReturnType().getTypeOfElement()).handleInstruction();
            case GETFIELD:
                //TODO
                return new GetFieldInstructionHandler(instruction).handleInstruction();
            case PUTFIELD:
                //TODO
                return new PutFieldInstructionHandler(instruction).handleInstruction();
            case ASSIGN:
                //TODO
                return new AssignInstructionHandler(instruction).handleInstruction();
            case GOTO:
                //TODO
                return new GoToInstructionHandler(instruction).handleInstruction();
            case BRANCH:
                //TODO
                return new BranchInstructionHandler(instruction).handleInstruction();
            case UNARYOPER:
                //seems to be negation etc
                //TODO
                return new UnaryOpInstructionHandler(instruction).handleInstruction();
            case BINARYOPER:
                //TODO
                return new BinaryOpInstructionHandler(instruction).handleInstruction();
            case NOPER:
                //TODO
                return new SingleOpInstructionHandler(instruction).handleInstruction();
            default:
                return "\t" + instruction.toString()+"\n";

        }
    }

}
