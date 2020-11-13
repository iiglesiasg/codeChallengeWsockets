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
      System.out.println("Connected");
      connect.exceptionHandler(ex -> {
        System.out.println("Exception");
        ex.printStackTrace();
      });
      connect.closeHandler(exit -> {
        System.out.println("Exited");
      });
    });
    client.webSocket("/", sock -> {
      if (sock.succeeded()) {
        System.out.println("Connected");
        WebSocket socket = sock.result();
        int milis=1000;
        Boolean full = false;
        vertx.executeBlocking(promise->{
          while (true) {
            String number = String.format("%09d", VertxContextPRNG.current(vertx).nextInt(1000000000));

            socket.writeTextMessage(number);
            while (socket.writeQueueFull()){
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }
          }
        },res->System.out.println("Fin"));
      }});


      /*  while (true) {
          String number = String.format("%09d", VertxContextPRNG.current(vertx).nextInt(1000000000));

          socket.writeTextMessage(number);

          //  vertx.setPeriodic(1,every ->{
       /*   String number = String.format("%09d", VertxContextPRNG.current(vertx).nextInt(1000000000));
          vertx.executeBlocking(promise ->{
            String numberf = String.format("%09d", VertxContextPRNG.current(vertx).nextInt(1000000000));
            promise.complete(numberf);
          },res -> socket.writeTextMessage((String) res.result()));

          Future<String> number = Future.future(fut-> String.format("%09d", VertxContextPRNG.current(vertx).nextInt(1000000000)));
          if(number.succeeded()) {
            socket.writeTextMessage(number.result());
          }
          if(socket.writeQueueFull()){
            nanos=nanos*10;
          }else{
            nanos=nanos/2;
          }

          //socket.writeTextMessage(number);

          try {
            Thread.sleep(0, nanos);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
*/

        //  });
  //    }

   // });


/*
    vertx.createNetClient().connect(4000, "localhost", sock -> {
      if (sock.succeeded()) {
        System.out.println("Connection Succeed!");
        NetSocket client = sock.result();
        vertx.setPeriodic(1000, kk -> {
          String number = String.format("%09d", VertxContextPRNG.current(vertx).nextInt(1000000000));
          client.write(number,jj -> {
            if (jj.succeeded())System.out.println(number);
          });
        });
      } else {
        System.out.println("Connection Failed!");
      }
    });*/
  }
}
