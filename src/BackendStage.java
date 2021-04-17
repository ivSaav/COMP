import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.specs.comp.ollir.*;

import org.specs.comp.ollir.Node;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

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
                jasminCode.append(".method public ");
                if(method.isStaticMethod()) jasminCode.append("static ");
                if(method.isFinalMethod()) jasminCode.append("final ");
                if(method.isConstructMethod()){
                    jasminCode.append("<init>(");
                    //if main
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
                    jasminCode.append(")"+parseType(method.getReturnType().getTypeOfElement()));
                }jasminCode.append("\n");



                //TODO limits
                handleInstructions(jasminCode, method);



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

    private void handleInstructions(StringBuilder jasminCode, Method method) {
        for (Instruction instruction : method.getInstructions()){

            switch (instruction.getInstType()){
                case RETURN:
                    ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                    ElementType returnType = method.getReturnType().getTypeOfElement();

                    if(returnType != null && returnType!=ElementType.VOID){
                        jasminCode.append("\t" + parseType(returnType).toLowerCase(Locale.ROOT));
                    }

                    jasminCode.append("return ");

                    jasminCode.append("\n");
                    break;



                case GETFIELD:
                    GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                    jasminCode.append("\tgetfield ");
                    break;
                case ASSIGN:
                    AssignInstruction assignInstruction = (AssignInstruction) instruction;
                    //TODO
                    break;
                case GOTO:
                    GotoInstruction gotoInstruction = (GotoInstruction) instruction;
                    jasminCode.append("\tgoto " + gotoInstruction.getLabel()+"\n");
                    break;
                case BRANCH:
                    CondBranchInstruction condBranchInstruction = (CondBranchInstruction) instruction;
                    Operation operation = condBranchInstruction.getCondOperation();

                    //TODO CAST
                    //Operand operand = (Operand) condBranchInstruction.getLeftOperand();
                    //LiteralElement literalElement = (LiteralElement) condBranchInstruction.getRightOperand();

                    //TODO CHECK IF OPERANDS

                    //jasminCode.append("\tif" + parseOperationType(operation.getOpType())+" "+ condBranchInstruction.getLabel()+"\n");
                    break;
                case CALL:
                    //callType methodspec  numargs

                    CallInstruction callInstruction = (CallInstruction) instruction;

                    //TODO INVOCATION TYPE

                     Operand classOperand = (Operand) callInstruction.getFirstArg();
                     String className = classOperand.getName();

                     LiteralElement methodLiteral = (LiteralElement) callInstruction.getSecondArg();
                     String methodName = methodLiteral.getLiteral().substring(1, methodLiteral.getLiteral().length()-1);

                     if(className.equals("this"))
                         className = "java/lang/Object";

                     jasminCode.append("\t"+className+"/" +methodName+ "(");

                     for(Element element: callInstruction.getListOfOperands()){
                         Operand operand1 = (Operand) element;

                         jasminCode.append(parseType(operand1.getType().getTypeOfElement()));
                     }


                     jasminCode.append(")");

                     jasminCode.append(parseType(callInstruction.getReturnType().getTypeOfElement())+"\n");
                    break;
                default:
                    jasminCode.append("\t" + instruction.toString()+"\n");
                    break;
            }
        }
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
            jasminCode.append(parseType(field.getFieldType().getTypeOfElement()));

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
            jasminCode.append(parseType(paramList.get(i).getType().getTypeOfElement()));
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


    public String parseType(ElementType type){
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
                return "S";
            case VOID:
                return "V";
            default:
                return null;
        }

    }

}


