import java.util.*;

import org.specs.comp.ollir.*;

import org.specs.comp.ollir.Node;
import InstructionHandlers.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
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

    private List<Report> reports;

    public BackendStage() {
        this.reports = new ArrayList<>();
    }

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {

            // Example of what you can do with the OLLIR class
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
//            ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method
//            ollirClass.show(); // print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            StringBuilder jasminCode = new StringBuilder(); // Convert node ...

            handleClass(ollirClass, jasminCode);
            handleFields(ollirClass, jasminCode);
            handleMethods(ollirClass, jasminCode);

//            System.out.println("JASMIN CODE\n" + jasminCode.toString());

            Utils.saveContents(jasminCode.toString(), "jasmin.j");

            return new JasminResult(ollirResult, jasminCode.toString(), reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }

    private void handleMethods(ClassUnit ollirClass, StringBuilder jasminCode) {
        for(Method method : ollirClass.getMethods()){
            HashMap<String, Descriptor> varTable = OllirAccesser.getVarTable(method);

            jasminCode.append(".method public ");
            if(method.isStaticMethod()) jasminCode.append("static ");
            if(method.isFinalMethod()) jasminCode.append("final ");
            if(method.isConstructMethod()){
                jasminCode.append("<init>(");
                parseMethodParameters(method.getParams(), jasminCode);
                jasminCode.append(")V\n");
            }
            else {
                String methodName = method.getMethodName();

                jasminCode.append(methodName + "(");
                if (methodName.equals("main")){
                    jasminCode.append("[Ljava/lang/String;");

                }else{
                    parseMethodParameters(method.getParams(), jasminCode);
                }
                jasminCode.append(")"+ MyJasminUtils.parseTypeForMethod(method.getReturnType().getTypeOfElement()));


                //LIMITS
                int localVariables = 1;   // this
                for(Map.Entry<String, Descriptor> variable : varTable.entrySet()){
                    if (variable.getValue().getScope().equals(VarScope.LOCAL))
                        localVariables++;
                    if (variable.getValue().getScope().equals(VarScope.PARAMETER))
                        localVariables++;
                    if (variable.getValue().getScope().equals(VarScope.FIELD))
                        localVariables++;
                }

                jasminCode.append("\n\t"+ ".limit locals " + localVariables);
                
                jasminCode.append("\n\t" + ".limit stack " + localVariables);
            }
            jasminCode.append("\n");

            handleInstructions(jasminCode, ollirClass, method, varTable);

            jasminCode.append(".end method \n\n");

        }
    }

    private void handleInstructions(StringBuilder jasminCode, ClassUnit classUnit,Method method, HashMap<String, Descriptor> varTable) {

        InstructionAllocator instructionAllocator = new InstructionAllocator();

        for (Instruction instruction : method.getInstructions()) {
            jasminCode.append(instructionAllocator.allocateAndHandle(instruction, classUnit,method));
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
        jasminCode.append(".class public ");
        AccessModifiers accessModifiers = ollirClass.getClassAccessModifier();
        if(!accessModifiers.equals(AccessModifiers.DEFAULT)) jasminCode.append(ollirClass.getClassAccessModifier()+ " ");
        if(ollirClass.isFinalClass()) jasminCode.append("final ");
        if(ollirClass.isStaticClass()) jasminCode.append("static ");
        jasminCode.append(ollirClass.getClassName());
        if (ollirClass.getSuperClass() != null)
            jasminCode.append("\n.super " +ollirClass.getSuperClass() + "\n\n");
        else
            jasminCode.append("\n.super java/lang/Object\n\n");
    }

    private void handleFields(ClassUnit ollirClass, StringBuilder jasminCode) {
        //.field <access-spec> <field-name> <signature> [ = <value> ]
        for(Field field : ollirClass.getFields()){

            jasminCode.append(".field ");
            if(field.isStaticField()) jasminCode.append("static ");
            jasminCode.append(field.getFieldName()+" ");
            jasminCode.append(MyJasminUtils.parseType(field.getFieldType().getTypeOfElement()));

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
            jasminCode.append(MyJasminUtils.parseTypeForMethod(paramList.get(i).getType().getTypeOfElement()));
        }

    }
}


