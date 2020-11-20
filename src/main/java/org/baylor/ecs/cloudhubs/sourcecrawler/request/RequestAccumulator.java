package org.baylor.ecs.cloudhubs.sourcecrawler.request;
import lombok.extern.java.Log;
import org.baylor.ecs.cloudhubs.sourcecrawler.cfg.CFG;
import org.springframework.boot.json.JacksonJsonParser;
import org.w3c.dom.Node;
import com.google.gson.Gson;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.*;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This class gathers information related to similar requests
 * and its associated log/static analysis information
 */
public class RequestAccumulator {
    protected List<CFG> methods; // Contains a list of top level methods (Using the CFG created)
    protected List<String> logPointSequences; // TODO: Data type? List of log point sequences (each comes from a top-level method)
    protected List<RequestNode> nodes; // List of nodes traversed with start & end timestamps for each)
    protected int requestID;        // Request identifier value (not sure if this needs to be a list)
    protected Instant startTime;    // Start timestamp
    protected Instant endTime;      // End timestamp

    public RequestAccumulator(){
        methods = new ArrayList<>();
        logPointSequences = new ArrayList<>();
        nodes = new ArrayList<>();
        requestID = 0;
        startTime = Instant.now();
        endTime = Instant.now();
    }

    public List<CFG> getMethods() {
        return methods;
    }

    public void setMethods(List<CFG> methods) {
        this.methods = methods;
    }

    public List<String> getLogPointSequences() {
        return logPointSequences;
    }

    public void setLogPointSequences(List<String> logPointSequences) {
        this.logPointSequences = logPointSequences;
    }

    public List<RequestNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<RequestNode> nodes) {
        this.nodes = nodes;
    }

    public int getRequestID() {
        return requestID;
    }

    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }


    public static void main(String[] args) throws FileNotFoundException {
        FullCluster clusteredLogs =
                FullCluster.readJSON("src/main/java/org/baylor/ecs/cloudhubs/sourcecrawler/request/test.json");
        clusteredLogs.display();

        RequestAccumulator ra = new RequestAccumulator();
    }

}


/**
 * Represents a node that is traversed by each request
 * Includes start and end timestamps
 */
class RequestNode{
    protected Instant rnStart;
    protected Instant rnEnd;

    public RequestNode(Instant start, Instant end){
        this.rnStart = start;
        this.rnEnd = end;
    }
}

