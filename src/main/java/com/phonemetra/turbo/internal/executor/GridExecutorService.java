/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phonemetra.turbo.internal.executor;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.phonemetra.turbo.TurboSQLCheckedException;
import com.phonemetra.turbo.TurboSQLLogger;
import com.phonemetra.turbo.internal.GridKernalContext;
import com.phonemetra.turbo.internal.TurboSQLFutureTimeoutCheckedException;
import com.phonemetra.turbo.internal.TurboSQLInternalFuture;
import com.phonemetra.turbo.internal.cluster.ClusterGroupAdapter;
import com.phonemetra.turbo.internal.compute.ComputeTaskTimeoutCheckedException;
import com.phonemetra.turbo.internal.util.typedef.CX1;
import com.phonemetra.turbo.internal.util.typedef.internal.A;
import com.phonemetra.turbo.internal.util.typedef.internal.U;
import com.phonemetra.turbo.lang.TurboSQLInClosure;

import static com.phonemetra.turbo.internal.GridClosureCallMode.BALANCE;

/**
 * An {@link ExecutorService} that executes each submitted task in grid
 * through {@link com.phonemetra.turbo.TurboSQL} instance, normally configured using
 * {@link com.phonemetra.turbo.TurboSQL#executorService()} method.
 * {@code GridExecutorService} delegates commands execution to already
 * started {@link com.phonemetra.turbo.TurboSQL} instance. Every submitted task will be serialized and
 * transferred to any node in grid.
 * <p>
 * All submitted tasks must implement {@link Serializable} interface.
 * <p>
 * Note, that GridExecutorService implements ExecutorService from JDK 1.5.
 * If you have problems with compilation for JDK 1.6 and above you need to apply
 * some changes (see <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6267833">http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6267833</a>)
 * <p>
 * Change signature for methods {@link GridExecutorService#invokeAll(Collection)},
 * {@link GridExecutorService#invokeAll(Collection, long, TimeUnit)},
 * {@link GridExecutorService#invokeAny(Collection)},
 * {@link GridExecutorService#invokeAny(Collection, long, TimeUnit)} to
 * <pre name="code" class="java">
 * public class GridExecutorService implements ExecutorService {
 * ...
 *     public &lt;T&gt; List&lt;Future&lt;T&gt;&gt; invokeAll(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks) throws InterruptedException {
 *         ...
 *     }
 *
 *     public &lt;T&gt; List&lt;Future&lt;T&gt;&gt; invokeAll(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks, long timeout, TimeUnit unit)
 *         throws InterruptedException {
 *         ...
 *     }
 *
 *     public &lt;T&gt; T invokeAny(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks) throws InterruptedException, ExecutionException {
 *         ...
 *     }
 *
 *     public &lt;T&gt; T invokeAny(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks, long timeout, TimeUnit unit)
 *         throws InterruptedException, ExecutionException, TimeoutException {
 *     }
 *     ...
 * }
 * </pre>
 */
