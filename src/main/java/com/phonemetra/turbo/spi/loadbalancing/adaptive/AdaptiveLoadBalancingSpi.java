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

package com.phonemetra.turbo.spi.loadbalancing.adaptive;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.phonemetra.turbo.TurboSQLCheckedException;
import com.phonemetra.turbo.TurboSQLException;
import com.phonemetra.turbo.TurboSQLLogger;
import com.phonemetra.turbo.cluster.ClusterNode;
import com.phonemetra.turbo.compute.ComputeJob;
import com.phonemetra.turbo.compute.ComputeTaskSession;
import com.phonemetra.turbo.events.DiscoveryEvent;
import com.phonemetra.turbo.events.Event;
import com.phonemetra.turbo.events.JobEvent;
import com.phonemetra.turbo.events.TaskEvent;
import com.phonemetra.turbo.internal.managers.eventstorage.GridLocalEventListener;
import com.phonemetra.turbo.internal.util.typedef.F;
import com.phonemetra.turbo.internal.util.typedef.internal.A;
import com.phonemetra.turbo.internal.util.typedef.internal.S;
import com.phonemetra.turbo.lang.TurboSQLBiTuple;
import com.phonemetra.turbo.lang.TurboSQLUuid;
import com.phonemetra.turbo.resources.LoggerResource;
import com.phonemetra.turbo.spi.TurboSQLSpiAdapter;
import com.phonemetra.turbo.spi.TurboSQLSpiConfiguration;
import com.phonemetra.turbo.spi.TurboSQLSpiContext;
import com.phonemetra.turbo.spi.TurboSQLSpiException;
import com.phonemetra.turbo.spi.TurboSQLSpiMBeanAdapter;
import com.phonemetra.turbo.spi.TurboSQLSpiMultipleInstancesSupport;
import com.phonemetra.turbo.spi.loadbalancing.LoadBalancingSpi;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.ConcurrentHashMap;

import static com.phonemetra.turbo.events.EventType.EVT_JOB_MAPPED;
import static com.phonemetra.turbo.events.EventType.EVT_NODE_FAILED;
import static com.phonemetra.turbo.events.EventType.EVT_NODE_JOINED;
import static com.phonemetra.turbo.events.EventType.EVT_NODE_LEFT;
import static com.phonemetra.turbo.events.EventType.EVT_NODE_METRICS_UPDATED;
import static com.phonemetra.turbo.events.EventType.EVT_TASK_FAILED;
import static com.phonemetra.turbo.events.EventType.EVT_TASK_FINISHED;

