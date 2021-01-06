# Programming-Languages-and-Compilers
# using java to implement a compiler for vc language
## how to use
First download VC and jasmin-2.4 files to the directory you want to run the compiler.
### compile
Go to directory you put VC and jasmin and run and compile the whole program.
```bash
javac -cp . VC/vc.java
```
you finish the compilation of the vc compiler.now go to the lang directory.All built-in functions for VC are in System.java so you also need to compile it
```bash
javac System.java
```
now you finish all the setup
### run the vc compiler
Go to directory you put VC and jasmin and use the vc compiler to compile your vc code
```bash
javac -cp . VC/vc [-options] filename
```
where options include:
```bash
        -d [1234]           display the AST (without SourcePosition)
                            1:  the AST from the parser (without SourcePosition)
                            2:  the AST from the parser (with SourcePosition)
                            3:  the AST from the checker (without SourcePosition)
                            4:  the AST from the checker (with SourcePosition)
        -t [file]           print the (non-annotated) AST into <file>
                            (or filename + "t" if <file> is unspecified)
        -u [file]           unparse the (non-annotated) AST into <file>
                            (or filename + "u" if <file> is unspecified
```
if your code passes all the checks from the compiler, it will produce a ```filename.j``` file, which is a jasmin assembly code
### jasmin assembler
To use jasmin, cd to jasmin-2.4 directory and run:
```bash
java -jar jasmin.jar filename
```
then jasmin assembler will convert ```.j``` file into ```.class``` file. Read more details about jasmin at ```Readme.txt``` in jasmin-2.4
### run your code
Run:
```bash
java filename
```
your code has been compiled and can run now. Enjoy!
