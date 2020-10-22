package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import lombok.NonNull;
import org.baylor.ecs.cloudhubs.sourcecrawler.model.LogType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class LogParser {
    @NonNull List<LogType> logTypes;
    @NonNull List<Boolean> printed;
    @NonNull String log;

    public LogParser(List<LogType> logTypes, String log) {
        this.logTypes = logTypes;
        printed = new ArrayList<>();
        this.log = log;

        Arrays.stream(log.split("\n")).forEach(line -> {
            for (var logType : logTypes) {
                if (line.matches(logType.getRegex())) {
                    printed.set(logTypes.indexOf(logType), true);
                }
            }
        });
    }

    public boolean wasLogExecuted(LogType log) {
        var logType = find(log);
        return logType.isPresent() ? printed.get(logTypes.indexOf(logType.get())) : false;
    }

    private Optional<LogType> find(LogType log) {
        for (var logType : logTypes) {
            if (log.equals(logType)) {
                return Optional.of(logType);
            }
        }
        return Optional.empty();
    }
}
