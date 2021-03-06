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

package com.phonemetra.turbo.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.phonemetra.turbo.TurboSQL;
import com.phonemetra.turbo.TurboSQLCluster;
import com.phonemetra.turbo.configuration.TurboSQLConfiguration;
import com.phonemetra.turbo.lang.TurboSQLProductVersion;
import org.jetbrains.annotations.Nullable;

/**
 * Interface representing a single cluster node. Use {@link #attribute(String)} or
 * {@link #metrics()} to get static and dynamic information about cluster nodes.
 * {@code ClusterNode} list, which includes all nodes within task topology, is provided
 * to {@link com.phonemetra.turbo.compute.ComputeTask#map(List, Object)} method.
 * <p>
 * <h1 class="header">Cluster Node Attributes</h1>
 * You can use cluster node attributes to provide static information about a node.
 * This information is initialized once within a cluster, during the node startup, and
 * remains the same throughout the lifetime of a node. Use
 * {@link TurboSQLConfiguration#getUserAttributes()} method to initialize your custom
 * node attributes at startup. Here is an example of how to assign an attribute to a node at startup:
 * <pre name="code" class="xml">
 * &lt;bean class="com.phonemetra.turbo.configuration.TurboSQLConfiguration">
 *     ...
 *     &lt;property name="userAttributes">
 *         &lt;map>
 *             &lt;entry key="worker" value="true"/>
 *         &lt;/map>
 *     &lt;/property>
 *     ...
 * &lt;/bean&gt;
 * </pre>
 * <p>
 * The system adds the following attributes automatically:
 * <ul>
 * <li>{@code {@link System#getProperties()}} - All system properties.</li>
 * <li>{@code {@link System#getenv(String)}} - All environment properties.</li>
 * <li>{@code org.turboSQL.build.ver} - TurboSQL build version.</li>
 * <li>{@code com.phonemetra.turbo.jit.name} - Name of JIT compiler used.</li>
 * <li>{@code com.phonemetra.turbo.net.itf.name} - Name of network interface.</li>
 * <li>{@code com.phonemetra.turbo.user.name} - Operating system user name.</li>
 * <li>{@code com.phonemetra.turbo.turboSQL.name} - TurboSQL name (see {@link TurboSQL#name()}).</li>
 * <li>
 *      {@code spiName.com.phonemetra.turbo.spi.class} - SPI implementation class for every SPI,
 *      where {@code spiName} is the name of the SPI (see {@link com.phonemetra.turbo.spi.TurboSQLSpi#getName()}.
 * </li>
 * <li>
 *      {@code spiName.com.phonemetra.turbo.spi.ver} - SPI version for every SPI,
 *      where {@code spiName} is the name of the SPI (see {@link com.phonemetra.turbo.spi.TurboSQLSpi#getName()}.
 * </li>
 * </ul>
 * <p>
 * Note that all System and Environment properties for all nodes are automatically included
 * into node attributes. This gives you an ability to get any information specified
 * in {@link System#getProperties()} about any node. So for example, in order to print out
 * information about Operating System for all nodes you would do the following:
 * <pre name="code" class="java">
 * for (ClusterNode node : turboSQL.cluster().nodes()) {
 *     System.out.println("Operating system name: " + node.getAttribute("os.name"));
 *     System.out.println("Operating system architecture: " + node.getAttribute("os.arch"));
 *     System.out.println("Operating system version: " + node.getAttribute("os.version"));
 * }
 * </pre>
 * <p>
 * <h1 class="header">Cluster Node Metrics</h1>
 * Cluster node metrics (see {@link #metrics()}) are updated frequently for all nodes
 * and can be used to get dynamic information about a node. The frequency of update
 * is controlled by  {@link com.phonemetra.turbo.configuration.TurboSQLConfiguration#getMetricsUpdateFrequency()} parameter.
 * The metrics data will be updated every {@code 2} seconds by default.
 * <p>
 * Grid node metrics provide information that can frequently change,
 * such as Heap and Non-Heap memory utilization, CPU load, number of active and waiting
 * grid jobs, etc... This information can become useful during job collision resolution or
 * {@link com.phonemetra.turbo.compute.ComputeTask#map(List, Object)} operation when jobs are
 * assigned to remote nodes for execution.
 * <p>
 * Local node metrics are registered as {@code MBean} and can be accessed from
 * any JMX management console. The simplest way is to use standard {@code jconsole}
 * that comes with JDK as it also provides ability to view any node parameter
 * as a graph.
 */
public interface ClusterNode extends BaselineNode {
    /**
     * Gets globally unique node ID. A new ID is generated every time a node restarts.
     *
     * @return Globally unique node ID.
     */
    public UUID id();

    /**
     * Gets consistent globally unique node ID. Unlike {@link #id()} method,
     * this method returns consistent node ID which survives node restarts.
     *
     * @return Consistent globally unique node ID.
     */
    @Override public Object consistentId();