public class GridExecutorService implements ExecutorService, Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Projection. */
    private ClusterGroupAdapter prj;

    /** */
    private GridKernalContext ctx;

    /** Logger. */
    private TurboSQLLogger log;

    /** Whether service is being stopped or not. */
    private boolean isBeingShutdown;

    /** List of executing or scheduled for execution tasks. */
    private List<TurboSQLInternalFuture<?>> futs = new ArrayList<>();

    /** Rejected or completed tasks listener. */
    private TaskTerminateListener lsnr = new TaskTerminateListener<>();

    /** */
    private final Object mux = new Object();

    /**
     * No-arg constructor is required by externalization.
     */
    public GridExecutorService() {
        // No-op.
    }

    /**
     * Creates executor service.
     *
     * @param prj Projection.
     * @param ctx Grid logger.
     */
    public GridExecutorService(ClusterGroupAdapter prj, GridKernalContext ctx) {
        assert prj != null;
        assert ctx != null;

        this.prj = prj;
        this.ctx = ctx;
        this.log = ctx.log(GridExecutorService.class);
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(prj);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        prj = (ClusterGroupAdapter)in.readObject();
    }

    /**
     * Reconstructs object on unmarshalling.
     *
     * @return Reconstructed object.
     * @throws ObjectStreamException Thrown in case of unmarshalling error.
     */
    protected Object readResolve() throws ObjectStreamException {
        return prj.executorService();
    }

    /** {@inheritDoc} */
    @Override public void shutdown() {
        synchronized (mux) {
            if (isBeingShutdown)
                return;

            isBeingShutdown = true;
        }
    }

    /** {@inheritDoc} */
    @Override public List<Runnable> shutdownNow() {
        List<TurboSQLInternalFuture<?>> cpFuts;

        // Cancel all tasks.
        synchronized (mux) {
            cpFuts = new ArrayList<>(futs);

            isBeingShutdown = true;
        }

        for (TurboSQLInternalFuture<?> task : cpFuts) {
            try {
                task.cancel();
            }
            catch (TurboSQLCheckedException e) {
                U.error(log, "Failed to cancel task: " + task, e);
            }
        }

        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override public boolean isShutdown() {
        synchronized (mux) {
            return isBeingShutdown;
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isTerminated() {
        synchronized (mux) {
            return isBeingShutdown && futs.isEmpty();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long now = U.currentTimeMillis();

        timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);

        long end = timeout == 0 ? Long.MAX_VALUE : timeout + now;

        // Prevent overflow.
        if (end < 0)
            end = Long.MAX_VALUE;

        List<TurboSQLInternalFuture<?>> locTasks;

        // Cancel all tasks.
        synchronized (mux) {
            locTasks = new ArrayList<>(futs);
        }

        Iterator<TurboSQLInternalFuture<?>> iter = locTasks.iterator();

        while (iter.hasNext() && now < end) {
            TurboSQLInternalFuture<?> fut = iter.next();

            try {
                fut.get(end - now);
            }
            catch (ComputeTaskTimeoutCheckedException e) {
                U.error(log, "Failed to get task result: " + fut, e);

                return false;
            }
            catch (TurboSQLCheckedException e) {
                U.error(log, "Failed to get task result: " + fut, e);

                if (e.getCause() instanceof InterruptedException)
                    throw new InterruptedException("Got interrupted while waiting for task completion.");
            }

            now = U.currentTimeMillis();
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public <T> Future<T> submit(Callable<T> task) {
        A.notNull(task, "task != null");

        checkShutdown();

        ctx.gateway().readLock();

        try {
            return addFuture(ctx.closure().callAsync(BALANCE, task, prj.nodes()));
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /** {@inheritDoc} */
    @Override public <T> Future<T> submit(Runnable task, final T res) {
        A.notNull(task, "task != null");

        checkShutdown();

        ctx.gateway().readLock();

        try {
            TurboSQLInternalFuture<T> fut = ctx.closure().runAsync(BALANCE, task, prj.nodes()).chain(
                new CX1<TurboSQLInternalFuture<?>, T>() {
                @Override public T applyx(TurboSQLInternalFuture<?> fut) throws TurboSQLCheckedException {
                    fut.get();

                    return res;
                }
            });

            return addFuture(fut);
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /** {@inheritDoc} */
    @Override public Future<?> submit(Runnable task) {
        A.notNull(task, "task != null");

        checkShutdown();

        ctx.gateway().readLock();

        try {
            return addFuture(ctx.closure().runAsync(BALANCE, task, prj.nodes()));
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note, for compilation with JDK 1.6 necessary to change method signature
     * (note the {@code &lt;? extends T&gt;} clause).
     * <pre name="code" class="java">
     *     ...
     *     public &lt;T&gt; List&lt;Future&lt;T&gt;&gt; invokeAll(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks) throws InterruptedException {
     *         // Method body.
     *     }
     *     ...
     * </pre>
     */
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return invokeAll(tasks, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note, for compilation with JDK 1.6 necessary to change method signature
     * (note the {@code &lt;? extends T&gt;} clause).
     * <pre name="code" class="java">
     *     public &lt;T&gt; List&lt;Future&lt;T&gt;&gt; invokeAll(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks, long timeout, TimeUnit unit)
     *         throws InterruptedException {
     *         ...
     *     }
     * </pre>
     */
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException {
        A.notNull(tasks, "tasks != null");
        A.ensure(timeout >= 0, "timeout >= 0");
        A.notNull(unit, "unit != null");

        long now = U.currentTimeMillis();

        timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);

        long end = timeout == 0 ? Long.MAX_VALUE : timeout + now;

        // Prevent overflow.
        if (end < 0)
            end = Long.MAX_VALUE;

        checkShutdown();

        Collection<TurboSQLInternalFuture<T>> taskFuts = new ArrayList<>();

        for (Callable<T> task : tasks) {
            // Execute task without predefined timeout.
            // GridFuture.cancel() will be called if timeout elapsed.
            TurboSQLInternalFuture<T> fut;

            ctx.gateway().readLock();

            try {
                fut = ctx.closure().callAsync(BALANCE, task, prj.nodes());
            }
            finally {
                ctx.gateway().readUnlock();
            }

            taskFuts.add(fut);

            now = U.currentTimeMillis();
        }

        boolean isInterrupted = false;

        for (TurboSQLInternalFuture<T> fut : taskFuts) {
            if (!isInterrupted && now < end) {
                try {
                    fut.get(end - now);
                }
                catch (ComputeTaskTimeoutCheckedException ignore) {
                    if (log.isDebugEnabled())
                        log.debug("Timeout occurred during getting task result: " + fut);

                    cancelFuture(fut);
                }
                catch (TurboSQLCheckedException e) {
                    if (e.getCause() instanceof InterruptedException) {
                        // This invokeAll() method was interrupted (therefore, need to cancel all tasks).
                        // Note: that execution may be interrupted on remote node. Possible bug.
                        isInterrupted = true;

                        cancelFuture(fut);
                    }
                }
            }

            now = U.currentTimeMillis();
        }

        // Throw exception if any task wait was interrupted.
        if (isInterrupted)
            throw new InterruptedException("Got interrupted while waiting for tasks invocation.");

        List<Future<T>> futs = new ArrayList<>(taskFuts.size());

        // Convert futures.
        for (TurboSQLInternalFuture<T> fut : taskFuts) {
            // Per executor service contract any task that was not completed
            // should be cancelled upon return.
            if (!fut.isDone())
                cancelFuture(fut);

            futs.add(new TaskFutureWrapper<>(fut));
        }

        return futs;
    }

    /**
     * Cancels given future.
     *
     * @param fut Future to cancel.
     */
    private void cancelFuture(TurboSQLInternalFuture<?> fut) {
        try {
            fut.cancel();
        }
        catch (TurboSQLCheckedException e) {
            U.error(log, "Failed to cancel task: " + fut, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note, for compilation with JDK 1.6 necessary to change method signature
     * (note the {@code &lt;? extends T&gt;} clause).
     * <pre name="code" class="java">
     *     ...
     *     public &lt;T&gt; T invokeAny(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks) throws InterruptedException, ExecutionException {
     *         // Method body.
     *     }
     *     ...
     * </pre>
     */
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
        ExecutionException {
        try {
            return invokeAny(tasks, 0, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            throw new ExecutionException("Timeout occurred during commands execution.", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note, for compilation with JDK 1.6 necessary to change method signature
     * (note the {@code &lt;? extends T&gt;} clause).
     * <pre name="code" class="java">
     *     ...
     *     public &lt;T&gt; T invokeAny(Collection&lt;? extends Callable&lt;T&gt;&gt; tasks, long timeout, TimeUnit unit)
     *         throws InterruptedException, ExecutionException, TimeoutException {
     *     }
     *     ...
     * </pre>
     */
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        A.notNull(tasks, "tasks != null");
        A.ensure(!tasks.isEmpty(), "!tasks.isEmpty()");
        A.ensure(timeout >= 0, "timeout >= 0");
        A.notNull(unit, "unit != null");

        long now = System.currentTimeMillis();

        timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);

        long end = timeout == 0 ? Long.MAX_VALUE : timeout + now;

        // Prevent overflow.
        if (end < 0)
            end = Long.MAX_VALUE;

        checkShutdown();

        Collection<TurboSQLInternalFuture<T>> taskFuts = new ArrayList<>();

        for (Callable<T> cmd : tasks) {
            // Execute task with predefined timeout.
            TurboSQLInternalFuture<T> fut;

            ctx.gateway().readLock();

            try {
                fut = ctx.closure().callAsync(BALANCE, cmd, prj.nodes());
            }
            finally {
                ctx.gateway().readUnlock();
            }

            taskFuts.add(fut);
        }

        T res = null;

        boolean isInterrupted = false;
        boolean isResRcvd = false;

        int errCnt = 0;

        for (TurboSQLInternalFuture<T> fut : taskFuts) {
            now = U.currentTimeMillis();

            boolean cancel = false;

            if (!isInterrupted && !isResRcvd && now < end) {
                try {
                    res = fut.get(end - now);

                    isResRcvd = true;

                    // Cancel next tasks (avoid current task cancellation below in loop).
                    continue;
                }
                catch (TurboSQLFutureTimeoutCheckedException ignored) {
                    if (log.isDebugEnabled())
                        log.debug("Timeout occurred during getting task result: " + fut);

                    cancel = true;
                }
                catch (TurboSQLCheckedException e) {
                    // This invokeAll() method was interrupted (therefore, need to cancel all tasks).
                    // Note: that execution may be interrupted on remote node. Possible bug.
                    if (e.getCause() instanceof InterruptedException)
                        isInterrupted = true;
                    else
                        errCnt++;
                }
            }

            // Cancel active task if any task interrupted, timeout elapsed or received task result before.
            if ((isInterrupted || isResRcvd || cancel) && !fut.isDone())
                cancelFuture(fut);
        }

        // Throw exception if any task wait was interrupted.
        if (isInterrupted)
            throw new InterruptedException("Got interrupted while waiting for tasks invocation.");

        // If every task failed - throw execution exception
        // per executor service contract.
        if (!isResRcvd && taskFuts.size() == errCnt)
            throw new ExecutionException("Failed to get any task completion.", null);

        // In all other cases with no results received by the time timeout elapsed -
        // throw timeout exception per executor service contract.
        if (!isResRcvd)
            throw new TimeoutException("Timeout occurred during tasks invocation.");

        return res;
    }

    /** {@inheritDoc} */
    @Override public void execute(Runnable cmd) {
        A.notNull(cmd, "cmd != null");

        checkShutdown();

        ctx.gateway().readLock();

        try {
            addFuture(ctx.closure().runAsync(BALANCE, cmd, prj.nodes()));
        }
        finally {
            ctx.gateway().readUnlock();
        }
    }

    /**
     * Checks if service is being shutdown.
     */
    private void checkShutdown() {
        synchronized (mux) {
            if (isBeingShutdown)
                throw new RejectedExecutionException("Failed to execute command during executor shutdown.");
        }
    }

    /**
     * @param <T> Type of command result.
     * @param fut Future to add.
     * @return Future for command.
     */
    @SuppressWarnings("unchecked")
    private <T> Future<T> addFuture(TurboSQLInternalFuture<T> fut) {
        synchronized (mux) {
            if (!fut.isDone()) {
                fut.listen(lsnr);

                futs.add(fut);
            }

            return new TaskFutureWrapper<>(fut);
        }
    }

    /**
     * Listener to track tasks.
     */
    private class TaskTerminateListener<T> implements TurboSQLInClosure<TurboSQLInternalFuture<T>> {
        /** */
        private static final long serialVersionUID = 0L;

        /** {@inheritDoc} */
        @Override public void apply(TurboSQLInternalFuture<T> taskFut) {
            synchronized (mux) {
                futs.remove(taskFut);
            }
        }
    }

    /**
     * Wrapper for {@link com.phonemetra.turbo.internal.TurboSQLInternalFuture}.
     * Used for compatibility {@link Future} interface.
     * @param <T> The result type of the {@link Future} argument.
     */
    private class TaskFutureWrapper<T> implements Future<T> {
        /** */
        private final TurboSQLInternalFuture<T> fut;

        /**
         * Creates wrapper.
         *
         * @param fut Grid future.
         */
        TaskFutureWrapper(TurboSQLInternalFuture<T> fut) {
            assert fut != null;

            this.fut = fut;
        }

        /** {@inheritDoc} */
        @Override public boolean cancel(boolean mayInterruptIfRunning) {
            try {
                fut.cancel();
            }
            catch (TurboSQLCheckedException e) {
                U.error(log, "Failed to cancel task: " + fut, e);
            }

            return true;
        }

        /** {@inheritDoc} */
        @Override public boolean isCancelled() {
            return fut.isCancelled();
        }

        /** {@inheritDoc} */
        @Override public boolean isDone() {
            return fut.isDone();
        }

        /** {@inheritDoc} */
        @Override public T get() throws ExecutionException {
            try {
                T res = fut.get();

                if (fut.isCancelled())
                    throw new CancellationException("Task was cancelled: " + fut);

                return res;
            }
            catch (TurboSQLCheckedException e) {
                // Task cancellation may cause throwing exception.
                if (fut.isCancelled()) {
                    RuntimeException ex = new CancellationException("Task was cancelled: " + fut);

                    ex.initCause(e);

                    throw ex;
                }

                throw new ExecutionException("Failed to get task result: " + fut, e);
            }
        }

        /** {@inheritDoc} */
        @Override public T get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
            A.ensure(timeout >= 0, "timeout >= 0");
            A.notNull(unit, "unit != null");

            try {
                T res = fut.get(unit.toMillis(timeout));

                if (fut.isCancelled())
                    throw new CancellationException("Task was cancelled: " + fut);

                return res;
            }
            catch (TurboSQLFutureTimeoutCheckedException e) {
                TimeoutException e2 = new TimeoutException();

                e2.initCause(e);

                throw e2;
            }
            catch (ComputeTaskTimeoutCheckedException e) {
                throw new ExecutionException("Task execution timed out during waiting for task result: " + fut, e);
            }
            catch (TurboSQLCheckedException e) {
                // Task cancellation may cause throwing exception.
                if (fut.isCancelled()) {
                    RuntimeException ex = new CancellationException("Task was cancelled: " + fut);

                    ex.initCause(e);

                    throw ex;
                }

                throw new ExecutionException("Failed to get task result.", e);
            }
        }
    }
}
