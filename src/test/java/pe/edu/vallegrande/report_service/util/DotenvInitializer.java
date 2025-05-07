package pe.edu.vallegrande.report_service.util;

import io.github.cdimascio.dotenv.Dotenv;

public class DotenvInitializer {
    static {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
    }
}
