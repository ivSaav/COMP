package InstructionHandlers;

import org.specs.comp.ollir.*;

import java.util.*;

public class MyJasminUtils {

    static void loadElement(Method method, StringBuilder string, Element op) {
        if (op.isLiteral()){
            LiteralElement literal = (LiteralElement) op;
            string.append("\tldc "+literal.getLiteral()+" \n");

        }else{
            System.out.println("== \nDESC " + ((Operand) op).getName() + " " + ((Operand) op).getType());

            HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);

            Operand variable = (Operand) op;
            Descriptor d = vars.get(variable.getName());

            if (d.getVarType().getTypeOfElement() == ElementType.OBJECTREF) {
                string.append("\ta");
            }
            else if (d.getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                System.out.println(d.getVarType());
                // array access
                if (op.getType().getTypeOfElement() == ElementType.INT32) {
                    string.append("\taload " + d.getVirtualReg() + "\n");
                    ArrayOperand arrayOp = (ArrayOperand) op;

                    String name = MyJasminUtils.getElementName((arrayOp).getIndexOperands().get(0));

                    Descriptor desc = vars.get(name);
                    string.append("\tiload " + desc.getVirtualReg() + "\n");
                }
                else {
                    System.out.println("olha sou um array normal");
                }
                return;
            }
            else {
//                System.out.println("INT : " +  ((Operand) op).getName() );
                string.append("\t" + MyJasminUtils.parseType(op.getType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            }


            string.append("load "+ d.getVirtualReg()+"\n");
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
        switch (type){
            case INT32:
                return "I";
            case BOOLEAN:
                return "B";
            case ARRAYREF:
                return "[I";
            case OBJECTREF:
                return "Ljava/lang/Object";
            case CLASS:
                return "C";
            case THIS:
                return "T";
            case STRING:
                return "Ljava/lang/String";
            case VOID:
                return "V";
            default:
                return null;
        }
    }

    public static String parseOperationType(OperationType type){
        switch (type) {
            case LTH:
            case LTHI32:
                return "lt";
            case GTH:
            case GTHI32:
                return "gt";
            case EQ:
            case EQI32:
                return "eq";
            case NEQ:
            case NEQI32:
                return "ne";
            case LTE:
            case LTEI32:
                return "le";
            case GTE:
            case GTEI32:
                return "ge";
            case MUL:
            case MULI32:
                return "mul";
            case DIV:
            case DIVI32:
                return "div";
            case ADD:
            case ADDI32:
                return "add";
            case SUB:
            case SUBI32:
                return "sub";
            case AND:
            case ANDB:
            case ANDI32:
                return "and";
            case OR:
            case ORB:
                return "or";
            case NOT:
            case NOTB:
                return "not";
            case XOR:
            case XORI32:
                return "xor";
            case SHL:
            case SHR:
            case SHRR:
            case ORI32:
            case SHLI32:
            case SHRI32:
            case SHRRI32:
            default:
                return "another";
        }
    }

    public static Type getVarType(int regID, Map<String, Descriptor> varTable) {

        for (Map.Entry<String, Descriptor> entry : varTable.entrySet()) {
            if (entry.getValue().getVirtualReg() == regID)
                return entry.getValue().getVarType();
        }

        return null;
    }


    public static void printVarTable(Map<String, Descriptor> vars) {
        for (Map.Entry<String, Descriptor> entry : vars.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue().getVirtualReg() + " " + entry.getValue().getVarType());
        }
    }
}
