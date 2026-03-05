package freshlyground.server;

import freshlyground.api.CompilerService;
import freshlyground.common.CompilerException;
import io.javalin.Javalin;

public final class CompilerServer {
    private CompilerServer() {}

    public static void main(String[] args) {
        int port = 7070;

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        });

        // Healthcheck
        app.get("/health", ctx -> ctx.result("ok"));

        // Compile endpoint
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

        app.start(port);
        System.out.println("FreshlyGround server listening on http://localhost:" + port);
    }

    // JSON request/response payloads
    public record CompileRequest(String source) {}
    public record CompileResponse(String code) {}
    public record CompileError(String message, Integer index) {}
}
