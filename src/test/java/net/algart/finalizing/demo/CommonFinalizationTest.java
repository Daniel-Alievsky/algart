/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.finalizing.demo;

import java.lang.ref.*;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * <p>Common test for finalization via reference queues an <tt>finalize()</tt> method.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class CommonFinalizationTest {

    private static Set<FinalizeHolder> taskSet = new HashSet<FinalizeHolder>();
    private static ReferenceQueue<Object> refQueue = null;
    private static Thread thread = null;

    public static void invokeOnDeallocation(Object checkedForDeallocation, Runnable task) {
        if (thread == null) {
            refQueue = new ReferenceQueue<Object>();
            thread = new CleanupThread();
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
        new FinalizeHolder(checkedForDeallocation, task);
    }

    private static class FinalizeHolder extends PhantomReference<Object> {
        final Runnable task;
        FinalizeHolder(Object checkedForDeallocation, Runnable task) {
            super(checkedForDeallocation, refQueue);
            this.task = task;
            taskSet.add(this);
            // avoid deallocation of this reference before the cleanup procedure
        }
    }

    private static class CleanupThread extends Thread {
        public void run() {
            while (true) {
                FinalizeHolder phantomHolder = null;
                try {
                    phantomHolder = (FinalizeHolder)(Object)refQueue.remove();
                } catch (InterruptedException ex) {
                }
                if (phantomHolder != null) {
                    phantomHolder.task.run();
                    taskSet.remove(phantomHolder);
                }
            }
        }
    }

    static volatile int timeStamp = 0;
    public static class MyClassWithFinalize {
        private String name;
        private ByteBuffer bb;
        public MyClassWithFinalize(String name, ByteBuffer bb) {
            this.name = name;
            this.bb = bb;
        }

        protected void finalize() throws Throwable {
            try {
                long t = System.currentTimeMillis();
                while (System.currentTimeMillis() - t < 750) ; //emulation of long calculations
                System.out.println(" -- \"" + name + "\" was finalized at time " + (++timeStamp) + "!");
            } finally {
                super.finalize();
            }
        }
    }

    public static class MyClass {
        private ByteBuffer bb;
        private MyClassWithFinalize ref = new MyClassWithFinalize("From MyClass", null);
        public MyClass(ByteBuffer bb) {
            this.bb = bb;
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length == 0) {
            System.out.println("Usage: " + CommonFinalizationTest.class.getName()
                + " usual|objectWithFinalize|bb|bbInUsual|bbInObjectWithFinalize [gc]");
            return;
        }
        Object[] checked;
        RandomAccessFile raf = null;
        File file = null;
        if (args[0].equalsIgnoreCase("usual")) {
            checked = new Object[] {new MyClass(null)};
        } else if (args[0].equalsIgnoreCase("objectWithFinalize")) {
            checked = new Object[] {new MyClassWithFinalize("From main()", null)};
        } else if (args[0].equalsIgnoreCase("bb")
            || args[0].equalsIgnoreCase("bbInUsual")
            || args[0].equalsIgnoreCase("bbInObjectWithFinalize"))
        {
            file = File.createTempFile("phafNonMapped",".tmp");
            file.deleteOnExit();
            file = File.createTempFile("phaf",".tmp");
//            file.deleteOnExit();
            raf = new RandomAccessFile(file, "rw");
            raf.setLength(1000);
            MappedByteBuffer bb = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, 1000);
            if (args[0].equalsIgnoreCase("bb"))
                checked = new Object[] {bb};
            else if (args[0].equalsIgnoreCase("bbInUsual"))
                checked = new Object[] {new MyClass(bb), bb};
            else
                checked = new Object[] {new MyClassWithFinalize("From main with bb()", bb), bb};
        } else {
            System.err.println("Illegal mode " + args[0]);
            return;
        }
        for (int k = 0; k < checked.length; k++) {
            final int hash = checked[k].hashCode();
            System.out.println("Allocated " + hash + ": " + checked[k].getClass());
            final File fileToDelete = file;
            invokeOnDeallocation(checked[k], new Runnable() {
                public void run() {
                    int stamp = ++timeStamp;
                    System.out.println(" ~~ Deallocated " + hash + " at time " + stamp);
                    long t = System.currentTimeMillis();
                    while (System.currentTimeMillis() - t < 750) ; //emulation of long calculations
                    if (fileToDelete != null && fileToDelete.exists())
                        if (fileToDelete.delete())
                            System.out.println(" ~~ " + fileToDelete + " deleted at time " + stamp);
                        else
                            System.out.println(" ~~ CANNOT delete " + fileToDelete + " at time " + stamp);
                }
            });
        }
        if (raf != null)
            raf.close();
        checked = null;
        if (args.length > 1 && args[1].equalsIgnoreCase("gc")) {
            for (int k = 0; k < 5; k++) {
                System.out.println(k + ": calling System.runFinalization() ");
                System.runFinalization();
                System.out.println(k + ": calling System.gc() " );
                System.gc();
            }
        }
        System.out.println("Sleeping 2 sec...");
        Thread.sleep(2000);
        // allows finishing any long-time finalization procedures performed via daemon threads
    }
}
