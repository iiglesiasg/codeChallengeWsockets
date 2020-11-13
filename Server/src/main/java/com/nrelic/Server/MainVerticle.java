package com.nrelic.Server;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

public class MainVerticle extends AbstractVerticle {

  private static ArrayList<Integer> list;
  private static SortedSet<Integer> fileDigits;
  private static int connections;
  private static int newDigits = 0;
  private static int repeatedDigits = 0;

  private AsyncFile file;

  public static void main(String args[]) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    //fileDigits = new ArrayList<Integer>();
    fileDigits = new TreeSet<Integer>();
    startChallenge(vertx);

    // Scheduler (in miliseconds)
    vertx.setPeriodic(10 * 1000, kk -> {
      Future.future(f -> System.out.println("Received " + newDigits + " unique numbers, " + repeatedDigits + " duplicates. Unique Total: " + fileDigits.stream().count() + " Connections: " + connections));
      newDigits = 0;
      repeatedDigits = 0;
    }); // End Scheduler
  }

  private void startChallenge(Vertx vertx) {
    /*
        Everytime it boots it will delete and recreate the numbers.log file.
        When ready
    */
    vertx.fileSystem().delete("numbers.log", delete -> {
      if (delete.succeeded()) {
        System.out.println("File numbers.log deleted");
        vertx.fileSystem().open("numbers.log", new OpenOptions().setWrite(true).setCreate(true), openedFile());

      } else {
        System.out.println("Failed to delete ");
        vertx.fileSystem().open("numbers.log", new OpenOptions().setWrite(true).setCreate(true), openedFile());
      }
    });


  }

  private void startServer(Vertx vertx) {
    HttpServerOptions options = new HttpServerOptions();
    HttpServer server = vertx.createHttpServer(options);

    // Handles new connections to the server
    server.connectionHandler(conn -> {
      if (connections == 5) {
        conn.close();
      } else connections++;
    });

    // Handles Socket connections (After an update request)
    server.webSocketHandler(ws -> {

      // Write return message when socket joined
      ws.writeTextMessage("Joined");

      // Handles connection closed event
      ws.closeHandler(close -> {
        connections--;
      });

      // Handles websockets incoming messages
      ws.textMessageHandler(message -> {

        // Future, Vertx Non blocking
        Future.future(x -> {
          int intMessage = Integer.parseInt(message);
          if (message.length() == 9 && message.matches("\\d+")) {
            if (fileDigits.add(intMessage)) {
              newDigits++;
              x.complete();

            } else {
              repeatedDigits++;
              x.fail("kk");
            }
          } else { // Bad Input
            ws.reject();
            x.fail("kk");
          }
        }) // when logic is done, write to file
          .onComplete(write -> {
            file.write(Buffer.buffer(message + System.lineSeparator()));

            //BackPressure
            if (file.writeQueueFull()) {
              Future.future(f -> System.out.println("BackPressure"));
              ws.pause();
              file.drainHandler(done -> {
                ws.resume();
              });
            } // End Backpressure
          });

      }); // End TextMessage Handler

    }) // End websocket Handler
      // Bind the server
      .listen(4000, res -> {
        if (res.succeeded()) {
          System.out.println("Server is now listening on port 4000");
        } else {
          System.out.println("Failed to bind!");
        }
      });
  }

  // As this code is called from 2 sources, it has his own function.
  private Handler<AsyncResult<AsyncFile>> openedFile() {
    return event -> {
      if (event.succeeded()) {
        file = event.result();

        System.out.println("File Created");
        startServer(vertx);

      } else {
        System.out.println("Failed to Open/Create File");
      }
    };
  }
}
