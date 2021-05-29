package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.HashMap;
import java.util.Locale;

public class BinaryOpInstructionHandler implements IntructionHandler{

    private BinaryOpInstruction instruction;

    public BinaryOpInstructionHandler(Instruction instruction) {
        this.instruction = (BinaryOpInstruction) instruction;
    }

    @Override
    public String handleInstruction(ClassUnit classUnit ,Method method) {
        StringBuilder string = new StringBuilder();

        OperationType opType = instruction.getUnaryOperation().getOpType();
        Element rop = instruction.getRightOperand();
        Element lop = instruction.getLeftOperand();

        if (opType == OperationType.LTH) {
            MyJasminUtils.loadElement(method, string, lop);
            boolean isRhsZero = rop.isLiteral() && ((LiteralElement) rop).getLiteral().equals("0");
            // i < 0
            if (!isRhsZero) {
                MyJasminUtils.loadElement(method, string, rop);
                string.append("\tisub\n");
            }

            String labelID = "less_true_" + instruction.hashCode();

            string.append("\tiflt " + labelID + "\n");
            string.append("\ticonst_0\n");
            string.append("\tgoto end_less_" + instruction.hashCode() + "\n");
            string.append(labelID + ": \n").append("\ticonst_1\n");
            string.append("end_less_" + instruction.hashCode() + ":\n");

        }
        else {
            //load or lcd operands to stack
            MyJasminUtils.loadElement(method, string, lop);
            MyJasminUtils.loadElement(method, string, rop);
            string.append("\t" + MyJasminUtils.parseType(rop.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            string.append(MyJasminUtils.parseInstruction(opType) + "\n");
        }

        return string.toString();
    }


    public static boolean detectIncrementOperation(AssignInstruction assign, Method method) {
        Operand variable = (Operand) assign.getDest();
        String destName = variable.getName();

        Instruction rhsInst = assign.getRhs();
        if (rhsInst.getInstType() == InstructionType.BINARYOPER) {
            BinaryOpInstruction rhsBin = (BinaryOpInstruction) rhsInst;

            Element leftOp = rhsBin.getLeftOperand();
            Element rightOp = rhsBin.getRightOperand();

            if (rhsBin.getUnaryOperation().getOpType() != OperationType.ADD)
                return false;

            if (leftOp.isLiteral() && ((LiteralElement) leftOp).getLiteral().equals("1"))  {
                return MyJasminUtils.getElementName(rightOp).equals(destName);
            }

            if (rightOp.isLiteral() && ((LiteralElement) rightOp).getLiteral().equals("1")) {
                return MyJasminUtils.getElementName(leftOp).equals(destName);
            }
        }
        return false;
    }
  
}
