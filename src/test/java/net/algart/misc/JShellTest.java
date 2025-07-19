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

package net.algart.misc;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;

import java.util.List;

public class JShellTest {

    public static void main(String[] args) {
        try (JShell jshell = JShell.create()) {
            jshell.eval("double x = 100;");
            List<SnippetEvent> events = jshell.eval("double x = 200; double y = 300;");
            // - reassigning x leads to 2 snippets
            for (SnippetEvent e : events) {
                System.out.println("Snippet: " + e.snippet().source());
                System.out.println("Kind: " + e.snippet().kind());
                System.out.println("Status: " + e.status());
                System.out.println("Result: " + e.value());
            }
        }
    }
}