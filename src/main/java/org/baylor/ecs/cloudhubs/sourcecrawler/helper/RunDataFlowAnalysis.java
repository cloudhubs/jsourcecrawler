package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import java.util.Iterator;
import java.util.List;

import soot.*;
import soot.Body;
import soot.NormalUnitPrinter;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPrinter;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Sources;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.jimple.internal.*;

public class RunDataFlowAnalysis
{
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: java RunDataFlowAnalysis class_to_analyze");
            System.exit(1);
        } else {
            System.out.println("Analyzing class: "+args[0]);
        }

        String mainClass = args[0];

        // You may have to update the class Path based on your OS and Java version
        /*** *** YOU SHOULD EDIT THIS BEFORE RUNNING *** ***/
        //TODO: Classpath issue? Might be due to Java version
        //change to appropriate path to the test class
        String classPath = "src";
        // if needed add path to rt.jar (or classes.jar)


        //Set up arguments for Soot
        String[] sootArgs = {
                "-cp", classPath, "-pp", 	// sets the class path for Soot
                "-w", 						// Whole program analysis, necessary for using Transformer
                "-src-prec", "java",		// Specify type of source file
                "-main-class", mainClass,	// Specify the main class
                "-f", "J", 					// Specify type of output file
                mainClass
        };

        // Create transformer for analysis
        AnalysisTransformer analysisTransformer = new AnalysisTransformer();

        // Add transformer to appropriate Pack in PackManager. PackManager will run all Packs when main function of Soot is called
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.dfa", analysisTransformer));

        // Call main function with arguments
        soot.Main.main(sootArgs);

    }

    // Sample function for Call grapoh
//    public void printPossibleCallers(SootMethod target) {
//        CallGraph cg = Scene.v().getCallGraph();
//        Iterator sources = new Sources(cg.edgesInto(target));
//        while (sources.hasNext()) {
//            SootMethod src = (SootMethod)sources.next();
//            System.out.println(target + " could be called by " + src);
//        }
//    }
}
