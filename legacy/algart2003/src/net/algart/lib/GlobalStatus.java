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

import net.algart.status.*;

/**
 * <p>Implements global status line support.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */

public final class GlobalStatus implements TrueStatic {
// TrueStatic is necessary because status should be really global

    /**
    * Don't let anyone instantiate this class.
    */
    private GlobalStatus() {}

    public static String set(String status) {
        return set(null,status);
    }
    public static String get() { // Never returns null
        return get(null);
    }
    public static StatusShowerWithMemory getShower() { // Never returns null
        return getShower(null);
    }

    public static String setSys(String status) {
        return setSys(null,status);
    }
    public static String getSys() { // Never returns null
        return getSys(null);
    }
    public static StatusShowerWithMemory getSysShower() { // Never returns null
        return getSysShower(null);
    }

    public static String set(Object contextId, String status) {
        if (statusProducer != null)
            statusProducer.setStatus(contextId,status);
        return status;
    }
    public static String get(Object contextId) { // Never returns null
        if (statusProducer == null) return "";
        String s = statusProducer.getStatus(contextId);
        return s == null? "": s;
    }
    public static StatusShowerWithMemory getShower(Object contextId) { // Never returns null
        if (statusProducer == null) return new EmptyStatusShower();
        StatusShowerWithMemory shower = statusProducer.getStatusShower(contextId);
        return shower == null? new StatusShower(contextId,false): shower;
    }
    public static Object currentContextId() {
        if (statusProducer == null) return null;
        return statusProducer.currentContextId();
    }


    public static String setSys(Object contextId, String systemStatus) {
        if (statusProducer instanceof SystemStatusProducer)
            ((SystemStatusProducer)statusProducer).setSystemStatus(contextId,systemStatus);
        return systemStatus;
    }
    public static String getSys(Object contextId) { // Never returns null
        if (!(statusProducer instanceof SystemStatusProducer)) return "";
        String s = ((SystemStatusProducer)statusProducer).getSystemStatus(contextId);
        return s == null? "": s;
    }
    public static StatusShowerWithMemory getSysShower(Object contextId) { // Never returns null
        if (!(statusProducer instanceof SystemStatusProducer)) return new EmptyStatusShower();
        StatusShowerWithMemory shower = ((SystemStatusProducer)statusProducer).getSystemStatusShower(contextId);
        return shower == null? new StatusShower(contextId,true): shower;
    }
    public static Object currentSysContextId() {
        if (!(statusProducer instanceof SystemStatusProducer)) return null;
        return ((SystemStatusProducer)statusProducer).currentSysContextId();
    }

    public interface StatusProducer {
        public String getStatus(Object contextId);
        public void setStatus(Object contextId, String status);
        public StatusShowerWithMemory getStatusShower(Object contextId);
        public Object currentContextId();
            // null argument in getStatus/setStatus must work as a result of currentContextId()
    }

    public interface SystemStatusProducer {
        public String getSystemStatus(Object contextId);
        public void setSystemStatus(Object contextId, String systemStatus);
        public StatusShowerWithMemory getSystemStatusShower(Object contextId);
        public Object currentSysContextId();
            // null argument in getStatus/setStatus must work as a result of currentSysContextId()
    }

    public interface StatusesProducer extends StatusProducer,SystemStatusProducer {
    }

    private static StatusProducer statusProducer;
    public static void setStatusProducer(StatusProducer v) {
        statusProducer = v;
    }
    public static StatusProducer getStatusProducer() {
        return statusProducer;
    }


    public static class StatusShower implements StatusShowerWithMemory {
        final Object contextId;
        final boolean systemStatus;
        public StatusShower(Object contextId, boolean systemStatus) {
            this.contextId = contextId;
            this.systemStatus = systemStatus;
        }
        public StatusShower(Object contextId) {
            this(contextId,false);
        }
        public boolean isSystemStatus() {
            return systemStatus;
        }
        public Object getContextId() {
            return contextId;
        }

        private String lastStatus = "";
        public void showStatus(String status) {
            lastStatus = status;
            showStatusPreservingCurrent(status);
        }
        public String getStatus() {
            return lastStatus;
        }
        public void showStatusPreservingCurrent(String status) {
            if (systemStatus)
                GlobalStatus.setSys(contextId,status);
            else
                GlobalStatus.set(contextId,status);
        }
    }

}