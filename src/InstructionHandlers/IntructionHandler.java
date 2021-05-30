package InstructionHandlers;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;

public interface IntructionHandler {

    String handleInstruction(ClassUnit classUnit, Method method);
}
