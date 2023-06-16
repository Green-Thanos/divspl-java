package dev.mccue.divspl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

record RangeDeclaration(int from, int to) {}
record Rule(int n, String phrase) {
}


sealed interface ParseResult {}

sealed interface Problem extends ParseResult {
    record EmptyProgram() implements Problem {
        @Override
        public void print(String[] code) {
            System.out.println("""
                    \u001B[1;31merror\u001B[0m: The program you gave me is empty!
                    
                        ¯\\_(ツ)_/¯
                        
                    \u001B[0;36mhint\u001B[0m: Try putting this into the file
                    
                        1...15
                        fizz=3
                        buzz=5
                    """);
        }
    }
    record OnlyPutOneDot(int lineNumber, int indexOfDot) implements Problem {
        @Override
        public void print(String[] code) {
            var split = code[lineNumber].split("\\.", 2);
            var start = split[0];
            var end = split[1];
            System.out.println("""
                    \u001B[1;31merror\u001B[0m: The range declaration requires three dots.
                    
                      |
                    %d |   %s
                      |   %s
                        
                    \u001B[0;36mhint\u001B[0m: Add two more dots between the %s and the %s like so.
                    
                        %s...%s
                    """.formatted(
                            lineNumber + 1,
                            code[lineNumber],
                            " ".repeat(indexOfDot) + "^",
                            start,
                            end,
                            start,
                            end
            ));
        }
    }
    record OnlyPutTwoDots(int lineNumber, int startIndexOfDots) implements Problem {
        @Override
        public void print(String[] code) {
            var split = code[lineNumber].split("\\.\\.", 2);
            var start = split[0];
            System.out.println("""
                    \u001B[1;31merror\u001B[0m: The range declaration requires three dots followed by a number.
                    
                      |
                    %d |    %s
                      |    %s
                        
                    \u001B[0;36mhint\u001B[0m: Add one more dot between the %s and then a number like %d.
                    
                        %s...%d
                    """.formatted(
                            lineNumber + 1,
                    code[lineNumber],
                    " ".repeat(startIndexOfDots) + "^^",
                    start,
                    Integer.parseInt(start) + 14,
                    start,
                    Integer.parseInt(start) + 14
            ));
        }
    }

    record PutTooManyDots(int lineNumber, int startIndexOfDots) implements Problem {}


    record OnlyOneDotAndNoRightHandSide(int lineNumber, int indexOfDot) implements Problem {
        @Override
        public void print(String[] code) {
            var split = code[lineNumber].split("\\.", 2);
            var start = split[0];
            System.out.println("""
                    \u001B[1;31merror\u001B[0m: The range declaration requires three dots followed by a number.
                                        
                      |
                    %d |    %s
                      |    %s
                        
                    \u001B[0;36mhint\u001B[0m: Add two more dots after the %s and then a number like %d.
                                        
                        %s...%d
                    """.formatted(
                            lineNumber + 1,
                    code[lineNumber],
                    " ".repeat(indexOfDot) + "^",
                    start,
                    Integer.parseInt(start) + 14,
                    start,
                    Integer.parseInt(start) + 14
            ));
        }
    }
    record OnlyTwoDotsAndNoRightHandSide(int lineNumber, int startIndexOfDots) implements Problem {
        @Override
        public void print(String[] code) {
            var split = code[lineNumber].split("\\.\\.", 2);
            var start = split[0];
            var end = split[1];
            System.out.println("""
                    \u001B[1;31merror\u001B[0m: The range declaration requires three dots followed by a number.
                    
                      |
                    %d |    %s
                      |    %s
                        
                    \u001B[0;36mhint\u001B[0m: Add one more dot between the %s and the %s like so.
                    
                        %s...%s
                    """.formatted(
                    code[lineNumber],
                    " ".repeat(startIndexOfDots) + "^^",
                    start,
                    end,
                    start,
                    end
            ));
        }
    }

    record TooManyDotsAndNoRightHandSide() implements Problem {}

    record StartOfRangeDeclarationIsNotNumeric() implements Problem {}

