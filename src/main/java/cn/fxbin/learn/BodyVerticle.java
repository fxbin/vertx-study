package cn.fxbin.learn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class BodyVerticle extends AbstractVerticle {

  Router router;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // form-data 格式
    // content-type: application/x-www-form-urlencoded
    router.route("/test/form").handler(req -> {
      String page = req.request().getFormAttribute("page");
      req.response()
        .putHeader("content-type", "text/plain")
        .end(page);
    });

    router.route("/test/json").handler(req -> {
      JsonObject json = req.getBodyAsJson();
      req.response()
        .putHeader("content-type", "text/plain")
        .end(json.toString());
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
