package cn.fxbin.learn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;

public class TemplateVerticle extends AbstractVerticle {

  Router router;

  ThymeleafTemplateEngine thymeleafTemplateEngine;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);
    thymeleafTemplateEngine = ThymeleafTemplateEngine.create(vertx);

    router.route("/").handler(req -> {

      JsonObject json = new JsonObject();
      json.put("name", "hello world");

      thymeleafTemplateEngine.render(json,
        "templates/index.html",
        bufferAsyncResult -> {
           if (bufferAsyncResult.succeeded()) {
             req.response()
               .putHeader("content-type", "text/html")
               .end(bufferAsyncResult.result());
           }
        });

    });

    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
