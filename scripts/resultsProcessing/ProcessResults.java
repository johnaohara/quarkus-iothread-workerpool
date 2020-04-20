//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.aesh:aesh:2.6
//DEPS com.google.code.gson:gson:2.8.6

import com.google.gson.Gson;
import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;

import static java.lang.System.exit;

public class ProcessResults {

    public static void main(String[] args) {
        AeshRuntimeRunner.builder().interactive(true).command(ProcessResultsCmd.class).args(args).execute();
    }

    @CommandDefinition(name = "parser", description = "Microbenchmark results parser")
    public static class ProcessResultsCmd implements Command {

        @OptionList(required = true, shortName = 'i', description = "input file locations", valueSeparator = ':')
        List<String> inputFiles;

        @Option(required = true, shortName = 'o', description = "Path to output file")
        private static String outputPath;

        @Option(required = true, shortName = 'c', description = "Number of cores benchmark ran on")
        private static double cpuCount;


        @Override
        public CommandResult execute(CommandInvocation commandInvocation) {

            BenchmarkResult result = new BenchmarkResult();

            EnumSet.allOf(FILE_TYPES.class).forEach(file_type -> {
                inputFiles.forEach(fileName -> {
                    File inputFile = new File(fileName);
                    if (!inputFile.exists() || !inputFile.isDirectory()) {
                        System.out.println("Please specify a valid directory for Client Output Path: ".concat(fileName));
                        exit(1);
                    }
                    processFiles(result, file_type, inputFile.listFiles(file_type.fileFilter));
                });
            });

            System.out.println("Finished Parsing");

            Gson gson = new Gson();

            try (FileWriter fileWriter = new FileWriter(outputPath)) {
                fileWriter.write(gson.toJson(result));
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return CommandResult.SUCCESS;
        }

    }

    private enum FILE_TYPES {
        BUILD("(\\S*-\\S*-\\S*)\\.build\\.out", Parsers.buildParser),
        RUN("(\\S*-\\S*-\\S*)\\.run\\.log", Parsers.runParser),
        TEST("(\\S*-\\S*-\\S*)\\.test\\.out", Parsers.testParser),
        FIRST_REQUEST("(\\S*-\\S*-\\S*)\\.timeFirstRequest\\.out", Parsers.firstRequestParser),
        DOCKER_STATS("(\\S*-\\S*-\\S*)-([0-9]*)-MEASURE-stats\\.out", Parsers.serverStatsParser),
        CLIENT_RESULTS("(\\S*-\\S*-\\S*)-([0-9]*)-MEASURE\\.wrk2\\.out", Parsers.clientResultsParser),
        DSTAT("(\\S*-\\S*-\\S*)-([0-9]*)-MEASURE\\.dstat\\.out", Parsers.dstatParser),
        PROC_RSS("(\\S*-\\S*-\\S*)-([0-9]*)-MEASURE\\.procRss\\.out", Parsers.procRssParser),
        TOP("(\\S*-\\S*-\\S*)-([0-9]*)-MEASURE-top\\.out", Parsers.topParser);

        public final String filenamePattern;
        public final PatternFilter fileFilter;
        public final Parsers.OutputParser parser;

        FILE_TYPES(String filenamePattern, Parsers.OutputParser parser) {
            this.filenamePattern = filenamePattern;
            this.fileFilter = new PatternFilter(filenamePattern);
            this.parser = parser;
        }
    }

    static class PatternFilter implements FilenameFilter {

        private Pattern pattern;

        protected PatternFilter(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        public boolean accept(File file, String s) {
            return this.pattern.matcher(s).matches();
        }
    }

    private static void processFiles(BenchmarkResult result, FILE_TYPES fileType, File[] targetFiles) {
        for (File file : targetFiles) {
            System.out.println("Parsing: ".concat(file.getAbsoluteFile().getAbsolutePath()));
            try {
                fileType.parser.parseFile(result, file, fileType.filenamePattern);
            } catch (IOException e) {
                System.out.println("Exception occurred trying to parse: ".concat(file.getAbsolutePath()));
                e.printStackTrace();
            }
        }

    }

