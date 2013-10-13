package com.simagis.images;

import net.algart.arrays.ExternalProcessor;
import net.algart.contexts.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExternalJavaUtilityCaller extends ExternalUtilityCaller {
    private final String mainClassName;
    private final List<String> specificJVMOptions; // added to customJVMOptions
    private volatile File customJREHome;
    private volatile File javaExecutable;
    private volatile List<String> customJVMOptions;

    public ExternalJavaUtilityCaller(
        Context context,
        String mainClassName,
        List<String> startingArguments,
        List<String> specificJVMOptions,
        String settings,
        List<String> inputArgNames,
        List<String> outputArgNames,
        Object jsonSys)
        throws IOException
    {
        super(context,
            "java", // not used
            startingArguments,
            settings,
            inputArgNames,
            outputArgNames,
            jsonSys);
        if (mainClassName == null)
            throw new NullPointerException("Main class name is not specified");
        if (specificJVMOptions == null)
            throw new NullPointerException("JVM options are not specified. "
                + "At least, -classpath or -jar should be specified");
        if (inputArgNames == null)
            throw new NullPointerException("Input argument names are not specified");
        if (outputArgNames == null)
            throw new NullPointerException("Output argument names are not specified");
        this.mainClassName = mainClassName.trim();
        this.specificJVMOptions = new ArrayList<String>(specificJVMOptions);
        setCustomJRE(null); // initializes customJREHome, javaExecutable, customJVMOptions
        setAlgorithmCode("jeu");
    }

    public final File getCustomJREHome() {
        return customJREHome;
    }

    public final File getJavaExecutable() {
        return javaExecutable;
    }

    public final void setCustomJREHome(File customJREHome) throws FileNotFoundException {
        this.customJREHome = customJREHome;
        this.javaExecutable = ExternalProcessor.getJavaExecutable(customJREHome);
    }

    public final List<String> getCustomJVMOptions() {
        return new ArrayList<String>(customJVMOptions);
    }

    public final void setCustomJVMOptions(List<String> customJVMOptions) {
        this.customJVMOptions = customJVMOptions == null ? null : new ArrayList<String>(customJVMOptions);
    }

    public final void setCustomJRE(String jreName) throws FileNotFoundException {
        setCustomJREHome(ExternalProcessor.getCustomJREHome(jreName));
        setCustomJVMOptions(ExternalProcessor.getCustomJVMOptions(jreName));
    }

    @Override
    public void execute(ExternalProcessor processor) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable.getAbsolutePath());
        if (customJVMOptions != null) {
            command.addAll(customJVMOptions);
        }
        command.addAll(specificJVMOptions);
        command.add(mainClassName);
        command.addAll(getStartingArguments());
        command.addAll(getMainArguments(processor));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processor.execute(processBuilder);
    }
}
