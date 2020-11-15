package com.nrelic;

import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.ext.auth.VertxContextPRNG;

public class MainVerticle extends AbstractVerticle {

  public static void main(String args[]) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    HttpClientOptions options = new HttpClientOptions().setDefaultPort(4000).setDefaultHost("localhost");
    HttpClient client = vertx.createHttpClient(options).connectionHandler(connect -> {
      connect.exceptionHandler(ex -> {
        System.out.println("Exception");
        ex.printStackTrace();
      });
      connect.closeHandler(exit -> {
        System.out.println("Exited");
      });
    });

    // socket message sender
    client.webSocket("/", sock -> {
      if (sock.succeeded()) {
        System.out.println("Connected socket");
        WebSocket socket = sock.result();

        // As there is no input stream but an infinite loop we have to cheat vertX with a blocking piece of code.
        vertx.executeBlocking(promise -> {
          while (true) {
            // Only send if the queue is not full.
            if (!socket.writeQueueFull()) {
              Future.<String>future(f -> // Generate ramdom 9 digits with left pad
                f.complete(String.format("%09d", VertxContextPRNG.current(vertx).nextInt(1000000000)))
              ).onComplete(number -> socket.writeTextMessage(number.result()));
            }
          }
        }, res -> System.out.println("Fin"));

      }
    });

  }
}