    static class BenchmarkResult {
        private Map<String, RuntimeResult> benchmarkResults = new HashMap<>();
        private Map<String, Set<GraphPoint>> graphData = new HashMap<>();

        private Set<GraphPoint> findgraphData(String graphKey) {
            if (!graphData.containsKey(graphKey)) {
                graphData.put(graphKey, new TreeSet<>());
            }
            return graphData.get(graphKey);
        }


        private RuntimeResult findRuntimeResult(String runtime) {
            if (!benchmarkResults.containsKey(runtime)) {
                benchmarkResults.put(runtime, new RuntimeResult());
            }
            return benchmarkResults.get(runtime);
        }

        public RuntimeResult getRuntimeResult(String runtime) {
            return findRuntimeResult(runtime);
        }

        private static class GraphPoint implements Comparable {
            public GraphPoint(Double key, Double value) {
                this.key = key;
                this.value = value;
            }

            public Double key;
            public Double value;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                GraphPoint that = (GraphPoint) o;
                return Objects.equals(key, that.key) &&
                        Objects.equals(value, that.value);
            }

            @Override
            public int hashCode() {
                return Objects.hash(key, value);
            }

            @Override
            public int compareTo(Object o) {
                return (this.key.equals(((GraphPoint) o).key)) ? 0 : (this.key < ((GraphPoint) o).key) ? -1 : 1;
            }
        }
    }

    static class RuntimeResult {

        public Double avgStartTime;
        public Double avgBuildTime;
        public Double avgTestTime;
        private Map<String, RuntimeRunResult> runtimeRunResults = new TreeMap<>();

        public RuntimeRunResult getRuntimeResult(String txRate) {
            if (!runtimeRunResults.containsKey(txRate)) {
                runtimeRunResults.put(txRate, new RuntimeRunResult());
            }
            return runtimeRunResults.get(txRate);
        }

        public Map<String, RuntimeRunResult> getRuntimeRunResults() {
            return runtimeRunResults;
        }

    }

    static class RuntimeRunResult {
        public Double loadThroughput;
        public Double responseActualThroughput;
        public Double responseTimeAvg;
        public Double responseTime50;
        public Double responseTime75;
        public Double responseTime90;
        public Double responseTime99;
        public Double responseTime999;
        public Double responseTime9999;
        public Double responseTime99999;
        public Double responseTimeMax;
        public Double requests;
        public Double avgRss;
        public Double minRss;
        public Double maxRss;
        public Double avgCpu;
        public Double minCpu;
        public Double maxCpu;

    }

    private static class Parsers {

        interface OutputParser {
            void parseFile(BenchmarkResult result, File file, String pattern) throws IOException;
        }

        static final OutputParser buildParser = (result, file, pattern) -> {
            result.getRuntimeResult(extractRuntimeName(file, pattern)).avgBuildTime = averageResultCol(file, "Total time", 3);
        };

        static final OutputParser testParser = (result, file, pattern) -> {
            result.getRuntimeResult(extractRuntimeName(file, pattern)).avgTestTime = averageResultCol(file, "Total time", 3);
        };

        static final OutputParser runParser = (result, file, pattern) -> {
            //Do nothing with run log
        };

        static final OutputParser firstRequestParser = (result, file, pattern) -> {
            result.getRuntimeResult(extractRuntimeName(file, pattern)).avgStartTime = averageResultCol(file, "ms", 0);
        };

