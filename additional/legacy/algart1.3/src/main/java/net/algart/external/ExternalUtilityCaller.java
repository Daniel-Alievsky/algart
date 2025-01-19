/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.external;

import net.algart.arrays.*;
import net.algart.contexts.Context;
import net.algart.contexts.DefaultArrayContext;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExternalUtilityCaller extends ExternalAlgorithmCaller {
    public static final String SYS_USE_SETTINGS_FILE = "useSettingsFile";   // default: true
    public static final String SYS_IMAGE_FORMAT = "imageFormat";            // default: "aa" (AlgART raw files)
    public static final String SYS_CLONE_RESULTS = "cloneResults";          // default: true

    public static final String SETTINGS_FILE_NAME = "settings.cfg";

    private final String externalCommand;
    private final List<String> startingArguments;
    private final String settings;
    private final List<String> inputArgNames;
    private final List<String> outputArgNames;

    private volatile boolean useSettingsFile = true;
    private volatile String imageFormat = "aa"; // means raw AlgART format
    private volatile boolean cloneResults = true;
    // If cloneResults=false and tiling=false, the results will not be copied from the temporary directory;
    // it can improve performance for very large images, but lead to failure while attempt to remove temporary data:
    // we recommend to set cleanup=true in this case.
    // If tiling=true, the results of processing each tile are little are copied into SimpleMemoryModel always.

    public ExternalUtilityCaller(
        Context context,
        String externalCommand,
        List<String> startingArguments,
        String settings,
        List<String> inputArgNames,
        List<String> outputArgNames,
        Object jsonSys)
        throws IOException
    {
        super(new DefaultArrayContext(context));
        if (externalCommand == null)
            throw new NullPointerException("OS command is not specified");
        if (inputArgNames == null)
            throw new NullPointerException("Input argument names are not specified");
        if (outputArgNames == null)
            throw new NullPointerException("Output argument names are not specified");
        this.externalCommand = externalCommand.trim();
        this.startingArguments = startingArguments == null ? null : new ArrayList<String>(startingArguments);
        this.settings = settings;
        this.inputArgNames = new ArrayList<String>(inputArgNames);
        this.outputArgNames = new ArrayList<String>(outputArgNames);
        if (this.startingArguments != null && this.startingArguments.contains(null))
            throw new NullPointerException("Some of starting arguments is null");
        if (this.inputArgNames.contains(null))
            throw new NullPointerException("Some of input argument names is null");
        if (this.outputArgNames.contains(null))
            throw new NullPointerException("Some of output argument names is null");
        if (jsonSys != null) {
            setParametersFromJSON(jsonSys);
        }
        setAlgorithmCode("eu");
    }

    public final List<String> getInputArgNames() {
        return Collections.unmodifiableList(inputArgNames);
    }

    public final List<String> getOutputArgNames() {
        return Collections.unmodifiableList(inputArgNames);
    }

    public final List<String> getStartingArguments() {
        return Collections.unmodifiableList(startingArguments != null ? startingArguments : new ArrayList<String>());
    }

    public final boolean isUseSettingsFile() {
        return useSettingsFile;
    }

    public final void setUseSettingsFile(boolean useSettingsFile) {
        this.useSettingsFile = useSettingsFile;
    }

    public final String getImageFormat() {
        return imageFormat;
    }

    public final void setImageFormat(String imageFormat) {
        if (imageFormat == null)
            throw new NullPointerException("Null image format");
        this.imageFormat = imageFormat;
    }

    public final boolean isCloneResults() {
        return cloneResults;
    }

    public final void setCloneResults(boolean cloneResults) {
        this.cloneResults = cloneResults;
    }

    @Override
    public void setParametersFromJSON(Object jsonObjectOrString) {
        super.setParametersFromJSON(jsonObjectOrString);
        try {
            Object json = stringToJSON(jsonObjectOrString);
            setUseSettingsFile((Boolean) jsonOptBoolean.invoke(json, SYS_USE_SETTINGS_FILE, useSettingsFile));
            setImageFormat((String) jsonOptString.invoke(json, SYS_IMAGE_FORMAT, imageFormat));
            setCloneResults((Boolean) jsonOptBoolean.invoke(json, SYS_CLONE_RESULTS, cloneResults));
        } catch (Exception e) {
            throw (AssertionError) new AssertionError("Unexpected error while using org.json.JSONObject").initCause(e);
        }
    }

    public final List<String> getMainArguments(ExternalProcessor processor) {
        List<String> result = new ArrayList<String>();
        if (useSettingsFile) {
            result.add(processor.getWorkFile(SETTINGS_FILE_NAME).getAbsolutePath());
        }
        for (String name : inputArgNames) {
            result.add(processor.getWorkFile(name + "." + imageFormat).getAbsolutePath());
        }
        for (String name : outputArgNames) {
            result.add(processor.getWorkFile(name + "." + imageFormat).getAbsolutePath());
        }
        return result;
    }

    // The full form of process method with "additionalData" argument is not useful here
    public final Map<String, List<Matrix<? extends PArray>>> process(
        Map<String, List<Matrix<? extends PArray>>> source)
        throws IOException
    {
        return process(source, null);
    }

    public void execute(ExternalProcessor processor) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add(externalCommand);
        command.addAll(getStartingArguments());
        command.addAll(getMainArguments(processor));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processor.execute(processBuilder);
    }

    @Override
    protected Map<String, List<Matrix<? extends PArray>>> processImpl(
        ExternalProcessor processor,
        Map<String, List<Matrix<? extends PArray>>> images,
        Object additionalData, // ignored by this processor
        boolean calledForTile)
        throws IOException
    {
        if (settings != null || useSettingsFile) {
            processor.writeWorkUTF8(SETTINGS_FILE_NAME, settings == null ? "" : settings);
        }
        for (String name : inputArgNames) {
            List<Matrix<? extends PArray>> image = images.get(name);
            if (image == null)
                throw new IllegalArgumentException("Illegal usage of " + this
                    + ": no input argument " + name);
            File f = processor.getWorkFile(name + "." + imageFormat);
            if (imageFormat.equals("aa")) {
                writeAlgARTImage(f, image, !calledForTile);
            } else {
                writeImage(f, image);
            }
        }
        execute(processor);
        MemoryModel mm = calledForTile ? SimpleMemoryModel.getInstance() : getContext().getMemoryModel();
        Map<String, List<Matrix<? extends PArray>>> result = newImageMap();
        for (String name : outputArgNames) {
            File f = processor.getWorkFile(name + "." + imageFormat);
            List<Matrix<? extends PArray>> image = imageFormat.equals("aa") ?
                readAlgARTImage(f) :
                readImage(f);
            result.put(name, calledForTile || cloneResults ?
                cloneImage(mm, image) :
                image);
        }
        return result;
    }
}
