package freshlyground.compiler.integration;

import freshlyground.compiler.backend.java.JavaGenerator;

import freshlyground.compiler.frontend.artifacts.common.Token;
import freshlyground.compiler.frontend.Analyzer;
import freshlyground.compiler.frontend.artifacts.Ast;
import freshlyground.compiler.frontend.Lexer;
import freshlyground.compiler.frontend.Parser;
import freshlyground.compiler.semantic.Bindings;

import freshlyground.compiler.semantic.Types;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class JavaIntegrationTests {
    private static String wrapProgramInput(String fields, String methods, String mainInterior) {
        List<String> lines = new ArrayList<>();

        // 1) Fields
        if (fields != null && !fields.isBlank()) {
            lines.addAll(Arrays.asList(fields.split("\\R", -1)));
        }

        // 2) Methods (non-main)
        if (methods != null && !methods.isBlank()) {
            if (!lines.isEmpty()) lines.add(""); // blank line between sections
            lines.addAll(Arrays.asList(methods.split("\\R", -1)));
        }

        // 3) Main
        if (!lines.isEmpty()) lines.add(""); // blank line before main if prior content exists
        lines.add("DEF main(): Integer DO");

        if (mainInterior != null && !mainInterior.isBlank()) {
            lines.addAll(Arrays.asList(mainInterior.split("\\R", -1)));
        }

        lines.add("    RETURN 0;");
        lines.add("END");

        return String.join(System.lineSeparator(), lines);
    }

    private static String wrapProgramOutput(String fields, String methods, String mainBody) {
        List<String> lines = new ArrayList<>(List.of(
            "public class Main {",
            ""
        ));

        // 1) Fields
        if (fields != null && !fields.isBlank()) {
            lines.addAll(Arrays.asList(fields.split("\\R", -1)));
            lines.add("");
        }

        // 2) Java launcher
        lines.addAll(List.of(
            "    public static void main(String[] args) {",
            "        System.exit(new Main().main());",
            "    }",
            ""
        ));

        // 3) Other methods
        if (methods != null && !methods.isBlank()) {
            lines.addAll(Arrays.asList(methods.split("\\R", -1)));
            lines.add("");
        }

        // 4) main() implementation
        lines.add("    int main() {");

        if (mainBody != null && !mainBody.isBlank()) {
            lines.addAll(Arrays.asList(mainBody.split("\\R", -1)));
        }

        lines.addAll(List.of(
            "        return 0;",
            "    }",
            "",
            "}"
        ));

        return String.join(System.lineSeparator(), lines);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSourceCode(String test, String input, String expected) {
        // code -> LEXER -> tokens
        List<Token> tokens = new Lexer(input).lex();

        // tokens -> PARSER -> ast
        Ast ast = new Parser(tokens).parse();

        // ast -> ANALYZER -> ast (decorated)
        Bindings bindings = new Analyzer().decorate(ast);

        test(expected, ast, bindings);
    }

    private static Stream<Arguments> testSourceCode() {
        return Stream.of(
            Arguments.of("Wrapper",
                wrapProgramInput(null, null, null),
                wrapProgramOutput(null, null, null)
            ),
            Arguments.of("Builtin print()",
                wrapProgramInput(null, null, "    print(\"Hello, World!\");"),
                wrapProgramOutput(null, null, "        System.out.println(\"Hello, World!\");")
            ),
            Arguments.of("Builtin stringify()",
                wrapProgramInput("LET x: Integer = 1;", null, "    x.stringify();"),
                wrapProgramOutput("    int x = 1;", null, "        x.toString();")
            ),
            Arguments.of("Multiple Fields and Methods",
                wrapProgramInput(
                    String.join(System.lineSeparator(),
                        "LET x: Integer;",
                        "LET y: Decimal;",
                        "LET z: String;"
                    ),
                    String.join(System.lineSeparator(),
                        "DEF f(): Integer DO RETURN x; END",
                        "DEF g(): Decimal DO RETURN y; END",
                        "DEF h(): String DO RETURN z; END"
                    ),
                    null
                ),
                wrapProgramOutput(
                    String.join(System.lineSeparator(),
                        "    int x;",
                        "    double y;",
                        "    String z;"
                    ),
                    String.join(System.lineSeparator(),
                        "    int f() {",
                        "        return x;",
                        "    }",
                        "",
                        "    double g() {",
                        "        return y;",
                        "    }",
                        "",
                        "    String h() {",
                        "        return z;",
                        "    }"
                    ),
                    null
                )
            ),
            Arguments.of("For Loop",
                wrapProgramInput(
                    null,
                    null,
                    "FOR (; TRUE; ) LET x = 1; END"
                ),
                wrapProgramOutput(
                    null,
                    null,
                    String.join(System.lineSeparator(),
                        "        for ( ; true; ) {",
                            "            int x = 1;",
                            "        }"
                    )
                )
            )
        );
    }

    // LEXER - PARSER - ANALYZER - GENERATOR
    private static void test(String expected, Ast ast, Bindings bindings) {
        // ast (decorated) -> GENERATOR -> java
        String result = new JavaGenerator(bindings).emit(ast);

        Assertions.assertEquals(expected, result);
    }
}