/**
 * Load balancing SPI that adapts to overall node performance. It
 * proportionally distributes more jobs to more performant nodes based
 * on a pluggable and dynamic node load probing.
 * <p>
 * <h1 class="header">Adaptive Node Probe</h1>
 * This SPI comes with pluggable algorithm to calculate a node load
 * at any given point of time. The algorithm is defined by
 * {@link AdaptiveLoadProbe} interface and user is
 * free to provide custom implementations. By default
 * {@link AdaptiveCpuLoadProbe} implementation is used
 * which distributes jobs to nodes based on average CPU load
 * on every node.
 * <p>
 * The following load probes are available with the product:
 * <ul>
 * <li>{@link AdaptiveCpuLoadProbe} - default</li>
 * <li>{@link AdaptiveProcessingTimeLoadProbe}</li>
 * <li>{@link AdaptiveJobCountLoadProbe}</li>
 * </ul>
 * Note that if {@link AdaptiveLoadProbe#getLoad(com.phonemetra.turbo.cluster.ClusterNode, int)} returns a value of {@code 0},
 * then implementation will assume that load value is simply not available and
 * will try to calculate an average of load values for other nodes. If such
 * average cannot be obtained (all node load values are {@code 0}), then a value
 * of {@code 1} will be used.
 * <p>
 * When working with node metrics, take into account that all averages are
 * calculated over metrics history size defined by {@link com.phonemetra.turbo.configuration.TurboSQLConfiguration#getMetricsExpireTime()}
 * and {@link com.phonemetra.turbo.configuration.TurboSQLConfiguration#getMetricsHistorySize()} grid configuration parameters.
 * Generally the larger these configuration parameter values are, the more precise the metrics are.
 * You should tune these values based on the level of accuracy needed vs. the additional memory
 * that would be required for storing metrics.
 * <p>
 * You should also keep in mind that metrics for remote nodes are delayed (usually by the metrics
 * update frequency). So if it is acceptable in your environment, set the metrics update frequency
 * to be more inline with job execution time. Generally, the more often metrics update between nodes
 * are exchanged, the more precise the metrics are. However, you should keep in mind that if
 * metrics update are exchanged too often then it may create unnecessary traffic in the network.
 * Metrics update frequency can be configured via underlying
 * {@link com.phonemetra.turbo.configuration.TurboSQLConfiguration} used in your grid.
 * <p>
 * Here is an example of how probing can be implemented to use
 * number of active and waiting jobs as probing mechanism:
 * <pre name="code" class="java">
 * public class FooBarLoadProbe implements GridAdaptiveLoadProbe {
 *     // Flag indicating whether to use average value or current.
 *     private int useAvg = true;
 *
 *     public FooBarLoadProbe(boolean useAvg) {
 *         this.useAvg = useAvg;
 *     }
 *
 *     // Calculate load based on number of active and waiting jobs.
 *     public double getLoad(ClusterNode node, int jobsSentSinceLastUpdate) {
 *         GridNodeMetrics metrics = node.getMetrics();
 *
 *         if (useAvg) {
 *             double load = metrics.getAverageActiveJobs() + metrics.getAverageWaitingJobs();
 *
 *             if (load > 0) {
 *                 return load;
 *             }
 *         }
 *
 *         return metrics.getCurrentActiveJobs() + metrics.getCurrentWaitingJobs();
 *     }
 * }
 * </pre>
 * <h1 class="header">Which Node Probe To Use</h1>
 * There is no correct answer here. Every single node probe will work better or worse in
 * different environments. CPU load probe (default option) is the safest approach to start
 * with as it simply attempts to utilize every CPU on the grid to the maximum. However, you should
 * experiment with other probes by executing load tests in your environment and observing
 * which probe gives you best performance and load balancing.
 * <p>
 * <h1 class="header">Task Coding Example</h1>
 * If you are using {@link com.phonemetra.turbo.compute.ComputeTaskSplitAdapter} then load balancing logic
 * is transparent to your code and is handled automatically by the adapter.
 * Here is an example of how your task will look:
 * <pre name="code" class="java">
 * public class MyFooBarTask extends ComputeTaskSplitAdapter&lt;Object, Object&gt; {
 *    &#64;Override
 *    protected Collection&lt;? extends ComputeJob&gt; split(int gridSize, Object arg) throws TurboSQLCheckedException {
 *        List&lt;MyFooBarJob&gt; jobs = new ArrayList&lt;MyFooBarJob&gt;(gridSize);
 *
 *        for (int i = 0; i &lt; gridSize; i++) {
 *            jobs.add(new MyFooBarJob(arg));
 *        }
 *
 *        // Node assignment via load balancer
 *        // happens automatically.
 *        return jobs;
 *    }
 *    ...
 * }
 * </pre>
 * If you need more fine-grained control over how some jobs within task get mapped to a node
 * and use affinity load balancing for some other jobs within task, then you should use
 * {@link com.phonemetra.turbo.compute.ComputeTaskAdapter}. Here is an example of how your task will look. Note that in this
 * case we manually inject load balancer and use it to pick the best node. Doing it in
 * such way would allow user to map some jobs manually and for others use load balancer.
 * <pre name="code" class="java">
 * public class MyFooBarTask extends ComputeTaskAdapter&lt;String, String&gt; {
 *    // Inject load balancer.
 *    &#64;LoadBalancerResource
 *    ComputeLoadBalancer balancer;
 *
 *    // Map jobs to grid nodes.
 *    public Map&lt;? extends ComputeJob, ClusterNode&gt; map(List&lt;ClusterNode&gt; subgrid, String arg) throws TurboSQLCheckedException {
 *        Map&lt;MyFooBarJob, ClusterNode&gt; jobs = new HashMap&lt;MyFooBarJob, ClusterNode&gt;(subgrid.size());
 *
 *        // In more complex cases, you can actually do
 *        // more complicated assignments of jobs to nodes.
 *        for (int i = 0; i &lt; subgrid.size(); i++) {
 *            // Pick the next best balanced node for the job.
 *            jobs.put(new MyFooBarJob(arg), balancer.getBalancedNode())
 *        }
 *
 *        return jobs;
 *    }
 *
 *    // Aggregate results into one compound result.
 *    public String reduce(List&lt;ComputeJobResult&gt; results) throws TurboSQLCheckedException {
 *        // For the purpose of this example we simply
 *        // concatenate string representation of every
 *        // job result
 *        StringBuilder buf = new StringBuilder();
 *
 *        for (ComputeJobResult res : results) {
 *            // Append string representation of result
 *            // returned by every job.
 *            buf.append(res.getData().string());
 *        }
 *
 *        return buf.string();
 *    }
 * }
 * </pre>
 * <p>
 * <h1 class="header">Configuration</h1>
 * In order to use this load balancer, you should configure your grid instance
 * to use {@code JobsLoadBalancingSpi} either from Spring XML file or
 * directly. The following configuration parameters are supported:
 * <h2 class="header">Mandatory</h2>
 * This SPI has no mandatory configuration parameters.
 * <h2 class="header">Optional</h2>
 * This SPI has the following optional configuration parameters:
 * <ul>
 * <li>
 *      Adaptive node load probing implementation (see {@link #setLoadProbe(AdaptiveLoadProbe)}).
 *      This configuration parameter supplies a custom algorithm for probing a node's load.
 *      By default, {@link AdaptiveCpuLoadProbe} implementation is used which
 *      takes every node's CPU load and tries to send proportionally more jobs to less loaded nodes.
 * </li>
 * </ul>
 * <p>
 * Below is Java configuration example:
 * <pre name="code" class="java">
 * AdaptiveLoadBalancingSpi spi = new AdaptiveLoadBalancingSpi();
 *
 * // Configure probe to use latest job execution time vs. average.
 * AdaptiveProcessingTimeLoadProbe probe = new AdaptiveProcessingTimeLoadProbe(false);
 *
 * spi.setLoadProbe(probe);
 *
 * TurboSQLConfiguration cfg = new TurboSQLConfiguration();
 *
 * // Override default load balancing SPI.
 * cfg.setLoadBalancingSpi(spi);
 *
 * // Starts grid.
 * G.start(cfg);
 * </pre>
 * Here is how you can configure {@code GridJobsLoadBalancingSpi} using Spring XML configuration:
 * <pre name="code" class="xml">
 * &lt;property name="loadBalancingSpi"&gt;
 *     &lt;bean class="com.phonemetra.turbo.spi.loadBalancing.adaptive.AdaptiveLoadBalancingSpi"&gt;
 *         &lt;property name="loadProbe"&gt;
 *             &lt;bean class="com.phonemetra.turbo.spi.loadBalancing.adaptive.AdaptiveProcessingTimeLoadProbe"&gt;
 *                 &lt;constructor-arg value="false"/&gt;
 *             &lt;/bean&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 * &lt;/property&gt;
 * </pre>
 * <br>
 * For information about Spring framework visit <a href="http://www.springframework.org/">www.springframework.org</a>
 */