    /**
     * Gets a node attribute. Attributes are assigned to nodes at startup
     * via {@link TurboSQLConfiguration#getUserAttributes()} method.
     * <p>
     * The system adds the following attributes automatically:
     * <ul>
     * <li>{@code {@link System#getProperties()}} - All system properties.</li>
     * <li>{@code {@link System#getenv(String)}} - All environment properties.</li>
     * <li>All attributes defined in {@link com.phonemetra.turbo.internal.TurboSQLNodeAttributes}</li>
     * </ul>
     * <p>
     * Note that attributes cannot be changed at runtime.
     *
     * @param <T> Attribute Type.
     * @param name Attribute name. <b>Note</b> that attribute names starting with
     *      {@code com.phonemetra.turbo} are reserved for internal use.
     * @return Attribute value or {@code null}.
     */
    @Override @Nullable public <T> T attribute(String name);

    /**
     * Gets metrics snapshot for this node. Note that node metrics are constantly updated
     * and provide up to date information about nodes. For example, you can get
     * an idea about CPU load on remote node via {@link ClusterMetrics#getCurrentCpuLoad()}
     * method and use it during {@link com.phonemetra.turbo.compute.ComputeTask#map(List, Object)} or during collision
     * resolution.
     * <p>
     * Node metrics are updated with some delay which is controlled by
     * {@link com.phonemetra.turbo.configuration.TurboSQLConfiguration#getMetricsUpdateFrequency()} parameter.
     * By default the update will happen every {@code 2} seconds.
     *
     * @return Runtime metrics snapshot for this node.
     */
    public ClusterMetrics metrics();

    /**
     * Gets all node attributes. Attributes are assigned to nodes at startup
     * via {@link TurboSQLConfiguration#getUserAttributes()} method.
     * <p>
     * The system adds the following attributes automatically:
     * <ul>
     * <li>{@code {@link System#getProperties()}} - All system properties.</li>
     * <li>{@code {@link System#getenv(String)}} - All environment properties.</li>
     * <li>All attributes defined in {@link com.phonemetra.turbo.internal.TurboSQLNodeAttributes}</li>
     * </ul>
     * <p>
     * Note that attributes cannot be changed at runtime.
     *
     * @return All node attributes.
     */
    @Override public Map<String, Object> attributes();

    /**
     * Gets collection of addresses this node is known by.
     * <p>
     * If {@link TurboSQLConfiguration#getLocalHost()} value isn't {@code null} node will try to use that
     * address for all communications and returned collection will contain only that address.
     * If it is {@code null} then local wildcard address will be used, and TurboSQL
     * will make the best effort to supply all addresses of that node in returned collection.
     *
     * @return Collection of addresses.
     */
    public Collection<String> addresses();

    /**
     * Gets collection of host names this node is known by.
     * <p>
     * If {@link TurboSQLConfiguration#getLocalHost()} value isn't {@code null} node will try to use
     * the host name of that resolved address for all communications and
     * returned collection will contain only that host name.
     * If that host name can not be resolved then ip address returned by method {@link #addresses()} is used.
     * <p>
     * If {@link TurboSQLConfiguration#getLocalHost()} value is {@code null} then local wildcard address will be used,
     * and this method returns host names of all addresses of that node.
     * <p>
     * Note: the loopback address will be omitted in results.
     *
     * @return Collection of host names.
     */
    public Collection<String> hostNames();

    /**
     * Node order within grid topology. Discovery SPIs that support node ordering will
     * assign a proper order to each node and will guarantee that discovery event notifications
     * for new nodes will come in proper order. All other SPIs not supporting ordering
     * may choose to return node startup time here.
     * <p>
     * <b>NOTE</b>: in cases when discovery SPI doesn't support ordering TurboSQL cannot
     * guarantee that orders on all nodes will be unique or chronologically correct.
     * If such guarantee is required - make sure use discovery SPI that provides ordering.
     *
     * @return Node startup order.
     */
    public long order();

    /**
     * Gets node version.
     *
     * @return Node version.
     */
    public TurboSQLProductVersion version();

    /**
     * Tests whether or not this node is a local node.
     *
     * @return {@code True} if this node is a local node, {@code false} otherwise.
     */
    public boolean isLocal();

    /**
     * Tests whether or not this node is a daemon.
     * <p>
     * Daemon nodes are the usual cluster nodes that participate in topology but are not
     * visible on the main APIs, i.e. they are not part of any cluster group. The only
     * way to see daemon nodes is to use {@link TurboSQLCluster#forDaemons()} method.
     * <p>
     * Daemon nodes are used primarily for management and monitoring functionality that
     * is build on TurboSQL and needs to participate in the topology, but should be
     * excluded from the "normal" topology, so that they won't participate in the task execution
     * or data grid operations.
     * <p>
     * Application code should never use daemon nodes.
     *
     * @return {@code True} if this node is a daemon, {@code false} otherwise.
     */
    public boolean isDaemon();

    /**
     * Whether this node is cache client (see {@link TurboSQLConfiguration#isClientMode()}).
     *
     * @return {@code True if client}.
     *
     * @see TurboSQLConfiguration#isClientMode()
     */
    public boolean isClient();
}