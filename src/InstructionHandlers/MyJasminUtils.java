package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.*;

public class MyJasminUtils {

    static void loadElement(Method method, StringBuilder string, Element op) {

        if (op.isLiteral()){
            LiteralElement literal = (LiteralElement) op;

            int lit = Integer.parseInt(literal.getLiteral());
            if (lit == -1)
                string.append("\ticonst_m1\n");
            else if (lit < 6 && lit >= 0)
                string.append("\ticonst_"+  lit + "\n");
            else
                string.append("\t" + (lit > -129 && lit < 128 ? "bipush " : "ldc ") +literal.getLiteral()+" \n");

        }else{

            HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);

            Operand variable = (Operand) op;

            Descriptor d = vars.get(variable.getName());

            // Case the return value is a literal boolean
            if (d == null && op.getType().getTypeOfElement() == ElementType.BOOLEAN) {
                if (variable.getName().equals("true"))
                    string.append("\ticonst_1 \n");
                else if (variable.getName().equals("false"))
                    string.append("\ticonst_0 \n");
                return;
            }

            ElementType elementType = d.getVarType().getTypeOfElement();
            if (elementType == ElementType.OBJECTREF) {

                string.append("\taload" + (d.getVirtualReg() < 4 ? "_" : " ") + d.getVirtualReg()).append("\n");
            }
            else if (elementType == ElementType.ARRAYREF) {
                // array access
                if (op.getType().getTypeOfElement() == ElementType.INT32) {
                    string.append("\taload" + (d.getVirtualReg() < 4 ? "_" : " ") + d.getVirtualReg() + "\n");
                    ArrayOperand arrayOp = (ArrayOperand) op;

                    String name = MyJasminUtils.getElementName((arrayOp).getIndexOperands().get(0));

                    Descriptor desc = vars.get(name);
                    string.append("\tiload" + (desc.getVirtualReg() < 4 ? "_" : " ") + desc.getVirtualReg() + "\n");
                }
                else {
                    if (d.getScope() != VarScope.FIELD)
                        string.append("\taload" + (d.getVirtualReg() < 4 ? "_" : " ") + d.getVirtualReg() + "\n");
                }
                return;
            }
            else if (elementType == ElementType.INT32 || elementType == ElementType.BOOLEAN){

                string.append("\tiload"+ (d.getVirtualReg() < 4 ? "_" : " ") + d.getVirtualReg()+"\n");
            }
            else {
                string.append("INVALID ======\n"); // debug
            }


        }
    }

    static String getElementName(Element element){
        String name;
        if(element.isLiteral()){
            LiteralElement literal  =  (LiteralElement) element;
            name = literal.getLiteral();
        }else {
            Operand op1= (Operand) element;
            name = op1.getName();
        }
        return name;
    }

    public static String parseType(ElementType type){
        return switch (type) {
            case INT32 -> "I";
            case BOOLEAN -> "I";
            case ARRAYREF -> "[I";
            case OBJECTREF -> "Ljava/lang/Object";
            case CLASS -> "C";
            case THIS -> "T";
            case STRING -> "Ljava/lang/String";
            case VOID -> "V";
        };
    }

    public static String parseTypeForMethod(ElementType type){
        return switch (type) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> "[I";
            case OBJECTREF -> "Ljava/lang/Object";
            case CLASS -> "C";
            case THIS -> "T";
            case STRING -> "Ljava/lang/String";
            case VOID -> "V";
        };
    }

    public static String parseInstruction(OperationType type){
        return switch (type) {
            case LTH, LTHI32 -> "lt";
            case NEQ, NEQI32 -> "neg";
            case MUL, MULI32 -> "mul";
            case DIV, DIVI32 -> "div";
            case ADD, ADDI32 -> "add";
            case SUB, SUBI32 -> "sub";
            case AND, ANDB, ANDI32 -> "and";
            case OR, ORB -> "or";
            case NOT, NOTB -> "xor";
            case XOR, XORI32 -> "xor";
            default -> "another";
        };
    }


    public static void printVarTable(Map<String, Descriptor> vars) {
        for (Map.Entry<String, Descriptor> entry : vars.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue().getVirtualReg() + " " + entry.getValue().getVarType());
        }
    }
}
