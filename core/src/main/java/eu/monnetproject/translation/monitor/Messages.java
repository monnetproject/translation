/**
 * ********************************************************************************
 * Copyright (c) 2011, Monnet Project All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. * Neither the name of the Monnet Project nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************
 */
package eu.monnetproject.translation.monitor;

import eu.monnetproject.translation.Label;
import java.net.URI;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author John McCrae
 */
public final class Messages {

    private static Map<Long, JobImpl> jobs = new HashMap<Long, JobImpl>();
    public static List<MessageHandler> handlers = new LinkedList<MessageHandler>();

    public static Job beginTranslation(int size) {
        if (jobs.get(Thread.currentThread().getId()) != null) {
            final Message msg = new Message(MessageType.SEVERE, "Job did not close cleanly", jobs.get(Thread.currentThread().getId()));
            fire(msg);
        }
        final JobImpl job = new JobImpl(size);
        jobs.put(Thread.currentThread().getId(), job);
        for (MessageHandler handler : handlers) {
            handler.beginJob(job);
        }
        return job;
    }

    public static void endTranslation(Job job) {
        if (jobs.get(Thread.currentThread().getId()) != job) {
            final Message msg = new Message(MessageType.SEVERE, "Job did not close cleanly", jobs.get(Thread.currentThread().getId()));
            fire(msg);
        }
        final Iterator<Map.Entry<Long, JobImpl>> iter = jobs.entrySet().iterator();
        while (iter.hasNext()) {
            if (iter.next().getValue() == job) {
                iter.remove();
            }
        }
        for (MessageHandler handler : handlers) {
            handler.endJob(job);
        }
    }

    public static void associateThread(Job job, Thread thread) {
        if (jobs.get(thread.getId()) != null && jobs.get(thread.getId()) != job) {
            final Message msg = new Message(MessageType.SEVERE, "Job did not close cleanly", jobs.get(Thread.currentThread().getId()));
            fire(msg);
        }
        if (!(job instanceof JobImpl)) {
            throw new IllegalArgumentException("Bad job");
        } else {
            jobs.put(thread.getId(), (JobImpl) job);
        }
    }

    private static Job currentJob() {
        return jobs.get(Thread.currentThread().getId());
    }

    private static void fire(Message msg) {
        for (MessageHandler handler : handlers) {
            handler.handle(msg);
        }
    }

    public static void addHandler(MessageHandler handler) {
        if (!handlers.contains(handler)) {
            handlers.add(handler);
        }
    }

    public static void translationStart(URI entity) {
        final Message msg = new Message(MessageType.START, entity, currentJob());
        fire(msg);
    }

    public static void translationSuccess(URI entity, Label targetLabel) {
        final Message msg = new Message(MessageType.SUCCESS, entity, targetLabel.asString(), currentJob());
        fire(msg);
    }

    public static void translationFail(URI entity, Throwable cause) {
        final Message msg = new Message(MessageType.FAIL, entity, cause, currentJob());
        fire(msg);
    }

    public static void translationFail(URI entity, String message) {
        final Message msg = new Message(MessageType.FAIL, entity, message, currentJob());
        fire(msg);
    }

    public static void componentLoadFail(Class<?> component, Throwable cause) {
        final Message msg = new Message(MessageType.CFAIL, component, cause, currentJob());
        fire(msg);
    }

    public static void componentLoadFail(Class<?> component, String message) {
        final Message msg = new Message(MessageType.CFAIL, component, message, currentJob());
        fire(msg);
    }

    public static void componentLoadSuccess(Class<?> component) {
        final Message msg = new Message(MessageType.CLOAD, component, currentJob());
        fire(msg);
    }

    public static void cleanupFailure(Throwable t) {
        final Message msg = new Message(MessageType.CLEAN_FAIL, t, currentJob());
        fire(msg);
    }

    public static void info(String message) {
        final Message msg = new Message(MessageType.INFO, message, currentJob());
        fire(msg);
    }

    public static void warning(String message) {
        final Message msg = new Message(MessageType.WARNING, message, currentJob());
        fire(msg);
    }

    public static void severe(String message) {
        final Message msg = new Message(MessageType.SEVERE, message, currentJob());
        fire(msg);
    }

    private static class JobImpl implements Job {

        private final long id = new Random().nextLong();
        public final int size;

        public JobImpl(int size) {
            this.size = size;
        }

        @Override
        public long id() {
            return id;
        }
    }

    public static enum MessageType {

        START,
        SUCCESS,
        FAIL,
        CLOAD,
        CFAIL,
        INFO,
        WARNING,
        SEVERE,
        CLEAN_FAIL
    }

    public static class Message {

        public final MessageType type;
        public final String message;
        public final Job job;
        public final URI entity;
        public final Class<?> component;
        public final Throwable cause;

        public Message(MessageType type, URI entity, Job job) {
            this.type = type;
            this.message = "";
            this.job = job;
            this.entity = entity;
            this.component = null;
            this.cause = null;
        }

        public Message(MessageType type, URI entity, String message, Job job) {
            this.type = type;
            this.message = message;
            this.job = job;
            this.entity = entity;
            this.component = null;
            this.cause = null;
        }

        public Message(MessageType type, URI entity, Throwable cause, Job job) {
            this.type = type;
            this.message = "";
            this.job = job;
            this.entity = entity;
            this.component = null;
            this.cause = cause;
        }

        public Message(MessageType type, Class<?> component, Throwable cause, Job job) {
            this.type = type;
            this.message = "";
            this.job = job;
            this.entity = null;
            this.component = component;
            this.cause = cause;
        }

        public Message(MessageType type, Class<?> component, String message, Job job) {
            this.type = type;
            this.message = message;
            this.job = job;
            this.entity = null;
            this.component = component;
            this.cause = null;
        }

        public Message(MessageType type, Class<?> component, Job job) {
            this.type = type;
            this.message = "";
            this.job = job;
            this.entity = null;
            this.component = component;
            this.cause = null;
        }

        public Message(MessageType type, String message, Job job) {
            this.type = type;
            this.message = message;
            this.job = job;
            this.entity = null;
            this.component = null;
            this.cause = null;
        }

        public Message(MessageType type, Throwable cause, Job job) {
            this.type = type;
            this.message = "";
            this.job = job;
            this.entity = null;
            this.component = null;
            this.cause = cause;
        }
    }
}
