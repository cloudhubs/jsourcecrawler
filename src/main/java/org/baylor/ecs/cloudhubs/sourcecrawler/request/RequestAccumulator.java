package org.baylor.ecs.cloudhubs.sourcecrawler.request;
import lombok.extern.java.Log;
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
    protected List<String> methods; // Contains a list of top level methods TODO: Data type? (SootMethod or CFG)
    protected List<String> logPointSequences; // TODO: Data type? List of log point sequences (each comes from a top-level method)
    protected List<String> nodes;    // TODO: List of nodes traversed (Change into node data type with start & end timestamps for each)
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

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public List<String> getLogPointSequences() {
        return logPointSequences;
    }

    public void setLogPointSequences(List<String> logPointSequences) {
        this.logPointSequences = logPointSequences;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
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

        RequestAccumulator ra = new RequestAccumulator();

        clusteredLogs.display();
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

