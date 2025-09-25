package com.pb.chatbot.orchestrator;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import jakarta.enterprise.context.ApplicationScoped;

@QuarkusMain
public class Application {
    public static void main(String[] args) {
        Quarkus.run(ApplicationRunner.class, args);
    }

    @ApplicationScoped
    public static class ApplicationRunner implements QuarkusApplication {
        @Override
        public int run(String... args) throws Exception {
            Quarkus.waitForExit();
            return 0;
        }
    }
}
