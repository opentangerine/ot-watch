package com.opentangerine.watch;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Grzegorz Gajos
 */
// FIXME GG: in progress, extract to different project
public class Doclet {
    static String readme = "";

    public static void main(String[] args) throws Exception {
        // creates an input stream for the file to be parsed
        FileInputStream in = new FileInputStream("src/test/java/com/opentangerine/watch/WatchTest.java");

        CompilationUnit cu;
        try {
            // parse the file
            cu = JavaParser.parse(in);
        } finally {
            in.close();
        }

        // prints the resulting compilation unit to default system output
        new VoidVisitorAdapter() {
            @Override
            public void visit(MethodDeclaration n, Object arg) {
                if(n.getComment() != null && n.getComment().getContent() != null) {
                    String c = n.getComment().getContent();
                    String code = StringUtils.substringBetween(c, "<code>", "</code>");
                    code = StringUtils.replace(code, "*", "").replaceAll("\\s{2,}", " ").trim();
                    readme += code + "\n\n```\n" + n.getBody().toString() + "```\n\n";
                }
                super.visit(n, arg);
            }

            @Override
            public void visit(BlockStmt n, Object arg) {
                super.visit(n, arg);
            }


        }.visit(cu, null);
        System.out.println(readme);
        String content = FileUtils.readFileToString(new File("README.md"), StandardCharsets.UTF_8);
        String head = StringUtils.substringBefore(content, "## Documentation");
        FileUtils.writeStringToFile(new File("README.md"), head + "## Documentation\n\n" + readme, StandardCharsets.UTF_8);
    }

}
