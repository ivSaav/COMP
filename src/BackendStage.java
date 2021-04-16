import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.specs.comp.ollir.*;

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

            jasminCode.append(".class public ");
            if(ollirClass.isFinalClass()) jasminCode.append("final ");
            if(ollirClass.isStaticClass()) jasminCode.append("static ");
            jasminCode.append(ollirClass.getClassName());

            jasminCode.append("\n.super java/lang/Object\n\n");


            for(Field field : ollirClass.getFields()){
                if(field.getFieldType().equals("ARRAYREF")){
                    jasminCode.append("; array "+ field.getFieldName()+"\n");
                }

                jasminCode.append(".field ");
                if(field.isStaticField()) jasminCode.append("static ");
                jasminCode.append(field.getFieldName()+" ");
                jasminCode.append(parseType(field.getFieldType()));

                if(field.isInitialized()){
                    jasminCode.append(" = ");
                    jasminCode.append(field.getInitialValue());
                }

                jasminCode.append("\n");

            }


            for(Method method : ollirClass.getMethods()){
                jasminCode.append(".method public ");
                if(method.isStaticMethod()) jasminCode.append("static ");
                if(method.isFinalMethod()) jasminCode.append("final ");
                if(method.isConstructMethod()){
                    jasminCode.append("<init>(");
                    parseMethodParameters(method.getParams(), jasminCode);
                    jasminCode.append(")V");
                }else{
                    jasminCode.append(method.getMethodName() + "(");
                    parseMethodParameters(method.getParams(), jasminCode);
                    jasminCode.append(")"+parseType(method.getReturnType()));
                }



                for (Instruction inst : method.getInstructions()){
                    //System.out.println();
                }


                jasminCode.append("\n.end method \n\n");

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



    public void parseMethodParameters(ArrayList<Element> paramList, StringBuilder jasminCode){
        for(Element element: paramList){
            jasminCode.append(parseType(element.getType()));
        }
    }


    public String parseType(Type type){
        switch (type.getTypeOfElement()){
            case INT32:
                return "I";
            case BOOLEAN:
                return "B";
            case ARRAYREF:
                return "[I";
            case OBJECTREF:
                return "O";
            case CLASS:
                return "C";
            case THIS:
                return "T";
            case STRING:
                return "S";
            case VOID:
                return "V";
        }
        return null;
    }

}