    record EndOfRangeDeclarationIsNotNumeric(int lineNumber, String declaration) implements Problem {}

    record DanglingEquals(int startLine, int nextLine) implements Problem {
        @Override
        public void print(String[] code) {
            var message = new StringBuilder();
            message.append("\u001B[1;31merror\u001B[0m: For each rule, I expect to see an equals sign followed by a number.");
            message.append("""
                    
                    
                      |
                    """);


            boolean rightNextTo = startLine == nextLine - 1;
            if (rightNextTo) {
                message.append("""
                        %d | %s
                        %d | %s
                          |
                        
                        """.formatted(

                        startLine, code[startLine],
                        nextLine, code[nextLine]
                ));
            }
            else {
                message.append("""
                        %d | %s
                          |
                        %d | %s
                          |
                        
                        """.formatted(
                        startLine, code[startLine],
                        nextLine, code[nextLine]
                ));
            }


            if (rightNextTo) {
                message.append("""
                    \u001B[0;36mhint\u001B[0m: Try combining this line with the next one.
                    
                        %s%s
                    """.formatted(code[startLine], code[nextLine].trim()));
            }
            else {
                message.append("""
                    \u001B[0;36mhint\u001B[0m: Try combining line %d with line %d.
                    
                        %s%s
                    """.formatted(startLine, nextLine, code[startLine], code[nextLine].trim()));
            }


            System.out.println(message);
        }
    }

    record NoEqualsInLine(int startLine) implements Problem {
        @Override
        public void print(String[] code) {
            System.out.println("""
                    \u001B[1;31merror\u001B[0m: For each rule, I expect to see an equals sign followed by a number.
                    
                      |
                    %d |   %s
                      |   %s
                      
                    \u001B[0;36mhint\u001B[0m: Try adding =5 to the end of the line.
                                        
                        %s=5
                    """.formatted(
                            startLine,
                            code[startLine],
                            " ".repeat(code[startLine].length()) + "^",
                            code[startLine]
            ));
        }
    }

    record RightHandSideOfRuleNotNumber() implements Problem {}

    record StartOfRangeCantBeGreaterThanEnd(int lineNumber, int start, int end) implements Problem {}

    default void print(String[] code) {
        System.out.println("Not yet implemented: " + this.getClass());
    }

    default void print(String code) {
        print(code.split("\n"));
    }
}

