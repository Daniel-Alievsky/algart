package net.algart.executable.preprocessor.java;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaDocCorrector {
    private static final Pattern PATTERN = Pattern.compile(
        "\\<\\!--\\s*algart(\\w+)\\s*--\\>"
        + "(.*?)"
        + "\\<\\!--\\s*\\/algart(\\w+)\\s*--\\>",
        Pattern.DOTALL);
    private static final String REPLACEMENT = "<!--(removed by " + JavaDocCorrector.class.getSimpleName() + ")-->";

    private boolean verbose = false;
    private int processedFilesCount = 0;

    public static void main(String[] args) throws IOException {
        JavaDocCorrector corrector = new JavaDocCorrector();
        int startArgIndex = 0;
        if (args.length > startArgIndex && args[startArgIndex].equals("-verbose")) {
            corrector.verbose = true;
            startArgIndex++;
        }
        if (args.length == startArgIndex) {
            System.out.println("Usage: " + JavaDocCorrector.class.getName() + " [-verbose] JavaDoc-directory");
            System.out.println("This utility will remove consequent fragment (besides the 1st");
            System.out.println("<!--algartXXXX-->...</!--algartXXXX-->");
            System.out.println("where XXXX is any character combination.");
            return;
        }
        File dir = new File(args[0]);
        if (!dir.isDirectory())
            throw new FileNotFoundException("\"" + dir + "\" is not an existing directory");
        corrector.processSubdirectory(dir);
        System.out.println(corrector.processedFilesCount + " files corrected in " + dir.getAbsolutePath());
    }

    private void processSubdirectory(File dir) throws IOException {
        final File[] files = dir.listFiles();
        assert files != null;
        for (File f : files) {
            if (f.isDirectory()) {
                processSubdirectory(f);
            } else if (f.isFile()) {
                final String name = f.getName();
                if (name.endsWith(".html") || name.endsWith(".htm")) {
                    processFile(f);
                }
            }
        }
    }

    private void processFile(File f) throws IOException {
        final String data = readUTF8(f);
        StringBuilder sb = new StringBuilder();
        final Set<String> alreadyAppeared = new HashSet<String>();
        final Matcher m = PATTERN.matcher(data);
        int p = 0;
        while (m.find()) {
            int q = m.start();
            sb.append(data.substring(p, q));
            p = m.end();
            final String blockName = m.group(1);
            if (!alreadyAppeared.contains(blockName)) {
                alreadyAppeared.add(blockName);
                sb.append(data.substring(q, p));
            } else {
                sb.append(REPLACEMENT);
            }
//            System.out.println(f + ": " + m.group(1) + ".." + m.group(3));
        }
        sb.append(data.substring(p));
        final String result = sb.toString();
        if (!result.equals(data)) {
            if (verbose) {
                System.out.println("Processing " + f);
            }
            writeUTF8(f, result);
            processedFilesCount++;
        } else {
            if (verbose) {
                System.out.println("Skipping " + f);
            }
        }
    }

    private static String readUTF8(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        StringBuilder sb = new StringBuilder();
        try {
            char[] buf = new char[65536];
            int len;
            while ((len = reader.read(buf)) >= 0) {
                sb.append(buf, 0, len);
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    private static void writeUTF8(File file, String text) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(file);
        Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
        try {
            writer.write(text);
        } catch (IOException e) {
            try {
                writer.close();
            } catch (IOException ex) {
                // preserve the original exception
            }
            throw e;
        }
        writer.close();
    }
}
