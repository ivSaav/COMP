import java.util.*;

import org.specs.comp.ollir.*;

import org.specs.comp.ollir.Node;
import pt.up.fe.comp.jmm.jasmin.InstructionHandlers.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class BackendStage implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {

            // Example of what you can do with the OLLIR class
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method
            //ollirClass.show(); // print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            StringBuilder jasminCode = new StringBuilder(); // Convert node ...

            handleClass(ollirClass, jasminCode);
            handleFields(ollirClass, jasminCode);


            for(Method method : ollirClass.getMethods()){
                HashMap<String, Descriptor> varTable = OllirAccesser.getVarTable(method);

                jasminCode.append(".method public ");
                if(method.isStaticMethod()) jasminCode.append("static ");
                if(method.isFinalMethod()) jasminCode.append("final ");
                if(method.isConstructMethod()){
                    jasminCode.append("<init>(");
                    parseMethodParameters(method.getParams(), jasminCode);
                    jasminCode.append(")V\n");
                    jasminCode.append("\taload_0");
                }else{

                    String methodName = method.getMethodName();

                    jasminCode.append(methodName + "(");
                    if (methodName.equals("main")){
                        jasminCode.append("[Ljava/lang/String;");

                    }else{
                        parseMethodParameters(method.getParams(), jasminCode);
                    }
                    jasminCode.append(")"+ JasminUtils.parseType(method.getReturnType().getTypeOfElement()));


                    //LIMITS

                    int localVariables = 0;

                    for(Map.Entry<String, Descriptor> variable : varTable.entrySet()){
                        if (variable.getValue().getScope().equals(VarScope.LOCAL))
                            localVariables++;
                    }
                    jasminCode.append("\n\t"+ ".limit locals " + localVariables);

                }jasminCode.append("\n");


                handleInstructions(jasminCode, method, varTable);

                jasminCode.append(".end method \n\n");

            }

            System.out.println("JASMIN CODE\n" + jasminCode.toString());



            // More reports from this stage
            List<Report> reports = new ArrayList<>();
            //jasminCode=SpecsIo.getResource("fixtures/public/jasmin/Greeter.j");

            return new JasminResult(ollirResult, jasminCode.toString(), reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }

    private void handleInstructions(StringBuilder jasminCode, Method method, HashMap<String, Descriptor> varTable) {

        InstructionAllocator instructionAllocator = new InstructionAllocator();

        for (Instruction instruction : method.getInstructions()) {
            jasminCode.append(instructionAllocator.allocateAndHandle(instruction, method));
        }

    }


    private void generate(Node T, StringBuilder jasminCode, Stack<String> stack) {

        System.out.println("GENERATE");

        Node leftNode = T.getSucc1();
        Node rightNode = T.getSucc2();

        if (leftNode == null && rightNode == null) {
            System.out.println("ULTIMA");
            jasminCode.append(T.toString() + stack.peek().toString() + leftNode.toString() + rightNode.toString());
        } else if (leftNode != null || rightNode != null){
            System.out.println("fudeu");
            System.out.println(genrs(rightNode));
            System.out.println("aqui");
            //System.out.println(leftNode+"|"+rightNode.getSucc1());

            if (rightNode.getSucc1()==null){
                System.out.println("fudeu aqui");
                //generate(leftNode, jasminCode);
                //jasminCode.append(T.toString() + stack.peek().toString() + leftNode.toString() + rightNode.toString());
            }

            if (genrs(leftNode)>= genrs(rightNode)){
                System.out.println("entrou aqui");
                generate(leftNode, jasminCode, stack);
                System.out.println("chegou aaqui tambem");
                String r = stack.pop();
                System.out.printf("chegou aqui aqui tambem");
                generate(rightNode, jasminCode, stack);
                System.out.printf("chegou aqui aqui aqui tambem");
                stack.push(r);
            }

            else{
                System.out.println("else");
                swapTopElements(stack);

                generate(rightNode, jasminCode, stack);
                String r = stack.pop();
                generate(leftNode, jasminCode, stack);
                stack.push(r);

                swapTopElements(stack);
            }
        }else{
            System.out.println("entrei no else");
        }


    }

    private int genrs(Node n ){

        if (n.getSucc2() != null)return  2;
        if (n.getSucc1()!= null)return 1;
        return 0;
    }


    private void swapTopElements(Stack<String> stack){
        String first = stack.pop();
        String second = stack.pop();
        stack.push(first);
        stack.push(second);
    }

    private void handleClass(ClassUnit ollirClass, StringBuilder jasminCode) {
        jasminCode.append(".class ");
        AccessModifiers accessModifiers = ollirClass.getClassAccessModifier();
        if(!accessModifiers.equals(AccessModifiers.DEFAULT)) jasminCode.append(ollirClass.getClassAccessModifier()+ " ");
        if(ollirClass.isFinalClass()) jasminCode.append("final ");
        if(ollirClass.isStaticClass()) jasminCode.append("static ");
        jasminCode.append(ollirClass.getClassName());
        if (ollirClass.getPackage()==null){
            jasminCode.append("\n.super java/lang/Object\n\n");
        }

    }

    private void handleFields(ClassUnit ollirClass, StringBuilder jasminCode) {
        //.field <access-spec> <field-name> <signature> [ = <value> ]
        for(Field field : ollirClass.getFields()){

            jasminCode.append(".field ");
            if(field.isStaticField()) jasminCode.append("static ");
            jasminCode.append(field.getFieldName()+" ");
            jasminCode.append(JasminUtils.parseType(field.getFieldType().getTypeOfElement()));

            if(field.isInitialized()){
                jasminCode.append(" = ");
                jasminCode.append(field.getInitialValue());
            }

            jasminCode.append("\n");

        }
        jasminCode.append("\n");
    }


    public void parseMethodParameters(ArrayList<Element> paramList, StringBuilder jasminCode){

        int paramListSize = paramList.size();

        for(int i=0; i < paramListSize; i++){
            jasminCode.append(JasminUtils.parseType(paramList.get(i).getType().getTypeOfElement()));
            if(i!=paramListSize-1){
                jasminCode.append(";");
            }
        }

    }



    public String parseOperationType(OperationType type){
        switch (type){

            /**
             * COND BRANCH OPERATIONS
             */

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



            /*case MUL:

            case DIV:
                        SHR,
                        SHL,
                        SHRR,
                        XOR,
                        AND,
                        OR,

                        ADDI32,
                        SUBI32,
                        MULI32,
                        DIVI32,
                        SHRI32,
                        SHLI32,
                        SHRRI32,
                        XORI32,



                        ANDB,  // boolean
                        ORB, // boolean
                        NOTB, // boolean
                        case NOT:*/
        }
        return null;

    }




}