@TurboSQLSpiMultipleInstancesSupport(true)
public class AdaptiveLoadBalancingSpi extends TurboSQLSpiAdapter implements LoadBalancingSpi {
    /** Random number generator. */
    private static final Random RAND = new Random();

    /** Grid logger. */
    @LoggerResource
    private TurboSQLLogger log;

    /** */
    private AdaptiveLoadProbe probe = new AdaptiveCpuLoadProbe();

    /** Local event listener to listen to task completion events. */
    private GridLocalEventListener evtLsnr;

    /** Task topologies. First pair value indicates whether or not jobs have been mapped. */
    private ConcurrentMap<TurboSQLUuid, TurboSQLBiTuple<Boolean, WeightedTopology>> taskTops =
        new ConcurrentHashMap<>();

    /** */
    private final Map<UUID, AtomicInteger> nodeJobs = new HashMap<>();

    /** */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * Gets text description of current load probing implementation used.
     *
     * @return Text description of current load probing implementation used.
     */
    public String getLoadProbeFormatted() {
        return probe.toString();
    }

    /**
     * Sets implementation of node load probe. By default {@link AdaptiveProcessingTimeLoadProbe}
     * is used which proportionally distributes load based on the average job execution
     * time on every node.
     *
     * @param probe Implementation of node load probe
     * @return {@code this} for chaining.
     */
    @TurboSQLSpiConfiguration(optional = true)
    public AdaptiveLoadBalancingSpi setLoadProbe(AdaptiveLoadProbe probe) {
        A.ensure(probe != null, "probe != null");

        this.probe = probe;

        return this;
    }

    /** {@inheritDoc} */
    @Override public void spiStart(@Nullable String turboSQLInstanceName) throws TurboSQLSpiException {
        startStopwatch();

        assertParameter(probe != null, "loadProbe != null");

        if (log.isDebugEnabled())
            log.debug(configInfo("loadProbe", probe));

        registerMBean(turboSQLInstanceName, new AdaptiveLoadBalancingSpiMBeanImpl(this),
            AdaptiveLoadBalancingSpiMBean.class);

        // Ack ok start.
        if (log.isDebugEnabled())
            log.debug(startInfo());
    }

