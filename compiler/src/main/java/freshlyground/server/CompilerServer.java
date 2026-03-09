package freshlyground.server;

import freshlyground.api.CompilerSerializer;
import freshlyground.api.CompilerService;
import freshlyground.common.CompilerException;
import freshlyground.compiler.frontend.Lexer;
import freshlyground.compiler.frontend.Parser;
import freshlyground.compiler.frontend.artifacts.Ast;
import freshlyground.compiler.frontend.artifacts.common.Token;
import io.javalin.Javalin;

import java.util.List;

public final class CompilerServer {
    private CompilerServer() {}

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.allowHost("http://localhost:5173");
                    rule.allowHost("https://freshlyground.onrender.com");
                });
            });
        });

        /* -------------- Healthcheck -------------- */
        app.get("/health", ctx -> ctx.result("ok"));

        /* ---------------- Compile ---------------- */

        app.post("/compile", ctx -> {
            CompileRequest req = ctx.bodyAsClass(CompileRequest.class);

            if (req == null || req.source() == null) {
                ctx.status(400).json(new CompileError("Missing field: source", null));
                return;
            }

            try {
                String code = CompilerService.compile(req.source());
                ctx.status(200).json(new CompileResponse(code));

            } catch (CompilerException e) {
                Integer index = e.getIndex().orElse(null);
                ctx.status(400).json(new CompileError(e.getMessage(), index));
            }
        });

        /* ---------------- Tokens ---------------- */

        app.post("/tokens", ctx -> {
            CompileRequest req = ctx.bodyAsClass(CompileRequest.class);

            if (req == null || req.source() == null) {
                ctx.status(400).json(new CompileError("Missing field: source", null));
                return;
            }

            try {
                Lexer lexer = new Lexer(req.source());
                List<Token> tokens = lexer.lex();

                ctx.status(200).json(CompilerSerializer.serializeTokens(tokens));

            } catch (CompilerException e) {
                Integer index = e.getIndex().orElse(null);
                ctx.status(400).json(new CompileError(e.getMessage(), index));
            }
        });

        /* ---------------- AST ---------------- */

        app.post("/ast", ctx -> {
            CompileRequest req = ctx.bodyAsClass(CompileRequest.class);

            if (req == null || req.source() == null) {
                ctx.status(400).json(new CompileError("Missing field: source", null));
                return;
            }

            try {
                Lexer lexer = new Lexer(req.source());
                List<Token> tokens = lexer.lex();

                Parser parser = new Parser(tokens);
                Ast ast = parser.parse();

                ctx.status(200).json(CompilerSerializer.serializeAst(ast));

            } catch (CompilerException e) {
                Integer index = e.getIndex().orElse(null);
                ctx.status(400).json(new CompileError(e.getMessage(), index));
            }
        });

        app.start("0.0.0.0", port);
        System.out.println("FreshlyGround server listening on http://localhost:" + port);
    }

    // JSON request/response payloads
    public record CompileRequest(String source) {}
    public record CompileResponse(String code) {}
    public record CompileError(String message, Integer index) {}
}
