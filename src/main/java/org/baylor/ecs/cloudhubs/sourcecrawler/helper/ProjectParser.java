package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import soot.ClassSource;
import soot.SootClass;
import soot.SourceLocator;

import java.util.ArrayList;
import java.util.List;

public class ProjectParser {
    private SourceLocator srcLocator;
    private List<SootClass> sootClasses;

    // TODO add lombok and switch to JUnit 5cC

    public ProjectParser(String projectRoot) {
        super();
        srcLocator = SourceLocator.v();
        var classes = srcLocator.getClassesUnder(projectRoot);
        var classSources = new ArrayList<ClassSource>();
        classes
            .parallelStream()
            .iterator()
            .forEachRemaining((c) -> classSources.add(srcLocator.getClassSource(c)));

        sootClasses = new ArrayList();
        var srcIter = classSources.iterator();
        for (var c : classes) {
            var sc = new SootClass(c);
            srcIter.next().resolve(sc);
            sootClasses.add(sc);
        }
    }

}
