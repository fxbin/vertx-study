package cn.fxbin.learn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;

/**
 * MysqlVerticle
 *
 * @author fxbin
 * @version v1.0
 * @since 2020/9/12 14:35
 */
public class MysqlVerticle extends AbstractVerticle {

  MySQLConnectOptions connectOptions = new MySQLConnectOptions()
    .setPort(3306)
    .setHost("localhost")
    .setDatabase("test")
    .setUser("root")
    .setPassword("123456");

  // Pool options
  PoolOptions poolOptions = new PoolOptions()
    .setMaxSize(5);

  Router router;


  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // Create the client pool
    MySQLPool client = MySQLPool.pool(vertx, connectOptions, poolOptions);


    router.route("/test/list/:page").handler(req -> {

      List<JsonObject> list = new ArrayList<>();

      Integer page = Integer.valueOf(req.request().getParam("page"));


      client.getConnection(ar1 -> {

        if (ar1.succeeded()) {

          System.out.println("Connected");

          // Obtain our connection
          SqlConnection conn = ar1.result();

          Integer offset = (page-1) * 4;
          // All operations execute on the same connection
          conn
//            .query("SELECT * FROM roles")
            .preparedQuery("SELECT * FROM roles limit 4 offset ?")
            .execute(Tuple.of(offset),ar2 -> {
              // Release the connection to the pool
              conn.close();

              if (ar2.succeeded()) {
                ar2.result().forEach(item -> {
                  JsonObject json = new JsonObject();
                  json.put("username", item.getValue("username"));
                  json.put("role", item.getValue("role"));
                  list.add(json);
                });

                req.response()
                  .putHeader("content-type", "application/json")
                  .putHeader("charset", "utf8")
                  .end(list.toString());
              } else {
                req.response()
                  .putHeader("content-type", "text/plain")
                  .putHeader("charset", "utf8")
                  .end(ar2.cause().getMessage());
              }
            });
        } else {
          System.out.println("Could not connect: " + ar1.cause().getMessage());
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
