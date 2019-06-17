/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.bcel.classfile.ClassParser;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives
 * into a single call graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
public class JCallGraph {
    private List<File> jarFiles;
    private List<Pattern> includedIdentifiers;
    private List<ClassVisitor> classVisitors;
    private BufferedWriter log;

    public static void main(String[] args) {
        List<File> jarFiles = Arrays.stream(args).map(File::new).collect(Collectors.toList());
        List<Pattern> includedIdentifiers = retrieveIncludedExcludedPatterns(System.getProperty("incl"));

        JCallGraph jCallGraph = new JCallGraph(jarFiles, includedIdentifiers);
        jCallGraph.addLog(new BufferedWriter(new OutputStreamWriter(System.out)));
        jCallGraph.analyze();
    }

    public JCallGraph(List<File> jarFiles, List<Pattern> includedIdentifiers) {
        this.jarFiles = jarFiles;
        this.includedIdentifiers = includedIdentifiers;
    }

    public void analyze() {
        Function<ClassParser, ClassVisitor> getClassVisitor =
                (ClassParser cp) -> {
                    try {
                        return new ClassVisitor(cp.parse(), includedIdentifiers);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                };

        try {
            for (File jarFile : jarFiles) {
                if (!jarFile.exists()) System.err.println("Jar file " + jarFile.getPath() + " does not exist");

                try (JarFile jar = new JarFile(jarFile)) {
                    Stream<JarEntry> entries = enumerationAsStream(jar.entries());

                    this.classVisitors = entries
                            .flatMap(e -> {
                                if (e.isDirectory() || !e.getName().endsWith(".class") || !JCallGraph.identifierIsIncluded(e.getName().replace('/', '.'), includedIdentifiers)) {
                                    return Stream.empty();
                                }

                                ClassParser cp = new ClassParser(jarFile.getAbsolutePath(), e.getName());
                                return Stream.of(getClassVisitor.apply(cp).start());
                            })
                            .collect(Collectors.toList());

                    writeToLog();
                }
            }

            if (log != null) log.close();
        } catch (IOException e) {
            System.err.println("Error while processing jar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addLog(BufferedWriter log) {
        this.log = log;
    }

    private void writeToLog() throws IOException {
        if (log != null) {
            for (ClassVisitor classVisitor : classVisitors) {
                log.write(concatInvokations(classVisitor.getClassInvokations()));
            }

            for (ClassVisitor classVisitor : classVisitors) {
                log.write(concatInvokations(classVisitor.getMethodCalls()));
            }
        }
    }

    private String concatInvokations(List<String> invokations) {
        return invokations
                .stream()
                .map(s -> s + "\n")
                .reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append).toString();
    }

    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            public T next() {
                                return e.nextElement();
                            }

                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED
                ),
                false
        );
    }

    public List<ClassVisitor> getClassVisitors() {
        return classVisitors;
    }

    public static List<Pattern> retrieveIncludedExcludedPatterns(String argument) {
        String[] patterns = argument.split(",");

        if (patterns.length < 1) return null;

        List<Pattern> compiledPatterns = new ArrayList<>();

        for (String pattern : patterns) {
            try {
                compiledPatterns.add(Pattern.compile(pattern + "$"));
            } catch (PatternSyntaxException pse) {
                err("pattern: " + pattern + " not valid, ignoring");
            }
        }

        return compiledPatterns;
    }

    private static void err(String msg) {
    }

    static boolean identifierIsIncluded(String identifier, List<Pattern> includedIdentifiers) {
        if (includedIdentifiers == null) return true;
        for (Pattern includedIdentifier : includedIdentifiers) {
            if (includedIdentifier.matcher(identifier).matches()) return true;
        }
        return false;
    }
}
