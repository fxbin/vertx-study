package cn.fxbin.learn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
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
public class FuturePromiseVerticle extends AbstractVerticle {

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

  MySQLPool client;


  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // Create the client pool
    client = MySQLPool.pool(vertx, connectOptions, poolOptions);


    router.route("/test/list/:page").handler(req -> {

      List<JsonObject> list = new ArrayList<>();

      int page = Integer.parseInt(req.request().getParam("page"));

      this.getConnection()
        .compose(connection -> this.sqlExecute(connection, (page-1) * 4)
          .onSuccess(rows -> {
            rows.forEach(item -> {
              JsonObject json = new JsonObject();
              json.put("username", item.getValue("username"));
              json.put("role", item.getValue("role"));
              list.add(json);
            });

            req.response()
              .putHeader("content-type", "application/json")
              .putHeader("charset", "utf8")
              .end(list.toString());
          })
          .onFailure(throwable -> {
            req.response()
              .putHeader("content-type", "text/plain")
              .putHeader("charset", "utf8")
              .end(throwable.getMessage());
          }))
        .onFailure(throwable -> System.out.println("Could not connect: " + throwable.getMessage()));


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

  private Future<SqlConnection> getConnection() {
    Promise<SqlConnection> promise = Promise.promise();
    client.getConnection(ar1 -> {

      if (ar1.succeeded()) {
        promise.complete(ar1.result());

      } else {
        promise.fail(ar1.cause());
      }
    });
    return promise.future();
  }

  private Future<RowSet<Row>> sqlExecute(SqlConnection connection, Integer offset) {
    Promise<RowSet<Row>> promise = Promise.promise();
    connection.preparedQuery("SELECT * FROM roles limit 4 offset ?")
      .execute(Tuple.of(offset), ar2 ->{
        // Release the connection to the pool
        connection.close();
        if (ar2.succeeded()) {
          promise.complete(ar2.result());
        } else {
          promise.fail(ar2.cause());
        }
      });
    return promise.future();
  }

}
