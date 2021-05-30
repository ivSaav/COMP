### **GROUP:** 4E

**NAME1:** Isla Cassamo, **NR1:** 201808549, **GRADE1:** 14, **CONTRIBUTION1:** 25%  
**NAME2:** Ivo Saavedra, **NR2:** 201707093, **GRADE2:** 17, **CONTRIBUTION2:** 30%  
**NAME3:** Maria Baía,   **NR3:** 201704951, **GRADE3:** 16, **CONTRIBUTION3:** 30%  
**NAME4:** Rodrigo Reis, **NR4:** 201806534, **GRADE4:** 12 , **CONTRIBUTION4:** 15%    

### **GLOBAL Grade of the project:** 17

### **SUMMARY:**
This project aimed to develop a compiler for java--, derived from the java language. 
The work was divided into 4 main groups, parser generation, syntactic analysis, semantic analysis and code generation.
We only implemented the optimization flag.
In order to run the compiler you need to specify the -o flag for optimizations and specify the test file.  
**Command:** java -jar comp2021-4e.jar -o \<filename\>


### **DEALING WITH SYNTACTIC ERRORS:**

Whenever a syntatical is encountered the parser aborts execution and displays the errors in the console.
However if the error happened inside a 'while' loop then we have a limit of 10 errors that can be displayed at the same time, before aborting the syntaic analysis.


### **SEMANTIC ANALYSIS:**

For evaluating semantic erros, it starts by calling the function semanticAnalysis, using the class AJmmVisitor which iterates through every node from the ast.

Our analysis covers the following points:
	
- verify if operations are carried out with the same type
- verify if the access to an array variable is done correctly
- verify if destination has been declared before assignment
- verify if the destination and the assignment have the same data type
- verify if an operation is made by operator with the same type as the variables
- verify if there are operations between two arrays
- verify if a variable was already declared and if his type exists
- verify if a global variable is called in the main
- verify if conditional expressions (if and while) result in a boolean
- verify if a method is defined in the class or in one of the imports
- verify if the number of arguments in the method 
- verify if the type of the parameters matches the type of the arguments


### **CODE GENERATION:**

The ollir code generation is done in the 'OllirEmitter' class which uses the provided AJmmVisitor in order to deal with the different components of a java-- class.
We defined two visitors one for the 'Class' node to handle imports and create the constructor and the other for the 'Method' nodes in which we generate all the code of a given method.
All the method's statements are generated recursivly from the method handler.

The main problem of our code generator is that it uses a lot of auxiliary variables and most of which are redundant and could be removed in order to reduce the number of instructions used. Another point that could be made is the large amount of code in the same class that could have been prevented if we had specialized each handler function into new classes. For example one class that deals only with whiles, another that deals with staments and so on. 
Other than these two points our ollir code generator works as intendend.


### **TASK DISTRIBUTION:** 

**CHEKPOINT 1:** 
 - For this checkpoint the work was divided eavenly between the four elements (25% for each);

**CHECKPOINT 2:**
 - Semantic Analysis: Ivo Saavedra (30%), Maria Baía (30%), Isla Cassamo (20%), Rodrigo Reis (20%)
 - Symbol Table: Ivo Saavedra (60%), Maria Baía (40%)
 - Ollir Code: Ivo Saavedra (50%), Maria Baía (50%)
 - Jasmin (Expressions): Isla Cassamo (50%), Rodrigo Reis (50%)
 - Tests (HelloWorld and Simple): Ivo Saavedra (100%)

**CHECKPOINT 3:** 
 - Ollir Code (fixing previous errors): Ivo Saavedra (50%), Maria Baía (50%)
 - Jasmin Code (remaining elements): Ivo Saavedra (45%), Maria Baía (45%)
 - Public Tests: Ivo Saavedra (60%), Maria Baía (40%)
 - Private Tests: Ivo Saavedra (33%), Maria Baía (33%), Isla Cassamo (33%)


**PROS:**  
	- Optimization to reduce jasmin statements for if's, negating them  
	- Optimization to turn whiles into do whiles are done


**CONS:**  
	- The register allocation optimization was not implemented  
	- The constant folding optimization was not implemented