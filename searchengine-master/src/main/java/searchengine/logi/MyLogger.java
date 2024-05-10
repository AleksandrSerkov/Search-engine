package searchengine.logi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class MyLogger {

    private static final Logger logger = LoggerFactory.getLogger(MyLogger.class);
    private String data;

    public MyLogger(String data) {
        this.data = data;
        logger.info("Logging information about data saved in DB: " + data);

        // Устанавливаем уровень логирования в MDC (Mapped Diagnostic Context)
        MDC.put("logLevel", "DEBUG");
    }
}


