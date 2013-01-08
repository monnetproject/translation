/**********************************************************************************
 * Copyright (c) 2011, Monnet Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Monnet Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
package eu.monnetproject.translation.evaluation.evaluate;

import eu.monnetproject.util.LogPolicy;
import eu.monnetproject.util.Logger;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author John McCrae
 */
public class EvaluateLogger implements Logger {

    private final String className;
    private final static List<LogEntry> logEntries = new LinkedList<LogEntry>();

    public EvaluateLogger(String className) {
        this.className = className;
    }

    public static void writeXML(StringBuilder builder, int offset) {
        for (int i = 0; i < offset; i++) {
            builder.append("\t");
        }
        builder.append("<log>\n");
        for(LogEntry le : logEntries) {
            le.writeXML(builder, offset+1);
        }
        for (int i = 0; i < offset; i++) {
            builder.append("\t");
        }
        builder.append("</log>\n");
        
    }

    @Override
    public void severe(String msg) {
        logEntries.add(new SimpleLogEntry(Level.SEVERE, msg, className));
        System.err.println("[SEVERE] "+msg);
    }

    @Override
    public void warning(String msg) {
        logEntries.add(new SimpleLogEntry(Level.WARNING, msg, className));
        System.err.println("[WARNING] "+msg);
    }

    @Override
    public void info(String msg) {
        logEntries.add(new SimpleLogEntry(Level.INFO, msg, className));
        System.err.println("[INFO] "+msg);
    }

    @Override
    public void config(String msg) {
        logEntries.add(new SimpleLogEntry(Level.CONFIG, msg, className));
    }

    @Override
    public void fine(String msg) {
        logEntries.add(new SimpleLogEntry(Level.FINE, msg, className));
    }

    @Override
    public void finer(String msg) {
        logEntries.add(new SimpleLogEntry(Level.FINER, msg, className));
    }

    @Override
    public void finest(String msg) {
        logEntries.add(new SimpleLogEntry(Level.FINEST, msg, className));
    }

    @Override
    public void setPolicy(LogPolicy policy) {
    }

    @Override
    public void stackTrace(Throwable x) {
        logEntries.add(new ExceptionLogEntry(x, className));
        x.printStackTrace();
    }

    @Override
    public String getName() {
        return "EvaluateLogger for " + className;
    }

    private static interface LogEntry {

        void writeXML(StringBuilder builder, int offset);
    }

    private static class SimpleLogEntry implements LogEntry {

        private final Level level;
        private final String message;
        private final String className;
        private final long time;

        public SimpleLogEntry(Level level, String message, String className) {
            this.level = level;
            this.message = message;
            this.className = className;
            this.time = System.currentTimeMillis();
        }

        @Override
        public void writeXML(StringBuilder builder, int offset) {
            for (int i = 0; i < offset; i++) {
                builder.append("\t");
            }
            builder.append("<msg source=\"").append(className).append("\" level=\"").append(level.toString()).append("\">").append(message).append("</msg>\n");
        }
    }

    private static class ExceptionLogEntry implements LogEntry {

        private final Throwable t;
        private final String className;
        private final long time;

        public ExceptionLogEntry(Throwable t, String className) {
            this.t = t;
            this.className = className;
            this.time = System.currentTimeMillis();
        }

        @Override
        public void writeXML(StringBuilder builder, int offset) {
            for (int i = 0; i < offset; i++) {
                builder.append("\t");
            }
            builder.append("<exception source=\"").append(className).append("\" type=\"").append(t.getClass().getName()).append("\" message=\"").append(t.getMessage()).append("\">\n");
            for (StackTraceElement ste : t.getStackTrace()) {
                for (int i = 0; i < offset + 1; i++) {
                    builder.append("\t");
                }
                builder.append("<trace class=\"").append(ste.getClassName()).append("\" line=\"").append(ste.getLineNumber()).append("\"/>");
            }
            for (int i = 0; i < offset; i++) {
                builder.append("\t");
            }
            builder.append("</exception>\n");
        }
    }
}
