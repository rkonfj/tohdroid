package sh.toh.app.log;

import com.computablefacts.logfmt.LogFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sh.toh.app.msg.Broadcaster;
import sh.toh.app.msg.MessageBus;

public class Logger extends MessageBus {

    private final ExecutorService executors = Executors.newFixedThreadPool(4);

    public Logger(Broadcaster broadcaster) {
        super(broadcaster);
    }


    public void show(String tag, Process p, boolean logfmt) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        executors.submit(() -> {
            try {
                for (; ; ) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (logfmt) {
                        Map<String, String> labels = LogFormatter.parse(line);
                        if (labels.containsKey("msg")) {
                            show(tag, labels.get("msg"));
                        }
                    } else {
                        show(tag, line);
                    }

                }
            } catch (IOException e) {
                show(tag, e);
            }
        });

        executors.submit(() -> {
            try {
                for (; ; ) {
                    String line = errReader.readLine();
                    if (line == null) {
                        break;
                    }
                    show(tag, line);
                }
            } catch (IOException ignored) {
            }
        });
    }

    public void show(String tag, String log) {
        pub("log", String.format("[%s] %s", tag, log));
    }

    public void show(String tag, Exception e) {
        show(tag, e.getMessage());
    }
}
