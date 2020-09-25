package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import lombok.Getter;
import lombok.Value;
import soot.ClassSource;
import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;

import java.util.ArrayList;
import java.util.List;

@Value
public class ProjectParser {
    @Getter
    private final SourceLocator srcLocator;
    @Getter
    private final List<SootClass> sootClasses;

    public ProjectParser(String projectRoot) {
        super();
        srcLocator = SourceLocator.v();
        Scene.v().setSootClassPath(projectRoot);
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