        static final OutputParser serverStatsParser = (result, file, pattern) -> {
            String runtime = extractRuntimeName(file, pattern);
            String txRate = extractLoadRate(file, pattern);

            RuntimeRunResult runtimeResult = result.getRuntimeResult(runtime).getRuntimeResult(txRate);

            runtimeResult.avgRss = averageResultCol(file, runtime, 3);
            runtimeResult.minRss = minResultCol(file, runtime, 3);
            runtimeResult.maxRss = maxResultCol(file, runtime, 3);

            runtimeResult.avgCpu = averageResultCol(file, runtime, 2) / ProcessResultsCmd.cpuCount;
            runtimeResult.minCpu = minResultCol(file, runtime, 2) / ProcessResultsCmd.cpuCount;
            runtimeResult.maxCpu = maxResultCol(file, runtime, 2) / ProcessResultsCmd.cpuCount;

            result.findgraphData(runtime.concat("-avgRss")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.avgRss));
            result.findgraphData(runtime.concat("-avgCpu")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.avgCpu));


        };

        public static OutputParser topParser = (result, file, pattern) -> {
            String runtime = extractRuntimeName(file, pattern);
            String txRate = extractLoadRate(file, pattern);

            RuntimeRunResult runtimeResult = result.getRuntimeResult(runtime).getRuntimeResult(txRate);

            //top outputs RSS in KB
            runtimeResult.avgRss = averageResultCol(file, null, 5) / 1024;
            runtimeResult.minRss = minResultCol(file, null, 5) / 1024;
            runtimeResult.maxRss = maxResultCol(file, null, 5) / 1024;

            runtimeResult.avgCpu = averageResultCol(file, null, 8) / ProcessResultsCmd.cpuCount;
            runtimeResult.minCpu = minResultCol(file, null, 8) / ProcessResultsCmd.cpuCount;
            runtimeResult.maxCpu = maxResultCol(file, null, 8) / ProcessResultsCmd.cpuCount;

            result.findgraphData(runtime.concat("-avgRss")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.avgRss));
            result.findgraphData(runtime.concat("-avgCpu")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.avgCpu));
        };

        public static OutputParser dstatParser = (result, file, pattern) -> {
            String runtime = extractRuntimeName(file, pattern);
            String txRate = extractLoadRate(file, pattern);

            RuntimeRunResult runtimeResult = result.getRuntimeResult(runtime).getRuntimeResult(txRate);

            runtimeResult.avgCpu = 100.0 - averageResultCol(file, null, 2, 3);
            runtimeResult.minCpu = 100.0 - minResultCol(file, null, 2, 3) / ProcessResultsCmd.cpuCount;
            runtimeResult.maxCpu = 100.0 - maxResultCol(file, null, 2, 3) / ProcessResultsCmd.cpuCount;

            result.findgraphData(runtime.concat("-avgCpu")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.avgCpu));

        };

        public static OutputParser procRssParser = (result, file, pattern) -> {
            String runtime = extractRuntimeName(file, pattern);
            String txRate = extractLoadRate(file, pattern);

            RuntimeRunResult runtimeResult = result.getRuntimeResult(runtime).getRuntimeResult(txRate);

            runtimeResult.avgRss = averageResultCol(file, null, 0);
            runtimeResult.minRss = minResultCol(file, null, 0);
            runtimeResult.maxRss = maxResultCol(file, null, 0);

            result.findgraphData(runtime.concat("-avgRss")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.avgRss));

        };

        static final OutputParser clientResultsParser = (result, file, pattern) -> {
            String runtime = extractRuntimeName(file, pattern);
            String txRate = extractLoadRate(file, pattern);
            RuntimeRunResult runtimeResult = result.getRuntimeResult(runtime).getRuntimeResult(txRate);

            runtimeResult.loadThroughput = Double.parseDouble(txRate);
            runtimeResult.requests = singleResultCol(file, "requests", 0);
            runtimeResult.responseActualThroughput = singleResultCol(file, "Requests/sec:", 1);
            runtimeResult.responseTimeAvg = singleResultCol(file, "#[Mean", 2);
            runtimeResult.responseTimeMax = singleResultCol(file, "#[Max", 2);
            runtimeResult.responseTime50 = singleResultCol(file, "50.000%", 1);
            runtimeResult.responseTime75 = singleResultCol(file, "75.000%", 1);
            runtimeResult.responseTime90 = singleResultCol(file, "90.000%", 1);
            runtimeResult.responseTime99 = singleResultCol(file, "99.000%", 1);
            runtimeResult.responseTime999 = singleResultCol(file, "99.900%", 1);
            runtimeResult.responseTime9999 = singleResultCol(file, "99.990%", 1);
            runtimeResult.responseTime99999 = singleResultCol(file, "99.999%", 1);

            //Measuring response time vs request throughput - otherwise see strange graphs as throughput drops at saturation
            result.findgraphData(runtime.concat("-meanResponse")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.responseTimeAvg));
            result.findgraphData(runtime.concat("-maxResponse")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.responseTimeMax));
            result.findgraphData(runtime.concat("-actualTxrate")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.responseActualThroughput));
            result.findgraphData(runtime.concat("-response-50")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.responseTime50));
            result.findgraphData(runtime.concat("-response-90")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.responseTime90));
            result.findgraphData(runtime.concat("-response-99")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.responseTime99));
            result.findgraphData(runtime.concat("-response-99999")).add(new BenchmarkResult.GraphPoint(Double.parseDouble(txRate), runtimeResult.responseTime99999));
        };

        private static String extractRuntimeName(File file, String pattern) {
            return extractCaptureGroup(file, pattern, 1);
        }

        private static String extractLoadRate(File file, String pattern) {
            return extractCaptureGroup(file, pattern, 2);
        }

        private static String extractCaptureGroup(File file, String pattern, int captureGroupOrdinal) {
            final Matcher matcher = Pattern.compile(pattern).matcher(file.getName());
            String reVal = matcher.matches() ? captureGroupOrdinal <= matcher.groupCount() ? matcher.group(captureGroupOrdinal) : null : null;
            return reVal;

        }

        private static Double singleResultCol(File file, String lineFilter, int col) throws IOException {
            return filterValues(file, lineFilter, col, 0)
                    .findFirst()
                    .getAsDouble();
        }

        private static Double averageResultCol(File file, String lineFilter, int col) throws IOException {
            return averageResultCol(file, lineFilter, col, 0);
        }

        private static Double averageResultCol(File file, String lineFilter, int col, int skip) throws IOException {
            return filterValues(file, lineFilter, col, skip)
                    .average()
                    .getAsDouble();
        }

        private static Double maxResultCol(File file, String lineFilter, int col) throws IOException {
            return maxResultCol(file, lineFilter, col, 0);
        }

        private static Double maxResultCol(File file, String lineFilter, int col, int skip) throws IOException {
            return filterValues(file, lineFilter, col, skip)
                    .max()
                    .getAsDouble();
        }

        private static Double minResultCol(File file, String lineFilter, int col) throws IOException {
            return minResultCol(file, lineFilter, col, 0);
        }

        private static Double minResultCol(File file, String lineFilter, int col, int skip) throws IOException {
            return filterValues(file, lineFilter, col, skip)
                    .min()
                    .getAsDouble();
        }

        private static DoubleStream filterValues(File file, String lineFilter, int col, int skipLines) throws IOException {
            return Files.lines(file.toPath())
                    .filter(line -> lineFilter == null ? true : line.contains(lineFilter))
                    .skip(skipLines)
                    .map(line -> line.replaceAll("%", ""))
                    .map(line -> line.replaceAll("MiB", ""))
                    .map(line -> line.replaceAll("ms", ""))
                    .map(line -> line.replaceAll(",", ""))
                    .map(line -> line.trim().replaceAll("\\s+", " "))
                    .filter(line -> !(line.split(" ").length <= col))
                    .map(line -> line.split(" ")[col])
                    .mapToDouble(line -> line.contains("s") ? Double.parseDouble(line.replaceAll("s", "")) * 1000.0 : Double.parseDouble(line));
        }


    }
}