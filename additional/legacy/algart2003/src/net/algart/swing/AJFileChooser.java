/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.swing;

import java.io.*;
import java.util.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import net.algart.lib.*;
import net.algart.array.Arrays;

public class AJFileChooser extends JFileChooser {

    /** Identifies a change in the approvingNonExistingFilesEnabled property. */
    public static final String APPROVING_NON_EXISTING_FILES_ENABLED_CHANGED_PROPERTY= "ApprovingNonExistingFilesEnabledChangedProperty";
    /** Identifies a change in the overwriteWarningEnabled property. */
    public static final String OVERWRITE_WARNING_ENABLED_CHANGED_PROPERTY= "OverwriteWarningEnabledChangedProperty";
    /** Identifies a change in the approvingFilesOnlyEnabled property. */
    public static final String APPROVING_FILES_ONLY_ENABLED_CHANGED_PROPERTY= "ApprovingFilesOnlyEnabledChangedProperty";
    /** Identifies a change in the traversingLinksEnabled property. */
    public static final String TRAVERSING_LINKS_ENABLED_CHANGED_PROPERTY= "TraversingLinksEnabledChangedProperty";
    /** Identifies a change in the deleter property. */
    public static final String DELETER_CHANGED_PROPERTY= "DeleterChangedProperty";

