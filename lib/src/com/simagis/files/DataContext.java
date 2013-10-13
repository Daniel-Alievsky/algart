package com.simagis.files;

import net.algart.contexts.Context;

import java.io.File;
import java.io.IOException;

/**
 * User: av@simagis.com
 * Date: 19.10.2009
 * Time: 19:12:57
 */
public interface DataContext extends Context {

    DataDir linkToExternalDir(File src);

    DataFile linkToExternalFile(File src);

    void createLinkOrCopy(File target, File source) throws IOException;
}
