package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import lombok.Getter;
import lombok.Value;
import soot.*;
import soot.options.Options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class ProjectParser {
    @Getter
    SourceLocator srcLocator;
    @Getter
    List<SootClass> sootClasses;
//        Scene.v().setSootClassPath(projectRoot);
//        Options.v().set_whole_program(true);
//        Options.v().set_prepend_classpath(true);
//        Options.v().set_soot_classpath(".:/usr/lib/jvm/java-14-openjdk/lib/jrt-fs.jar");
//        Scene.v().loadNecessaryClasses();

    public ProjectParser(String projectRoot) {
        super();
        soot.G.reset();
        srcLocator = SourceLocator.v();
        Options.v().set_verbose(true);
        String[] dirs = {projectRoot};
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Arrays.stream(dirs).collect(Collectors.toList()));
        Scene.v().loadNecessaryClasses();
        List<String> classes = srcLocator.getClassesUnder(projectRoot);
        List<ClassSource> classSources = new ArrayList<>();

        classes
            .parallelStream()
            .iterator()
            .forEachRemaining((c) -> classSources.add(srcLocator.getClassSource(c)));

        sootClasses = new ArrayList();
        Iterator<ClassSource> srcIter = classSources.iterator();
        for (String c : classes) {
            SootClass sc = new SootClass(c);
            var src = srcIter.next();
            src.resolve(sc);
            sootClasses.add(sc);
        }
    }

    public List<SootMethod> getMethods() {
        List<SootMethod> methods = new ArrayList<>();
        sootClasses.forEach(sc -> methods.addAll(sc.getMethods()));
        methods.forEach(m -> m.setActiveBody(m.retrieveActiveBody()));
        return methods;
    }

}