    /** {@inheritDoc} */
    @Override public void spiStop() throws TurboSQLSpiException {
        rwLock.writeLock().lock();

        try {
            nodeJobs.clear();
        }
        finally {
            rwLock.writeLock().unlock();
        }

        unregisterMBean();

        // Ack ok stop.
        if (log.isDebugEnabled())
            log.debug(stopInfo());
    }

    /** {@inheritDoc} */
    @Override protected void onContextInitialized0(TurboSQLSpiContext spiCtx) throws TurboSQLSpiException {
        getSpiContext().addLocalEventListener(evtLsnr = new GridLocalEventListener() {
            @Override public void onEvent(Event evt) {
                switch (evt.type()) {
                    case EVT_TASK_FINISHED:
                    case EVT_TASK_FAILED: {
                        TaskEvent taskEvt = (TaskEvent)evt;

                        taskTops.remove(taskEvt.taskSessionId());

                        if (log.isDebugEnabled())
                            log.debug("Removed task topology from topology cache for session: " +
                                taskEvt.taskSessionId());

                        break;
                    }

                    case EVT_JOB_MAPPED: {
                        // We should keep topology and use cache in ComputeTask#map() method to
                        // avoid O(n*n/2) complexity, after that we can drop caches.
                        // Here we set mapped property and later cache will be ignored
                        JobEvent jobEvt = (JobEvent)evt;

                        TurboSQLBiTuple<Boolean, WeightedTopology> weightedTop = taskTops.get(jobEvt.taskSessionId());

                        if (weightedTop != null)
                            weightedTop.set1(true);

                        if (log.isDebugEnabled())
                            log.debug("Job has been mapped. Ignore cache for session: " + jobEvt.taskSessionId());

                        break;
                    }

                    case EVT_NODE_METRICS_UPDATED:
                    case EVT_NODE_FAILED:
                    case EVT_NODE_JOINED:
                    case EVT_NODE_LEFT: {
                        DiscoveryEvent discoEvt = (DiscoveryEvent)evt;

                        rwLock.writeLock().lock();

                        try {
                            switch (evt.type()) {
                                case EVT_NODE_JOINED: {
                                    nodeJobs.put(discoEvt.eventNode().id(), new AtomicInteger(0));

                                    break;
                                }

                                case EVT_NODE_LEFT:
                                case EVT_NODE_FAILED: {
                                    nodeJobs.remove(discoEvt.eventNode().id());

                                    break;
                                }

                                case EVT_NODE_METRICS_UPDATED: {
                                    // Reset counter.
                                    nodeJobs.put(discoEvt.eventNode().id(), new AtomicInteger(0));

                                    break;
                                }
                            }
                        }
                        finally {
                            rwLock.writeLock().unlock();
                        }
                    }

                }
            }
        },
            EVT_NODE_METRICS_UPDATED,
            EVT_NODE_FAILED,
            EVT_NODE_JOINED,
            EVT_NODE_LEFT,
            EVT_TASK_FINISHED,
            EVT_TASK_FAILED,
            EVT_JOB_MAPPED
        );

        // Put all known nodes.
        rwLock.writeLock().lock();

        try {
            for (ClusterNode node : getSpiContext().nodes())
                nodeJobs.put(node.id(), new AtomicInteger(0));
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override protected void onContextDestroyed0() {
        if (evtLsnr != null) {
            TurboSQLSpiContext ctx = getSpiContext();

            if (ctx != null)
                ctx.removeLocalEventListener(evtLsnr);
        }
    }

    /** {@inheritDoc} */
    @Override public ClusterNode getBalancedNode(ComputeTaskSession ses, List<ClusterNode> top, ComputeJob job) {
        A.notNull(ses, "ses");
        A.notNull(top, "top");
        A.notNull(job, "job");

        TurboSQLBiTuple<Boolean, WeightedTopology> weightedTop = taskTops.get(ses.getId());

        // Create new cached topology if there is no one. Do not
        // use cached topology after task has been mapped.
        if (weightedTop == null)
            // Called from ComputeTask#map(). Put new topology and false as not mapped yet.
            taskTops.put(ses.getId(), weightedTop = F.t(false, new WeightedTopology(top)));
        // We have topology - check if task has been mapped.
        else if (weightedTop.get1())
            // Do not use cache after ComputeTask#map().
            return new WeightedTopology(top).pickWeightedNode();

        return weightedTop.get2().pickWeightedNode();
    }

    /**
     * Calculates node load based on set probe.
     *
     * @param top List of all nodes.
     * @param node Node to get load for.
     * @return Node load.
     * @throws TurboSQLException If returned load is negative.
     */
    @SuppressWarnings({"TooBroadScope"})
    private double getLoad(Collection<ClusterNode> top, ClusterNode node) throws TurboSQLException {
        assert !F.isEmpty(top);

        int jobsSentSinceLastUpdate = 0;

        rwLock.readLock().lock();

        try {
            AtomicInteger cnt = nodeJobs.get(node.id());

            jobsSentSinceLastUpdate = cnt == null ? 0 : cnt.get();
        }
        finally {
            rwLock.readLock().unlock();
        }

        double load = probe.getLoad(node, jobsSentSinceLastUpdate);

        if (load < 0)
            throw new TurboSQLException("Failed to obtain non-negative load from adaptive load probe: " + load);

        return load;
    }

    /**
     * Holder for weighted topology.
     */
    private class WeightedTopology {
        /** Topology sorted by weight. */
        private final SortedMap<Double, ClusterNode> circle = new TreeMap<>();

        /**
         * @param top Task topology.
         * @throws TurboSQLCheckedException If any load was negative.
         */
        WeightedTopology(List<ClusterNode> top) throws TurboSQLException {
            assert !F.isEmpty(top);

            double totalLoad = 0;

            // We need to cache loads here to avoid calls later as load might be
            // changed between the calls.
            double[] nums = new double[top.size()];

            int zeroCnt = 0;

            // Compute loads.
            for (int i = 0; i < top.size(); i++) {
                double load = getLoad(top, top.get(i));

                nums[i] = load;

                if (load == 0)
                    zeroCnt++;

                totalLoad += load;
            }

            // Take care of zero loads.
            if (zeroCnt > 0) {
                double newTotal = totalLoad;

                int nonZeroCnt = top.size() - zeroCnt;

                for (int i = 0; i < nums.length; i++) {
                    double load = nums[i];

                    if (load == 0) {
                        if (nonZeroCnt > 0)
                            load = totalLoad / nonZeroCnt;

                        if (load == 0)
                            load = 1;

                        nums[i] = load;

                        newTotal += load;
                    }
                }

                totalLoad = newTotal;
            }

            double totalWeight = 0;

            // Calculate weights and total weight.
            for (int i = 0; i < nums.length; i++) {
                assert nums[i] > 0 : "Invalid load: " + nums[i];

                double weight = totalLoad / nums[i];

                // Convert to weight.
                nums[i] = weight;

                totalWeight += weight;
            }

            double weight = 0;

            // Enforce range from 0 to 1.
            for (int i = 0; i < nums.length; i++) {
                weight = i == nums.length - 1 ? 1.0d : weight + nums[i] / totalWeight;

                assert weight < 2 : "Invalid weight: " + weight;

                // Complexity of this put is O(logN).
                circle.put(weight, top.get(i));
            }
        }

        /**
         * Gets weighted node in random fashion.
         *
         * @return Weighted node.
         */
        ClusterNode pickWeightedNode() {
            double weight = RAND.nextDouble();

            SortedMap<Double, ClusterNode> pick = circle.tailMap(weight);

            ClusterNode node = pick.get(pick.firstKey());

            rwLock.readLock().lock();

            try {
                AtomicInteger cnt = nodeJobs.get(node.id());

                if (cnt != null)
                    cnt.incrementAndGet();
            }
            finally {
                rwLock.readLock().unlock();
            }

            return node;
        }
    }

    /** {@inheritDoc} */
    @Override public AdaptiveLoadBalancingSpi setName(String name) {
        super.setName(name);

        return this;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(AdaptiveLoadBalancingSpi.class, this);
    }

    /**
     * MBean implementation for AdaptiveLoadBalancingSpi.
     */
    private class AdaptiveLoadBalancingSpiMBeanImpl extends TurboSQLSpiMBeanAdapter
        implements AdaptiveLoadBalancingSpiMBean {
        /** {@inheritDoc} */
        AdaptiveLoadBalancingSpiMBeanImpl(TurboSQLSpiAdapter spiAdapter) {
            super(spiAdapter);
        }

        /** {@inheritDoc} */
        @Override public String getLoadProbeFormatted() {
            return AdaptiveLoadBalancingSpi.this.getLoadProbeFormatted();
        }
    }
}