    private static ResourceBundle bundle = ResourceBundle.getBundle(AJFileChooser.class.getName());
    private static FileSystemView fileSystemView= FileSystemView.getFileSystemView();
    private boolean approvingNonExistingFilesEnabled= true;
    private boolean overwriteWarningEnabled= true;
    private boolean approvingFilesOnlyEnabled= false;
    private boolean traversingLinksEnabled= false;
    private Deleter deleter= null;
    {
        addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                if (e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    correctDeleteStatus();
                    correctApproveButton();
                }
            }
        });
    }

    public AJFileChooser() {
        super();
    }
    public AJFileChooser(String currentDirectoryPath) {
        super(currentDirectoryPath);
    }
    public AJFileChooser(File currentDirectory) {
        super(currentDirectory);
    }
    public AJFileChooser(FileSystemView fsv) {
        super(fsv);
    }
    public AJFileChooser(File currentDirectory, FileSystemView fsv) {
        super(currentDirectory,fsv);
    }
    public AJFileChooser(String currentDirectoryPath, FileSystemView fsv) {
        super(currentDirectoryPath,fsv);
    }


    /**
     * Returns the default text of the <i>Approve</i> button.
     * It is used when <code>getApproveButtonText</code> returns null.
     *
     * Typically, this would be "Open" or "Save".
     *
     * @see #getApproveButtonText
     */
    public String getDefaultApproveButtonText() {
        switch (getDialogType()) {
            case OPEN_DIALOG: return UIManager.getString("FileChooser.openButtonText",getLocale());
            case SAVE_DIALOG: return UIManager.getString("FileChooser.saveButtonText",getLocale());
        }
        return null;
    }

    /**
     * Returns the default tooltop text of the <i>Cancel</i> button.
     * It is used when <code>getApproveButtonToolTipText</code> returns null.
     *
     * Typically, this would be "Cancel".
     *
     * @see #getApproveButtonToolTipText
     */
    public String getDefaultApproveButtonToolTipText() {
        switch (getDialogType()) {
            case OPEN_DIALOG: return UIManager.getString("FileChooser.openButtonToolTipText",getLocale());
            case SAVE_DIALOG: return UIManager.getString("FileChooser.saveButtonToolTipText",getLocale());
        }
        return null;
    }
    /**
     * Returns the default text of the <i>Cancel</i> button.
     *
     * Typically, this would be "Cancel".
     *
     * @see #getApproveButtonText
     */
    public String getDefaultCancelButtonText() {
        return UIManager.getString("FileChooser.cancelButtonText",getLocale());
    }

    /**
     * Returns the default tooltop text of the <i>Cancel</i> button.
     *
     * Typically, this would be "Cancel".
     *
     * @see #getApproveButtonToolTipText
     */
    public String getDefaultCancelButtonToolTipText() {
        return UIManager.getString("FileChooser.cancelButtonToolTipText",getLocale());
    }


    /**
     * Returns the value of the <code>approvingNonExistingFilesEnabled</code>
     * property.
     *
     * @return the value of the <code>approvingNonExistingFilesEnabled</code>
     *     property
     *
     * #setApprovingNonExistingFilesEnabled
     */
    public boolean isApprovingNonExistingFilesEnabled() {
        return approvingNonExistingFilesEnabled;
    }

    /**
     * Sets the property that indicates whether the <i>approve</i>
     * button originates the corresponding APPROVE_SELECTION
     * action and closes the dialog shown by showXxxDialog method in a case
     * when the selected file or subdirectory does not exist.
     * It can be useful in <i>Open</i> dialogs.
     * By default (and in the standard JFileChooser component),
     * JFileChooser.APPROVE_SELECTION is originated (and the dialog
     * is closed) always when the user presses the approve button.
     * <p>Default value for this property is true (that corresponds to
     * the standard behaviour).
     * <p>This property has no effect in the <i>Save</i> dialogs or
     * when multiple file selections are allowed.
     * <p>This method fires a property-changed event, using the string
     * value of <code>APPROVING_NON_EXISTING_FILES_ENABLED_CHANGED_PROPERTY</code>
     * as the name of the property.
     *
     * @param b <code>true</code> if non-existing files should be approved;
     *    <code>false</code> it you want to disable approving non-existing files
     *
     * @see #isApprovingNonExistingFilesEnabled
     * @see #APPROVING_NON_EXISTING_FILES_ENABLED_CHANGED_PROPERTY
     */
    public void setApprovingNonExistingFilesEnabled(boolean b) {
        boolean oldValue= approvingNonExistingFilesEnabled;
        approvingNonExistingFilesEnabled= b;
        firePropertyChange(APPROVING_NON_EXISTING_FILES_ENABLED_CHANGED_PROPERTY, oldValue, approvingNonExistingFilesEnabled);
    }

    /**
     * Returns the value of the <code>overwriteWarningEnabled</code>
     * property.
     *
     * @return the value of the <code>overwriteWarningEnabled</code>
     *     property
     *
     * #setOverwriteWarningEnabled
     */
    public boolean isOverwriteWarningEnabled() {
        return overwriteWarningEnabled;
    }

    /**
     * Sets the property that indicates whether <i>overwrite warning</i>
     * is shown when the user tries to approve a file or folder that
     * already exists. It can be useful in <i>Save</i> dialogs.
     * By default (and in the standard JFileChooser component),
     * no warning is shown.
     * <p>Default value for this property is false (that corresponds to
     * the standard behaviour).
     * <p>This property has no effect in the <i>Open</i> dialogs or
     * when multiple file selections are allowed.
     * <p>This method fires a property-changed event, using the string
     * value of <code>OVERWRITE_WARNING_ENABLED_CHANGED_PROPERTY</code>
     * as the name of the property.
     *
     * @param b <code>true</code> if <i>Approve</i> button should lead to
     *    showing overwrite warning when an existing file or directory
     *    is selected;
     *    <code>false</code> if existing files can be approved without
     *    warning.
     *
     * @see #isOverwriteWarningEnabled
     * @see #OVERWRITE_WARNING_ENABLED_CHANGED_PROPERTY
     */
    public void setOverwriteWarningEnabled(boolean b) {
        boolean oldValue= overwriteWarningEnabled;
        overwriteWarningEnabled= b;
        firePropertyChange(OVERWRITE_WARNING_ENABLED_CHANGED_PROPERTY, oldValue, overwriteWarningEnabled);
    }

    /**
     * Returns the value of the <code>approvingFilesOnlyEnabled</code>
     * property.
     *
     * @return the value of the <code>approvingFilesOnlyEnabled</code>
     *     property
     *
     * #setApprovingFilesOnlyEnabled
     */
    public boolean isApprovingFilesOnlyEnabled() {
        return approvingFilesOnlyEnabled;
    }

    /**
     * Sets the property that indicates whether some folders
     * (more precisely, traversable elements) can be approved -
     * regargless of the current file-selection mode.
     * By default (and in the standard JFileChooser component),
     * when the file-selection mode is DIRECTORIES_ONLY or
     * FILES_AND_DIRECTORIES, <i>Approve</i> button finishs
     * the dialog and fires the corresponding <i>ActionEvent</i>
     * even if the selected element is a folder. If this property
     * is set, <i>Approve</i> button never applies to traversable
     * elements (such as folder) - instead, it traverses them,
     * as when the file-selection mode is FILES_ONLY.
     * However, unlike the FILES_ONLY file-selection mode,
     * it is still possible to know up the current selected folder
     * during using AJFileChooser dialog - by <code>getSelectedFile()</code>
     * method.
     * <p>Default value for this property is false (that corresponds to
     * the standard behaviour).
     * <p>This method fires a property-changed event, using the string
     * value of <code>APPROVING_FILES_ONLY_ENABLED_CHANGED_PROPERTY</code>
     * as the name of the property.
     *
     * @param b <code>true</code> if traversable elements should
     *    be never approved
     *
     * @see #isApprovingFilesOnlyEnabled
     * @see #APPROVING_FILES_ONLY_ENABLED_CHANGED_PROPERTY
     */
    public void setApprovingFilesOnlyEnabled(boolean b) {
        boolean oldValue= approvingFilesOnlyEnabled;
        approvingFilesOnlyEnabled= b;
        firePropertyChange(APPROVING_FILES_ONLY_ENABLED_CHANGED_PROPERTY, oldValue, approvingFilesOnlyEnabled);
    }

    /**
     * Returns the value of the <code>traversingLinksEnabled</code>
     * property.
     *
     * @return the value of the <code>traversingLinksEnabled</code>
     *     property
     *
     * #setTraversingLinksEnabled
     */
    public boolean isTraversingLinksEnabled() {
        return traversingLinksEnabled;
    }

    /**
     * Sets the property that indicates whether <i>links</i>
     * can be traversable. By default (and in the standard
     * JFileChooser component), links (.lnk files in Windows)
     * are processed as usual files. If this property contains
     * true, then an attempt to go into link corresponding
     * a subdirectory leads to going into this directory,
     * and an attemt to choose a link corresponding to a file
     * leads to choosing that file.
     * <p>Default value for this property is false (that corresponds to
     * the standard behaviour).
     * <p>This method fires a property-changed event, using the string
     * value of <code>TRAVERSING_LINKS_ENABLED_CHANGED_PROPERTY</code>
     * as the name of the property.
     *
     * @param b <code>true</code> if links should be processed
     *    correctly;
     *    <code>false</code> if links should be processed as
     *    usual files.
     *
     * @see #isTraversingLinksEnabled
     * @see #TRAVERSING_LINKS_ENABLED_CHANGED_PROPERTY
     */
    public void setTraversingLinksEnabled(boolean b) {
        boolean oldValue= traversingLinksEnabled;
        traversingLinksEnabled= b;
        firePropertyChange(TRAVERSING_LINKS_ENABLED_CHANGED_PROPERTY, oldValue, traversingLinksEnabled);
    }

    /**
     * Returns the value of the <code>deleter</code> property.
     *
     * @see #setDeleter
     */
    public Deleter getDeleter() {
        return deleter;
    }

    /**
     * Installs <code>deleter</code> - an abstract class allowing
     * <code>canDeleteSelectedFiles</code> and <code>deleteSelectedFiles</code>
     * methods to work.
     * <p>To make delete operations availavle to the user, you also should
     * add some buttons that call <code>canDeleteSelectedFiles</code> and
     * <code>deleteSelectedFiles</code> methods - for example, by means
     * of <code>tryToAddButtons</code> method.
     * <p>Warning: if you want to delete not only files, but also subdirectories,
     * you should not use FILES_ONLY file-selection mode. If you need to provide
     * usual behaviour of <i>Approve</i> button for folders - opening the
     * selected folder - please use <code>approvingFilesOnlyEnabled</code>
     * property.
     * <p>This method fires a property-changed event, using the string
     * value of <code>DELETER_CHANGED_PROPERTY</code>
     * as the name of the property.
     *
     * @param deleter New instance of <code>AJFileChooser.Deleter</code> abstract class.
     *  Can be null - then the current deleter will be removed.
     *
     * @see #getDeleter
     * @see #isApprovingFilesOnlyEnabled
     * @see #setApprovingFilesOnlyEnabled
     * @see #tryToAddButtons
     * @see #DELETER_CHANGED_PROPERTY
     */
    public void setDeleter(Deleter deleter) {
        Deleter oldValue= this.deleter;
        this.deleter= deleter;
        if (deleter!=null)  deleter.canDeleteStatusChanged(canDeleteSelectedFiles());
        firePropertyChange(DELETER_CHANGED_PROPERTY, oldValue, deleter);
    }


    /**
     * Convenient extension of standard <code>setFileFilter</code> and
     * <code>addChoosableFileFilter</code> methods.
     * <p>The methods resets the choosable file filter list (by
     * <code>resetChoosableFileFilters()</code> method), and adds
     * all given <code>filters</code>. If the current selected filter
     * is present in the new filters, it stays current. Else,
     * if the current selected file matches one of new filters,
     * the first such filter becomes current. Else,
     * the first filter (<code>filters[0]</code>) becomes current.
     *
     * @see javax.swing.JFileChooser#addChoosableFileFilter
     * @see javax.swing.JFileChooser#resetChoosableFileFilters
     * @see javax.swing.JFileChooser#setFileFilter
     */
    public void setFileFilters(javax.swing.filechooser.FileFilter[] filters) {
        javax.swing.filechooser.FileFilter previousFilter= getFileFilter();
        File previousSelected= getSelectedFile();
        resetChoosableFileFilters();
        boolean previousFilterFound= false;
        for (int k=0; k<filters.length; k++) {
            addChoosableFileFilter(filters[k]);
            if (filters[k]==previousFilter) previousFilterFound= true;
        }
        if (previousFilterFound) {
            setFileFilter(previousFilter);
            return;
        }
        if (filters.length==0) return;
        if (previousSelected!=null) {
            for (int k=0; k<filters.length; k++) {
                if (filters[k].accept(previousSelected)) {
                    setFileFilter(filters[k]);
                    return;
                }
            }
        }
        setFileFilter(filters[0]);
    }


    /**
     * <code>SubdirectoryFilter</code> should be set by the same setFileFilter() method.
     * To take effect, selection mode should be
     *  JFileChooser.FILES_AND_DIRECTORIES
     * or
     *  JFileChooser.DIRECTORIES_ONLY
     */
    public static abstract class SubdirectoryFilter extends javax.swing.filechooser.FileFilter {
        /**
         * Whether the given subdirectory can be approved by Approve button.
         * Never called for files, only for subdirectories.
         */
        public abstract boolean acceptSubdirectory(File f);
    }

    /**
     * <code>ExtensionFileFilter</code> accepts files with the one of the given extensions
     * and also any traversable nodes - subdirectories, links, network places, etc.
     * Also, if the current file filter is <code>ExtensionFileFilter</code>, then
     * the <i>Save</i> dialog automatically appends the selected file name
     * by the first extension from <code>extensions</code> list (if the
     * file name has no extension, or if its extension does not used by
     * one of existing <code>ExtensionFileFilter</code> instances).
     *
     * This class in <i>immutable</i> (except possible changes
     * in the <code>fileChooser</code>).
     */
    public static class ExtensionFileFilter
        extends javax.swing.filechooser.FileFilter
        implements Arrays.MainValue
    {
        /**
         * <code>Settings</code> is an argument for <code>ExtensionFileFilter</code
         * constructor. Unlike <code>ExtensionFileFilter</code, this class
         * it fully <i>immutable</i> and doesn't require <code>JFileChooser</code>
         * instance.
         */
        public static final class Settings implements Arrays.MainValue {
            private final String[] extensions; // never null
            private final String description;
            /**
             * All extensions are automatically converted to lower case
             * by the call <code>...toUpperCase().toLowerCase()</code>
             *
             * @param extensions - for example, {"jpg"} or {"HTML","htm"}
             * @param description - for example, "JPeg images" or "HTML documents"
             */
            public Settings(String[] extensions, String description) {
                if (extensions==null || extensions.length==0)
                    throw new IllegalArgumentException("ExtensionFileFilter: extensions list cannot be empty");
                this.extensions= new String[extensions.length];
                for (int k=0; k<extensions.length; k++)
                    this.extensions[k]= extensions[k].toUpperCase().toLowerCase();
                this.description= description;
            }
            /**
             * Returns a copy of <code>extensions</code> converted to lower case.
             */
            public String[] getExtensions() {
                return (String[])this.extensions.clone();
            }
            /**
             * Returns a <code>description</code> passed to the constructor.
             */
            public String getDescription() {
                return description;
            }
            /**
             * Implementations of Arrays.MainValue interface.
             * Returns the result of <code>getExtensions()</code>.
             */
            public Object getMainValue() {
                return getExtensions();
            }
        } // class Settings

        private JFileChooser fileChooser;
        private Settings settings;
        public ExtensionFileFilter(JFileChooser fileChooser, Settings settings) {
            this.fileChooser= fileChooser;
            this.settings= settings;
        }
        /**
         * Returns a copy of <code>extensions</code> converted to lower case.
         */
        public Settings getSettings() {
            return settings;
        }
        public boolean accept(File f) {
            if (!fileSystemView.isFileSystem(f)
                || fileSystemView.isRoot(f)
                || fileSystemView.isFileSystemRoot(f)) return true;
            return fileChooser.isTraversable(f) || indexOfExtension(f,settings.extensions)!=-1;
        }
        public String getDescription() {
            return settings.description;
        }
        /**
         * Implementations of Arrays.MainValue interface.
         * Returns the result of <code>getSettings().getExtensions()</code>.
         */
        public Object getMainValue() {
            return settings.getExtensions();
        }
    } // class ExtensionFileFilter

    public static interface FileNameFilter {
        String filterFileName(String name);
    }

    /**
     * Checks extension of the file <code>f</code> and returns
     * its index in <code>extensions</code> array (-1 if not found).
     * All <code>extensions</code> should be in lower case
     * (converted by the call <code>...toUpperCase().toLowerCase()</code>).
     * Used in the <code>ExtensionFileFilter.accept</code> method.
     */
    public static int indexOfExtension(File f, String[] extensions) {
        String name= f.getName().toUpperCase().toLowerCase();
        for (int k=0; k<extensions.length; k++)
          if (extensions[k].startsWith("regex:")) {
            if (name.matches(extensions[k].substring("regex:".length()))) return k;
          } else
            if (name.endsWith("."+extensions[k])) return k;
        return -1;
    }

    /**
     * <code>Deleter</code> abstract class allows <code>AJFileChooser</code>
     * to perform delete operations.
     * @see #setDeleter
     */
    public static abstract class Deleter {
        /**
         * This method is called by the default implementation of
         * the following <code>canDelete(File[] files)</code> method.
         * Usually it is enough to override this method instead
         * the full <code>canDelete(File[] files)</code> version.
         */
        public abstract boolean canDelete(File f);
        /**
         * <code>canDelete</code> returns false if some of the given
         * files/subdirectories cannot be deleted.
         * In this case, <code>AJFileChooser</code> will
         * not try to delete files. If <code>canDelete</code> returns true,
         * it doesn't mean that deletion is surely possible - some errors
         * can still occur while deleting; but in this case
         * <code>AJFileChooser</code> will try to delete selected
         * files/subdirectories and will show an error message in
         * a case of any errors.
         * <p>By default, this method calls the previous
         * <code>canDelete(File f)</code> method for every file/subdirectory
         * and returns true if and only if <code>files.length>0</code>
         * and all such calls return true.
         */
        public boolean canDelete(File[] files) {
            if (files.length==0) return false;
            for (int k=0; k<files.length; k++) {
                if (!canDelete(files[k])) return false;
            }
            return true;
        }
        /**
         * <code>delete</code> tries to delete the given files/subdirectories
         * and returns false in a case of any errors. <code>AJFileChooser</code>
         * calls this method to delete files/subdirectories.
         */
        public abstract boolean delete(File[] files);

        /**
         * <code>canDeleteStatusChanged</code> is called every time when
         * the result of <code>AJFileChooser.canDeleteSelectedFiles()</code>
         * method is changed. You can override this method to control
         * enable/disable status of your <i>delete</i> button (not added
         * by <code>addDeleteButton()</code> method).
         *
         * @param b - new value of <code>AJFileChooser.canDeleteSelectedFiles()</code>
         * result
         */
        public abstract void canDeleteStatusChanged(boolean b);
    }

    /**
     * <code>DefaultDeleter</code> is the default implementation of
     * </code>Deleter</code> abstract class.
     * Be careful: this abstract class doesn't provide any way to delete
     * non-empty subdirectory containing files that are not used
     * by any choosable file filter. To delete such subdirectories,
     * <code>canDelete</code> method should be overrided.
     */
    public static class DefaultDeleter extends Deleter {
        protected AJFileChooser fileChooser;
        /**
         * @param fileChooser - an instance of AJFileChooser used together
         * with this <i>deleter</i>. Is is used as <code>parentComponent</code>
         * argument for JOptionPane methods.
         */
        public DefaultDeleter(AJFileChooser fileChooser) {
            this.fileChooser= fileChooser;
        }
        /**
         * Returns true if and only if <code>f</code>
         * is a usual file or an <i>empty</i> directory.
         */
        public boolean canDelete(File f) {
            if (!fileSystemView.isFileSystem(f)
                || fileSystemView.isRoot(f)
                || fileSystemView.isFileSystemRoot(f)) return false;
            if (f.isFile()) return true;
            if (!f.isDirectory()) return false;
            String[] list= f.list();
            return list==null || list.length==0;
        }
        /**
         * Tries to delete files/subdirectories by the recursive delete function.
         * Shows an error message in a case of any errors. Never called
         * when the correspondin <code>canDelete</code> method returns false.
         * <p>Before deletion, this method shows confirmation message.
         * If the user answers "No", this method does nothing.
         *
         * @return True if all files were deleted successfully.
         */
        public boolean delete(File[] files) {
            if (!showConfirmationMessage(fileChooser,files)) return false;
            java.util.List errorFiles= new java.util.ArrayList();
            for (int k=0; k<files.length; k++) {
                try {
                    new FileVisitor.Remover(files[k]).execute();
                } catch (IOException e) {
                    errorFiles.add(files[k]);
                }
            }
            if (!errorFiles.isEmpty()) {
                showErrorMessage(fileChooser,(File[])errorFiles.toArray(new File[0]));
                return false;
            }
            return true;
        }
        /**
         * Used by <code>delete</code> method to show confirmation message.
         * The simplest way to disable confirmation is to override this
         * method by an empty one.
         */
        protected boolean showConfirmationMessage(Component parent, File[] files) {
            return JOptionPane.showConfirmDialog(parent,
                MessageFormat.format(
                    bundle.getString("FileChooser.deletionConfirmationText"),
                    new Object[] {"&nbsp;&nbsp;&nbsp;&nbsp;"+Out.join(files,"<br>&nbsp;&nbsp;&nbsp;&nbsp;")}
                ),bundle.getString("FileChooser.deletionConfirmationTitle"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE)== JOptionPane.YES_OPTION;
        }
        /**
         * Used by <code>delete</code> method to show error message.
         */
        protected void showErrorMessage(Component parent, File[] errorFiles) {
            JOptionPane.showMessageDialog(parent,
                MessageFormat.format(
                    bundle.getString("FileChooser.deletionErrorText"),
                    new Object[] {"&nbsp;&nbsp;&nbsp;&nbsp;"+Out.join(errorFiles,"<br>&nbsp;&nbsp;&nbsp;&nbsp;")}
                ),bundle.getString("FileChooser.deletionErrorTitle"),
                JOptionPane.ERROR_MESSAGE);
        }
        /**
         * Does nothing in this implementation.
         */
        public void canDeleteStatusChanged(boolean b) {
        }
    }


    /**
     * Returns true if the current <code>deleter</code> interface says
     * (by its <code>canDelete</code> method) that all selected files
     * can be deleted.
     * Always returns false it the deleter isn't installed.
     *
     * @see #setDeleter
     */
    public boolean canDeleteSelectedFiles() {
        if (deleter==null) return false;
        return deleter.canDelete(getSelectedFilesRegardlessMode());
    }
    /**
     * Tries to delete all selected files using the method <code>delete</code>
     * of the current <code>deleter</code> interface. Does nothing if the deleter
     * isn't installed. Does nothing if <code>canDeleteSelectedFiles()</code> returns
     * false.
     * @return True if all files were deleted successfully.
     */
    public boolean deleteSelectedFiles() {
        if (deleter==null) return false;
        if (!canDeleteSelectedFiles()) return false;
        try {
            correctDeleteStatusLocked= true;
            if (!deleter.delete(getSelectedFilesRegardlessMode())) return false;
            rescanCurrentDirectory();
        } finally {
            correctDeleteStatusLocked= false;
        }
        correctDeleteStatus();
        return true;
    }

    protected java.util.List deleteButtons= new java.util.ArrayList();

    /**
     * Adds the given button to internal list <code>deleteButtons</code>
     * and sets the button's action listener that calls
     * <code>deleteSelectedFiles()</code> method.
     * AJFileChooser automatically controls the enable/disable status
     * of the added button.
     */
    public void addDeleteButton(AbstractButton button) {
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteSelectedFiles();
            }
        });
        button.setEnabled(canDeleteSelectedFiles());
        deleteButtons.add(button);
    }

    /**
     * Overrided to process special (described above) features of this class.
     * <p>Also, this implementation requires some file to be selected
     * to perform <i>Approve</i> action. (By default, <code>null</code>
     * also can be approved.)
     */
    public void approveSelection() {
        javax.swing.filechooser.FileFilter fileFilter= getFileFilter();
        File f= getSelectedFile();
        if (f==null) return; // nothing selected
        if (traversingLinksEnabled && isLinkUndocumented(f)) {
            try {
                setSelectedFile(getLinkLocationUndocumented(f));
                f= getSelectedFile();
                if (f==null) return;
            } catch (FileNotFoundException e) {
            }
        }
        if (fileFilter instanceof SubdirectoryFilter) {
            if (f.isDirectory()) {
                if (!((SubdirectoryFilter)fileFilter).acceptSubdirectory(f)) {
                    if (isTraversable(f)) setCurrentDirectory(f);
                    return; // skipping default approving
                }
            }
        }
        if (approvingFilesOnlyEnabled && isTraversable(f)) {
            setCurrentDirectory(f);
            return;
        }
        if (fileFilter instanceof ExtensionFileFilter) {
            if (getDialogType()==SAVE_DIALOG) {
                Arrays.MainValue[] all= (Arrays.MainValue[])Arrays.choose(
                    getChoosableFileFilters(),
                    new Arrays.ClassMatcher(ExtensionFileFilter.class),
                    new Arrays.MainValue[0]);
                if (indexOfExtension(f,(String[])Arrays.join2d(
                    Arrays.getMainValues(all,new String[0][])))==-1)
                {
                    setSelectedFile(new File(f.getPath()+"."
                        +((ExtensionFileFilter)fileFilter).settings.extensions[0]));
                    f= getSelectedFile();
                }
            }
        }
        if (fileFilter instanceof FileNameFilter) {
            final FileNameFilter nameFilter = (FileNameFilter) fileFilter;
            final String newName = nameFilter.filterFileName(f.getName());
            setSelectedFile(new File(f.getParentFile(), newName));
            f = getSelectedFile();
        }
        if (!isMultiSelectionEnabled()) {
            if (getDialogType()!=SAVE_DIALOG && !approvingNonExistingFilesEnabled) {
                if (!f.exists()) return;
            }
            if (getDialogType()!=OPEN_DIALOG && overwriteWarningEnabled) {
                if (f.exists()) {
                    if (JOptionPane.showConfirmDialog(this,
                        MessageFormat.format(
                            bundle.getString("FileChooser.overwriteWarningText"),
                            new Object[] {f}
                        ),bundle.getString("FileChooser.overwriteWarningTitle"),
                        JOptionPane.YES_NO_OPTION)!= JOptionPane.YES_OPTION)
                        return;
                }
            }
        }
        super.approveSelection();
    }

    /**
     * Overrided to process <code>traversingLinksEnabled</code> property.
     */
    public boolean isTraversable(File f) {
        if (super.isTraversable(f)) return true;  // optimization for "My Network places"
        if (traversingLinksEnabled && f!=null && isLinkUndocumented(f)) {
            if (f.getPath().endsWith(".lnk")) return false;
                // - avoiding system dialog in a case of invalid link under Windows
            try {
                return super.isTraversable(getLinkLocationUndocumented(f));
            } catch (FileNotFoundException e) {
            }

        }
        return false;
    }

    /**
     * Overrided to process <code>traversingLinksEnabled</code> property.
     */
    public void setCurrentDirectory(File f) {
        if (traversingLinksEnabled && f!=null && isLinkUndocumented(f)) {
            try {
                super.setCurrentDirectory(getLinkLocationUndocumented(f));
            } catch (FileNotFoundException e) {
            }
        }
        super.setCurrentDirectory(f);
    }
    /**
     * Overrided to provide working of <code>tryToMakeApproveButtonDefault()</code> method.
     */
    protected JDialog createDialog(Component parent) {
        JDialog result= super.createDialog(parent);
        result.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                tryToMakeApproveButtonDefault_perform();
            }
        });
        return result;
    }

    /**
     * Tries to avoid extra slowing of choosing files in large directories
     */
    public void tryToAvoidSlowingProblemInLargeDirectories() {
        optimizationEnabled= true;
    }

    private boolean optimizationEnabled= false;
    private boolean optimizeDirectoryWorking= false;
    public void setSelectedFile(File file) {
        optimizeDirectoryWorking= true;
        try {
            super.setSelectedFile(file);
        } finally {
            optimizeDirectoryWorking= false;
        }
    }
    public File getCurrentDirectory() {
        File dir= super.getCurrentDirectory();
        if (traversingLinksEnabled && dir!=null && isLinkUndocumented(dir)) {
            try {
                return getLinkLocationUndocumented(dir);
            } catch (FileNotFoundException e) {
            }
        }
        if (!optimizationEnabled || dir==null) return dir;
        if (optimizeDirectoryWorking
            && dir.getClass()!=File.class
            && fileSystemView.isFileSystem(dir)
            && !fileSystemView.isRoot(dir)
            && !fileSystemView.isFileSystemRoot(dir)) {
            return new File(dir.getPath());
        }
        return dir;
    }

    /**
     * Should return true if successful.
     */
    public boolean tryToRemoveSingleClickListeners() {
        Component[] all= ASwingUtilities.getComponentsRecursive(this);
        int count= 0;
        for (int k=0; k<all.length; k++) {
            if (all[k] instanceof JList || all[k] instanceof JTable) {
                MouseListener[] listeners= all[k].getMouseListeners();
                for (int j=0; j<listeners.length; j++) {
                    MouseListener l= listeners[j];
                    if (l.getClass().getName().endsWith("SingleClickListener")) {
                        all[k].removeMouseListener(l);
                        count++;
                    }
                }
            }
        }
        return count>0;
    }

    public static final class WhereToAddButtons {
        private final String name;
        private WhereToAddButtons(String name) {this.name= name;}
        public static final WhereToAddButtons LEFT= new WhereToAddButtons("LEFT");
        public static final WhereToAddButtons MIDDLE= new WhereToAddButtons("MIDDLE");
        public static final WhereToAddButtons RIGHT= new WhereToAddButtons("RIGHT");
        public String toString() {return name;}
    }
    /**
     * Should return true if successful.
     */
    public boolean tryToAddButtons(AbstractButton[] buttons,
        WhereToAddButtons where,
        boolean addSpaceAround,
        boolean addSpaceBetween)
    {
        AbstractButton newFolderButton= tryToFindNewFolderButton();
        if (newFolderButton==null) return false;
        Container parent= newFolderButton.getParent();
        Component[] brothers= parent.getComponents();
        Dimension[] fdims= null;
        int leftButtonIndex= 0;
        for (int k=0; k<brothers.length; k++) {
            if (brothers[k] instanceof AbstractButton) {
                leftButtonIndex= k; break;
            }
        }
        for (int k=leftButtonIndex; k<brothers.length; k++) {
            if (brothers[k] instanceof Box.Filler) {
                fdims= new Dimension[] {
                    brothers[k].getMinimumSize(),
                    brothers[k].getPreferredSize(),
                    brothers[k].getMaximumSize()
                };
                break;
            }
        }
        if (fdims==null && (addSpaceAround || addSpaceBetween)) return false;

        int insertIndex= leftButtonIndex;
        if (where==WhereToAddButtons.MIDDLE) {
            for (int k=0; k<brothers.length; k++) {
                if (brothers[k] instanceof JToggleButton) {
                    insertIndex= k; break;
                }
            }
        } else if (where==WhereToAddButtons.RIGHT) {
            insertIndex= brothers.length;
        }
        if (addSpaceAround && fdims!=null && where!=WhereToAddButtons.RIGHT)
            parent.add(new Box.Filler(fdims[0],fdims[1],fdims[2]),insertIndex);
        for (int k=0; k<buttons.length; k++) {
            if (k>0 && fdims!=null && addSpaceBetween)
                parent.add(new Box.Filler(fdims[0],fdims[1],fdims[2]),insertIndex);
            AbstractButton button= buttons[buttons.length-1-k];
            parent.add(button,insertIndex);
        }
        if (addSpaceAround && fdims!=null && where==WhereToAddButtons.RIGHT)
            parent.add(new Box.Filler(fdims[0],fdims[1],fdims[2]),insertIndex);
        return true;
    }

    /**
     * Should return not null if successful.
     */
    public AbstractButton tryToFindNewFolderButton() {
        AbstractButton[] buttons= tryToFindButtons("NewFolder");
        if (buttons.length>0) return buttons[0];
        return null;
    }
    /**
     * Should return true if successful.
     */
    public boolean tryToRemoveNewFolderButton() {
        AbstractButton[] buttons= tryToFindButtons("NewFolder");
        for (int k=0; k<buttons.length; k++) {
            buttons[k].setVisible(false);
            buttons[k].setEnabled(false);
        }
        return buttons.length>0;
    }

    /**
     * Should return not null if successful.
     * @param newHomeDirectory - if not null, <i>Home</i> button will change
     *    the current directory to this one
     * @return Found <i>Home</i> button. You can correct it's tooltip, icon, etc.
     */
    public AbstractButton tryToFindAndCorrectHomeButton(
        File newHomeDirectory)
    {
        AbstractButton[] buttons= tryToFindButtons("GoHome");
        if (buttons.length==0) return null;
        if (newHomeDirectory!=null) {
            ActionListener[] listeners= buttons[0].getActionListeners();
            for (int j=0; j<listeners.length; j++) {
                String className= listeners[j].getClass().getName();
                if (className.indexOf("GoHome")!=-1) {
                    if (className.equals(SpecialGoHomeActionListener.class.getName()))
                        return buttons[0];
                    buttons[0].removeActionListener(listeners[j]);
                }
            }
            buttons[0].addActionListener(new SpecialGoHomeActionListener(this,newHomeDirectory));
        }
        return buttons[0];
    }
    protected static class SpecialGoHomeActionListener implements ActionListener {
        private final JFileChooser fileChooser;
        private final File newHomeDirectory;
        public SpecialGoHomeActionListener(JFileChooser fileChooser, File newHomeDirectory) {
            this.fileChooser= fileChooser;
            this.newHomeDirectory= newHomeDirectory;
        }
        public void actionPerformed(ActionEvent e) {
            fileChooser.setCurrentDirectory(newHomeDirectory);
        }
    }
    /**
     * Should return true if successful.
     * If AJFileChooser is inserted into some external
     * container (instead of usual call of <code>showXxxDialog()</code>
     * methods), this method should be called after adding
     * AJFileChooser into the container, but not just
     * after creating AJFileChooser instance.
     */
    public boolean tryToMakeApproveButtonDefault() {
        approveButtonShouldBeDefault= true;
        return tryToMakeApproveButtonDefault_perform();
    }
    protected boolean approveButtonShouldBeDefault= false;
    private boolean tryToMakeApproveButtonDefault_perform() {
        if (!approveButtonShouldBeDefault) return false;
        AbstractButton approveButton= findApproveButton();
        if (!(approveButton instanceof JButton)) return false;
        JRootPane rootPane= approveButton.getRootPane();
        if (rootPane==null) return true;
        rootPane.setDefaultButton((JButton)approveButton);
        return true;
    }

    /**
     * Should return not null if successful.
     * @return Found JList containing the list of files (in <i>list</i> mode)
     */
    public JList tryToFindFilesList() {
        Component[] all= ASwingUtilities.getComponentsRecursive(this);
        for (int k=0; k<all.length; k++) {
            if (all[k] instanceof JList) {
                MouseListener[] listeners= all[k].getMouseListeners();
                for (int j=0; j<listeners.length; j++) {
                    MouseListener l= listeners[j];
                    if (l.getClass().getName().endsWith("DoubleClickListener")) {
                        return (JList)all[k];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Should return not null if successful.
     * @return Found JList containing the list of files (in <i>list</i> mode)
     */
    public JList tryToFindFilesListAndCorrectKeyboardBug() {
        final JList result= tryToFindFilesList();
        if (result!=null) {
            KeyListener[] listeners= result.getKeyListeners();
            if (Arrays.contains(listeners,new Arrays.Matcher() {
                public boolean matches(Object o) {
                    return o.getClass().getName().equals(SpecialFilesListKeyListener.class.getName());
                }
            })) {
                return result;
            }
            ASwingUtilities.addKeyListenerRecursive(result,new SpecialFilesListKeyListener(result));
        }
        return result;
    }

    protected static class SpecialFilesListKeyListener extends KeyAdapter {
        private final JList filesList;
        public SpecialFilesListKeyListener(JList filesList) {
            this.filesList= filesList;
        }
        public void keyPressed(KeyEvent e) {
            if (filesList.isSelectionEmpty() && filesList.getLastVisibleIndex()>=0) {
                filesList.setSelectedIndex(0);
            }
        }
    }

/*
    // Some bad idea :-/
    public File getSelectedFileOrDirectory() {
        File f= getSelectedFile();
        if (f==null && getFileSelectionMode()==FILES_ONLY) {
            javax.swing.plaf.FileChooserUI uiAbstract= getUI();
            if (uiAbstract instanceof javax.swing.plaf.basic.BasicFileChooserUI) {
                javax.swing.plaf.basic.BasicFileChooserUI ui= (javax.swing.plaf.basic.BasicFileChooserUI)uiAbstract;
                foundSelectedDirectory= null;
                try {
                    setCurrentDirectoryLockedToDetectSelectedDirectory= true;
                    ui.getApproveSelectionAction().actionPerformed(null);
                } catch(Exception e) {
                } finally {
                    setCurrentDirectoryLockedToDetectSelectedDirectory= false;
                }
                return foundSelectedDirectory;
            }
        }
        return f;
    }
*/
    private File[] getSelectedFilesRegardlessMode() {
        File[] all= getSelectedFiles();
        File f= getSelectedFile();
        if (all==null || all.length==0) return f==null? new File[0]: new File[] {f};
        if (f==null || Arrays.indexOfEqual(all,f)!=-1) return all;
        return (File[])Arrays.append(all,new File[] {f});
    }

    private boolean correctDeleteStatusLocked= false;
    private void correctDeleteStatus() {
        if (correctDeleteStatusLocked) return;
        boolean canDelete= false;
        if (deleter!=null || !deleteButtons.isEmpty()) canDelete= canDeleteSelectedFiles();
        if (deleter!=null) deleter.canDeleteStatusChanged(canDelete);
        for (int k=0,n=deleteButtons.size(); k<n; k++)
            ((AbstractButton)deleteButtons.get(k)).setEnabled(canDelete);
    }
    private void correctApproveButton() {
        if (getFileSelectionMode()==FILES_ONLY) return;
        javax.swing.filechooser.FileFilter fileFilter= getFileFilter();
        File f= getSelectedFile();
        if (fileFilter instanceof SubdirectoryFilter) {
            correctApproveButton(f!=null && f.isDirectory()
                && !((SubdirectoryFilter)fileFilter).acceptSubdirectory(f)
                && isTraversable(f));
            return;
        }
        if (approvingFilesOnlyEnabled) {
            correctApproveButton(f!=null && isTraversable(f));
            return;
        }
    }
    private void correctApproveButton(boolean traversable) {
        javax.swing.plaf.FileChooserUI uiAbstract= getUI();
        if (!(uiAbstract instanceof javax.swing.plaf.basic.BasicFileChooserUI)) return;
        javax.swing.plaf.basic.BasicFileChooserUI ui= (javax.swing.plaf.basic.BasicFileChooserUI)uiAbstract;
        AbstractButton approveButton= findApproveButton();
        if (approveButton==null) return;
        if (traversable) {
            approveButton.setText(directoryOpenButtonText);
            approveButton.setToolTipText(directoryOpenButtonToolTipText);
        } else {
            approveButton.setText(ui.getApproveButtonText(this));
            approveButton.setToolTipText(ui.getApproveButtonToolTipText(this));
        }
    }
    private AbstractButton[] approveButtons;
    private String directoryOpenButtonText;
    private String directoryOpenButtonToolTipText;
    private synchronized AbstractButton findApproveButton() {
        if (approveButtons==null) {
            approveButtons= tryToFindButtons("ApproveSelection");
                directoryOpenButtonText = UIManager.getString("FileChooser.directoryOpenButtonText",getLocale());
                directoryOpenButtonToolTipText = UIManager.getString("FileChooser.directoryOpenButtonToolTipText",getLocale());
        }
        return approveButtons.length==0? null: approveButtons[0];
    }

    private AbstractButton[] tryToFindButtons(final String listenerNameFragment) {
        Component[] all= ASwingUtilities.getComponentsRecursive(this);
        java.util.List result= new java.util.ArrayList();
        for (int k=0; k<all.length; k++) {
            if (all[k] instanceof AbstractButton) {
                ActionListener[] listeners= ((AbstractButton)all[k]).getActionListeners();
                if (Arrays.contains(listeners,new Arrays.Matcher() {
                    public boolean matches(Object o) {
                        return o.getClass().getName().indexOf(listenerNameFragment)!=-1;
                    }
                })) {
                    result.add(all[k]);
                }
            }
        }
        return (AbstractButton[])result.toArray(new AbstractButton[0]);
    }

    public static boolean isLinkUndocumented(File f) {
        try {
            return sun.awt.shell.ShellFolder.getShellFolder(f).isLink();
        } catch (FileNotFoundException e) {
            return false;
        }
    }
    public static File getLinkLocationUndocumented(File f) throws FileNotFoundException {
        File result= sun.awt.shell.ShellFolder.getShellFolder(f).getLinkLocation();
        if (result==null || result.getPath().trim().length()==0) throw new FileNotFoundException("Incorrect link - it is empty");
        return result;
    }

}