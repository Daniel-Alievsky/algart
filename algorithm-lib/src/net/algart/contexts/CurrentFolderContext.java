package net.algart.contexts;

/**
 * <p>The context informing the module, working with {@link net.algart.arrays AlgART arrays},
 * about some <i>current folder</i> (usually a disk directory).</p>
 *
 * <p>You can use this context, if your application works with different subdirectories,
 * representing workplaces, projects or something like this.
 * The typical goal is to provide information about the preferred disk path, where your functions
 * should find necessary files.</p>
 *
 * <p>This package does not provide implementations of this context.
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface CurrentFolderContext extends Context {
    /**
     * Returns the path to the current folder.
     *
     * @return the current folder.
     */
    public String getCurrentFolder();
}