record Program(
        RangeDeclaration rangeDeclaration,
        List<Rule> rules
) implements ParseResult {
    static ParseResult parse(String program) {
        program = program.trim();

        record Line(int number, String contents) {}

        var lines = new ArrayList<Line>();
        var programSplit = program.split("\n");
        for (int i = 0; i < programSplit.length; i++) {
            var line = programSplit[i];
            if (!line.isBlank()) {
                lines.add(new Line(i, line));
            }
        }

        if (lines.size() == 0) {
            return new Problem.EmptyProgram();
        }

        var firstLine = lines.get(0);
        var range = firstLine.contents.split("\\.\\.\\.", 2);
        if (range.length == 1) {
            var indexOfTwoDots = range[0].indexOf("..");
            if (indexOfTwoDots > -1) {
                if (indexOfTwoDots == firstLine.contents.length() - 3) {
                    return new Problem.OnlyTwoDotsAndNoRightHandSide(firstLine.number, indexOfTwoDots);
                }
                else {
                    return new Problem.OnlyPutTwoDots(firstLine.number, indexOfTwoDots);
                }
            }

            var indexOfSingleDot = range[0].indexOf('.');
            if (indexOfSingleDot > -1) {
                if (indexOfTwoDots == firstLine.contents.length() - 3) {
                    return new Problem.OnlyOneDotAndNoRightHandSide(firstLine.number, indexOfSingleDot);
                }
                else {
                    return new Problem.OnlyPutOneDot(firstLine.number, indexOfSingleDot);
                }
            }
        }

        if (range[1].startsWith(".")) {
            return new Problem.PutTooManyDots(lines.get(0).number, lines.get(0).contents.indexOf("..."));
        }

        int start;
        try {
            start = Integer.parseInt(range[0], 10);
        } catch (NumberFormatException e) {
            return new Problem.StartOfRangeDeclarationIsNotNumeric();
        }


        int end;
        try {
            end = Integer.parseInt(range[1], 10);
        } catch (NumberFormatException e) {
            return new Problem.EndOfRangeDeclarationIsNotNumeric(firstLine.number, range[1]);
        }

        if (start > end) {
            return new Problem.StartOfRangeCantBeGreaterThanEnd(firstLine.number, start, end);
        }

        var rules = new ArrayList<Rule>();
        var linesAfterFirst = lines.stream().skip(1).toList();
        for (int i = 0; i < linesAfterFirst.size(); i++) {
            var line = linesAfterFirst.get(i);
            int lastEquals = line.contents.lastIndexOf('=');
            if (lastEquals < 0) {
                if (linesAfterFirst.size() - 1 > i) {
                    var nextLine = linesAfterFirst.get(i + 1);
                    if (nextLine.contents.trim().startsWith("=")) {
                        return new Problem.DanglingEquals(line.number, nextLine.number);
                    }
                }
                return new Problem.NoEqualsInLine(line.number);
            }
            var lhs = line.contents.substring(0, lastEquals);
            var rhs = line.contents.substring(lastEquals + 1);

            int val;
            try {
                val = Integer.parseInt(rhs, 10);
            } catch (NumberFormatException e) {
                return new Problem.RightHandSideOfRuleNotNumber();
            }


            rules.add(new Rule(val, lhs));
        }

        return new Program(
                new RangeDeclaration(start, end),
                rules
        );
    }

    void run(OutputStream outputStream) {
        var printStream = new PrintStream(outputStream);
        for (int i = this.rangeDeclaration.from(); i <= this.rangeDeclaration.to(); i++) {
            var output = new StringBuilder();
            boolean fizzed = false;

            for (var fizzer : this.rules) {
                if (i % fizzer.n() == 0) {
                    output.append(fizzer.phrase());
                    fizzed = true;
                }
            }
            if (!fizzed) {
                output.append(i);
            }
            printStream.println(output);
        }
    }

    void compile(String className) {
        var code = """
                public final class %s {
                    public static void main(String[] args) {
                        for (int i = %d; i <= %d; i++) {
                            var output = new StringBuilder();
                            boolean fizzed = false;
                            
                            %s;
                            
                            if (!fizzed) {
                                output.append(i);
                            }
                            
                            System.out.println(output);
                        }
                    }
                }
                """.formatted(
                        className,
                        this.rangeDeclaration.from(),
                        this.rangeDeclaration.to(),
                        this.rules.stream()
                                .map(rule -> """
                                                    if (i %% %d == 0) {
                                                        output.append("%s");
                                                        fizzed = true;
                                                    }
                                        """.formatted(rule.n(), rule.phrase()))
                                .collect(Collectors.joining("\n"))
                );

        try {
            var dir = Files.createTempDirectory("code");
            var file = Files.createFile(Path.of(dir.toString(), className + ".java"));
            Files.writeString(file, code);

            var javac = ToolProvider.findFirst("javac").orElseThrow();
            javac.run(
                    System.out,
                    System.err,
                    "-d",
                    ".",
                    file.toString()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println();
            System.exit(1);
        }
        else if (args.length > 1) {
            System.err.println("Too many arguments provided");
            System.exit(1);
        }
        else {
            var filename = args[0];
            try {
                Path filePath = Path.of(filename);
                var content = Files.readString(filePath);
                var parseResult = Program.parse(content);
                if (parseResult instanceof Program program) {
                    program.run(System.out);
                    //var className = filePath.getFileName().toString();
                    //className = className.split("\\.")[0];
                    //program.compile(className);
                }
                else if (parseResult instanceof Problem problem) {
                    problem.print(content);
                    System.exit(1);
                }
            } catch (FileNotFoundException e) {

            } catch (IOException e) {
                System.err.println("error reading file");
                System.exit(1);
            }
        }
    }
}