/*
 * Copyright 2014 The Solmix Project
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.gnu.org/licenses/ 
 * or see the FSF site: http://www.fsf.org. 
 */
package org.solmix.service.event.deliver;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.solmix.runtime.threadpool.ThreadPool;
import org.solmix.service.event.EventDeliver;
import org.solmix.service.event.EventTask;


/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2015年7月14日
 */

public class AsyncDeliver implements EventDeliver
{

    private final ThreadPool threadPool;

    private final EventDeliver syn_deliver;

    private final Map<Thread, EventExecutor> running_threads = new HashMap<Thread, EventExecutor>();

    public AsyncDeliver(final ThreadPool pool, final EventDeliver synDeliver)
    {
        threadPool = pool;
        syn_deliver = synDeliver;
    }

    /**
     * 
     * If the Event publisher used the same thread,the Event Handler also used the same thread as a synchronous event.
     * 
     */
    @Override
    public void execute(List<EventTask> tasks) {
        final Thread currentThread = Thread.currentThread();
        EventExecutor executer = null;
        synchronized (running_threads) {
            EventExecutor runningExecutor = running_threads.get(currentThread);
            if (runningExecutor != null) {
                runningExecutor.add(tasks);
            } else {
                executer = new EventExecutor(tasks, currentThread);
                running_threads.put(currentThread, executer);
            }
        }
        if (executer != null) {
            threadPool.execute(executer);
        }

    }

    private final class EventExecutor implements Runnable
    {

        private final List<List<EventTask>> _tasks = new LinkedList<List<EventTask>>();

        private final Object _key;

        public EventExecutor(final List<EventTask> tasks, final Object key)
        {
            _key = key;
            _tasks.add(tasks);
        }

        @Override
        public void run() {
            boolean running;
            do {
                List<EventTask> tasks = null;
                synchronized (_tasks) {
                    tasks = _tasks.remove(0);
                }
                syn_deliver.execute(tasks);
                synchronized (running_threads) {
                    running = _tasks.size() > 0;
                    if (!running) {
                        running_threads.remove(_key);
                    }
                }
            } while (running);
        }

        public void add(final List<EventTask> tasks) {
            synchronized (_tasks) {
                _tasks.add(tasks);
            }
        }
    }

}
