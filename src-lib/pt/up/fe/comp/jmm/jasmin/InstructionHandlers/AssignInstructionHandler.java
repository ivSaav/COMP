package pt.up.fe.comp.jmm.jasmin.InstructionHandlers;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;

import java.util.HashMap;
import java.util.Locale;

public class AssignInstructionHandler implements IntructionHandler{

    private AssignInstruction instruction;
    private Method method;

    public AssignInstructionHandler(Instruction instruction, Method method) {
        this.instruction = (AssignInstruction) instruction;
        this.method = method;
    }

    @Override
    public String handleInstruction() {
        StringBuilder string = new StringBuilder();
        //instruction.show();

        //this part is fine
        InstructionAllocator rhs = new InstructionAllocator();

        String rhss= rhs.allocateAndHandle(instruction.getRhs(),method );
        string.append(rhss);




        //HashMap<String, Descriptor> vars= OllirAccesser.getVarTable(method);

       // Descriptor d = vars.get(instruction.getDest()); //d is null?
        //string.append(d);
        //if(d.getScope()== VarScope.FIELD){

        //}else{
            //string.append(JasminUtils.parseType(d.getVarType().getTypeOfElement()).toLowerCase(Locale.ROOT));
            //string.append("store ");
            //string.append(d.getVirtualReg());
        //}
        //como distinguir entre store e pufield


        /*
                jasminCode.append("\t" + instruction.toString()+"\n");

                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                Stack stack = new Stack();
                for (String var : varTable.keySet()){
                    stack.push(var);
                }

                generate((Node) assignInstruction, jasminCode, stack);*/

        return string.toString();
    }
}
