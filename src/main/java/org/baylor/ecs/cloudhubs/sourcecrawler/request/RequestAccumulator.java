package org.baylor.ecs.cloudhubs.sourcecrawler.request;
import javafx.util.Pair;
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

    /**
     * Reads JSON file the clustered log file
     * @param fileName The file name of the clustered logs
     */
    public static FullCluster readJSON(String fileName) throws FileNotFoundException {
        JacksonJsonParser parser = new JacksonJsonParser();
        Gson gson = new Gson();

        //Grab the json string from the file
        Scanner in = new Scanner(new File(fileName));
        StringBuilder jsonString = new StringBuilder();
        while(in.hasNextLine()){
            jsonString.append(in.nextLine());
        }
        in.close();

        FullCluster logClusters = new FullCluster(); // Holds all the final log clusters

        //Read initial JSON object into a map of token length -> list of clusters
        // Triple list is due to output from json
        Map<Integer, List<List<List<String>>>> parsedMap = new LinkedHashMap<>();
        parsedMap = gson.fromJson(jsonString.toString(), parsedMap.getClass());

        List<?> finalClusterList = new ArrayList<>();

        //Process the json into a suitable format for our structure
        for(Map.Entry<Integer, List<List<List<String>>>> entry : parsedMap.entrySet()){
            System.out.println(entry.getKey());

            List<String> outerList = entry.getValue().get(0).get(0);
            List<?> temp_list = entry.getValue().get(0).get(1);


            //Un-nest additional list around token list layer and create new single list
            List<List<String>> tokenList = new ArrayList<>();
            for(Object t : outerList){
                ArrayList<String> innerList = (ArrayList)t;
                tokenList.add(innerList);
            }

            //Convert generic values of variable list to booleans
            List<Boolean> variableList = new ArrayList<>();
            for(Object b: temp_list){
                variableList.add((Boolean)b);
            }

            //Create the log cluster
            //Assuming the variable list and token list are same length
            LogCluster cluster = new LogCluster();
            cluster.setTokenList(tokenList);
            cluster.setVariableList(variableList);

            cluster.printCluster();

            //Add into the full cluster
            Object k = entry.getKey();
            Integer tokenSize = Integer.parseInt((String)k);
            logClusters.getFullCluster().
                    put(tokenSize, cluster);
        }

        return logClusters;
    }


    public static void main(String[] args) throws FileNotFoundException {
        FullCluster clusteredLogs =
                readJSON("src/main/java/org/baylor/ecs/cloudhubs/sourcecrawler/request/test.json");

        clusteredLogs.display();
    }
}

/**
 * These are the log clusters parsed from the JSON file of clustered logs
 * Map of log groups that have been fully clusters
 *          dict = {key=token_size, value=list of final_cluster}
 *          final_cluster = [ list of tokenized log statements, variable_list]
 *          variable_list = list of boolean values that indicate whether the token at the same index in the given cluster
 * #                         is variable or not.
 */
class FullCluster{
    protected Map<Integer, LogCluster> fullCluster; //Token size + list of final clusters

    FullCluster(){
        fullCluster = new HashMap<>(); // Can use linked hash map to keep insertion order
    }

    public Map<Integer, LogCluster> getFullCluster(){
        return fullCluster;
    }

    public void display(){
        for(Map.Entry<Integer, LogCluster> entry: fullCluster.entrySet()){
            System.out.println(entry.getKey());
            entry.getValue().printCluster();
            System.out.println();
        }
    }
}

/**
 * A log cluster with a list of tokenized log statements and variable value
 */
class LogCluster{

    protected List<List<String>> tokenList;  //A list of clusters, with each list containing a list of tokens
    protected List<Boolean> variableList;    //Variable bool for a token (index is for each token in it's own list)

    public LogCluster(){
        tokenList = new ArrayList<>();
        variableList = new ArrayList<>();
    }

    public List<List<String>> getTokenList() {
        return tokenList;
    }

    public void setTokenList(List<List<String>> tokenList) {
        this.tokenList = tokenList;
    }

    public List<Boolean> getVariableList() {
        return variableList;
    }

    public void setVariableList(List<Boolean> variableList) {
        this.variableList = variableList;
    }

    public void printCluster(){
        for (List<String> strings : tokenList) {
            for (int index = 0; index < strings.size(); index++) {
                System.out.println(strings.get(index) + " - " + variableList.get(index));
            }
        }
    }
}

/**
 * Represents a node that is traversed by each request
 * Includes start and end timestamps
 */
class RequestNode{
    protected Instant rnStart;
    protected Instant rnEnd;
}

