package freshlyground.cli;

import freshlyground.api.CompilerService;
import freshlyground.common.CompilerException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Fgc {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: ./build/install/PLC_Project/bin/fgc <input.fg> <output.java>");
            System.exit(1);
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);

        try {
            String source = Files.readString(input, StandardCharsets.UTF_8);
            String result = CompilerService.compile(source);
            Files.writeString(output, result, StandardCharsets.UTF_8);
        } catch (CompilerException e) {
            System.err.println(e.getMessage());
            System.exit(2); // semantic/syntax error
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            System.exit(3);
        }
    }
}