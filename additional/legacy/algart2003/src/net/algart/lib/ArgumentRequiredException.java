/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001-2003 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.lib;

public class ArgumentRequiredException extends AbstractException {
    public static final String INFO_XML_TAG_NAME = "required-arguments";

    private final String[] argumentNames;

    public ArgumentRequiredException(String message, String argumentName) {
        super(message);
        this.argumentNames = new String[1];
        this.argumentNames[0] = argumentName;
    }

    public ArgumentRequiredException(String message, String[] argumentNames) {
        super(message);
        this.argumentNames = new String[argumentNames.length];
        System.arraycopy(argumentNames,0,this.argumentNames,0,argumentNames.length);
    }

    public String[] getArgumentNames() {
        return (String[])argumentNames.clone();
    }

    public String toXmlString() {
        if (argumentNames.length == 0)
            return super.toXmlString();
        return super.toXmlString() + "\n" + Strings.xmlTag(
            INFO_XML_TAG_NAME,Out.join(argumentNames,", "));
    }

}