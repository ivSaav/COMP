package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.Locale;
import java.util.concurrent.atomic.LongAccumulator;

public class CondBranchInstructionHandler implements IntructionHandler{
    private CondBranchInstruction condBranchInstruction;

    public CondBranchInstructionHandler(Instruction branchInstruction) {

        this.condBranchInstruction = (CondBranchInstruction) branchInstruction;
    }

    public String handleInstruction(ClassUnit classUnit, Method method) {
        String label=condBranchInstruction.getLabel();
        StringBuilder string = new StringBuilder();

        Element lop = condBranchInstruction.getLeftOperand();
        Element rop = condBranchInstruction.getRightOperand();

        //load or lcd operands to stack
        MyJasminUtils.loadElement(method, string, lop);
        MyJasminUtils.loadElement(method, string, rop);

        OperationType opType = condBranchInstruction.getCondOperation().getOpType();
        this.buildBranchCondition(opType, lop, rop, label, string, method);
        return string.toString();
    }

    private  void buildBranchCondition(OperationType type, Element lop, Element rop, String label,
                                       StringBuilder builder, Method method){

        switch (type) {
            case LTH:
            case LTHI32:
                builder.append("\tif_icmplt ").append(label).append("\n"); // val1 < val2
                break;
            case GTH:
            case GTHI32:
                builder.append("\tif_icmpgt ").append(label).append("\n");
                break;
            case EQ:
            case EQI32:
                builder.append("\tif_icmpeq ").append(label).append("\n");
                break;
            case NEQ:
            case NEQI32:
                builder.append("\tif_icmpne ").append(label).append("\n");
                break;
            case LTE:
            case LTEI32:
                builder.append("\tif_icmple ").append(label).append("\n");
                break;
            case GTE:
            case GTEI32:
                builder.append("\tif_icmpge ").append(label).append("\n");
                break;
            case AND:
            case ANDB:
            case ANDI32:
                // when branch condition is like:
                // if ( var && true )
                if (rop.isLiteral() && ((LiteralElement) rop).getLiteral().equals("1")) {
                    builder.setLength(0); // remove loaded vars
                    MyJasminUtils.loadElement(method, builder, lop); // only load lhs
                }
                else {
                    builder.append("\tiand\n");
                }
                builder.append("\tifne ").append(label).append("\n");
                break;
            case OR:
            case ORB:
            case ORI32:
                builder.append("\tior\n");
                builder.append("\tifeq ").append(label).append("\n");
                break;
            default:
                builder.append("INVALID");
                break;
        }

    }
}

