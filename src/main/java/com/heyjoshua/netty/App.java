package com.heyjoshua.netty;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.heyjoshua.netty.http.ExampleServer;

public class App {
  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new AppModule());

    ExampleServer server = injector.getInstance(ExampleServer.class);
    server.start();
  }
}
