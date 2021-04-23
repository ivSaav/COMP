package pt.up.fe.comp.jmm.jasmin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import jas.jasError;
import jasmin.ClassFile;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.OperationType;

public class JasminUtils {
    /**
	 * Extracted from Jasmin code
	 */
	public static File assemble(File inputFile, File outputDir) {		
        
        try (FileInputStream fs = new FileInputStream(inputFile);
            InputStreamReader ir = new InputStreamReader(fs);
            BufferedReader inp = new BufferedReader(ir);) {

            ClassFile classFile = new ClassFile();
            classFile.readJasmin(inp, inputFile.getName(), true);

            // if we got some errors, don't output a file - just return.
            if (classFile.errorCount() > 0) {
				throw new RuntimeException ("Found "
                                    + classFile.errorCount() + " errors while compiling Jasmin code.");
                
            }

            String class_path[] = (splitClassField(
                                                classFile.getClassName()));
            String class_name = class_path[1];

            // determine where to place this class file
            //String dest_dir = dest_path;
            if (class_path[0] != null) {
                String class_dir = convertChars(
                                           class_path[0], "./",
                                           File.separatorChar);
                outputDir = new File(outputDir, class_dir);

            }
            //iocause = class_name + ".class: file can't be created";
            // if (dest_dir == null) {
            //     out_file = new File(class_name + ".class");
            // } else {
            File out_file = new File(outputDir, class_name + ".class");

            // check that dest_dir exists

            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            if (!outputDir.isDirectory()) {
                throw new IOException("Cannot create directory: "+outputDir.getAbsolutePath()+" is not a directory.");
            }

            try(FileOutputStream outp = new FileOutputStream(out_file);){
                classFile.write(outp);
            }
            //System.out.println("Generated: " + out_file.getPath());
            return out_file;
        } catch (java.io.FileNotFoundException e) {
            throw new RuntimeException("Class could not be created: "+ e.getMessage(),e);
        } catch (jasError e) {
            throw new RuntimeException("JAS Error: "+ e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("eXception: "+ e.getMessage(), e);
        }
        
    }

	//
    // Splits a string like:
    //    "java/lang/System/out"
    // into two strings:
    //    "java/lang/System" and "out"
    //
    public static String[] splitClassField(String name)
    {
        String result[] = new String[2];
        int i, pos = -1, sigpos = 0;
        for (i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.' || c == '/') pos = i;
        }
        if (pos == -1) {    // no '/' in string
            result[0] = null;
            result[1] = name;
        } else {
            result[0] = convertChars(name.substring(0, pos),".", '/'); // Maps '.' characters to '/' characters in a string
            result[1] = name.substring(pos + 1);
        }

        return result;
    }

    //
    // Maps chars to toChar in a given String
    //
    public static String convertChars(String orig_name,
                                      String chars, char toChar)
    {
        StringBuffer tmp = new StringBuffer(orig_name);
        int i;
        for (i = 0; i < tmp.length(); i++) {
            if (chars.indexOf(tmp.charAt(i)) != -1) {
                tmp.setCharAt(i, toChar);
            }
        }
        return new String(tmp);
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
                return "S";
            case VOID:
                return "V";
            default:
                return null;
        }

    }

    public static String parseOperationType(OperationType type){
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
        //return null;

    }
}
