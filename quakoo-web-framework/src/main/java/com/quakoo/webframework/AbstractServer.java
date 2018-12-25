package com.quakoo.webframework;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liyongbiao
 */
public abstract class AbstractServer {

    protected Server server;
    protected final Config config = new Config();

    public AbstractServer(String[] args) {

    }

    public abstract void init(Config config, String[] args);


	public void run(String[] anArgs) throws Exception {
		init(config, anArgs);
		start();
		ThreadMonitor.init(server, config.getMinWorkNumFactor(),
				config.getMaxQueueSizeFactor(), config.getCheckTime());
		System.out.println("server start....");
		join();
	}


    public void start() throws Exception {
        server = new Server();
        System.setProperty("DEBUT", "ture");
        server.setThreadPool(createThreadPool());
        server.addConnector(createConnector());
        server.setHandler(createHandlers());
        server.setStopAtShutdown(true);
        server.start();
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public ThreadPool createThreadPool() {
        // for your environment - this is an example only
        QueuedThreadPool _threadPool = new QueuedThreadPool();
        _threadPool.setMinThreads(config.min_thread);
        _threadPool.setMaxThreads(config.max_thread);
        return _threadPool;
    }

    private SelectChannelConnector createConnector() {
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(config.port);
        connector.setMaxIdleTime(config.max_idle_time);
        // connector.setHost(config.ip);
        // connector.setAcceptors(config.acceptorThreads);
        // connector.setForwarded(config.useForwardedHeaders);
        // connector
        // .setLowResourcesMaxIdleTime((int) config.lowResourcesMaxIdleTime
        // .toMilliseconds());
        // connector
        // .setAcceptorPriorityOffset(config.acceptorThreadPriorityOffset);
        // connector.setAcceptQueueSize(Config.acceptQueueSize);
        // connector.setMaxBuffers(config.maxBufferCount);
        // connector.setRequestBufferSize((int) requestBufferSize.toBytes());
        // connector.setRequestHeaderSize((int)
        // requestHeaderBufferSize.toBytes());
        // connector.setResponseBufferSize((int) config.responseBufferSize
        // .toBytes());
        // connector.setResponseHeaderSize((int) config.responseHeaderBufferSize
        // .toBytes());
        // connector.setReuseAddress(config.reuseAddress);
        // connector.setSoLingerTime((int)
        // config.soLingerTime.toMilliseconds());
        // connector.setName("main-jetty");

        return connector;
    }

    private HandlerCollection createHandlers() {

        WebAppContext _ctx = new WebAppContext();
        String serverName = config.getTempDir();
        serverName = serverName == null ? this.getClass().getSimpleName()
                : serverName;
        if (!serverName.endsWith("/")) {
            serverName = serverName + "/";
        }
        System.setProperty("jetty.home", "/work/" + serverName);
        File tempDir = new File("/work/" + serverName);
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        _ctx.setTempDirectory(tempDir);
        _ctx.setMaxFormContentSize(config.getMaxFormContentSize());
        _ctx.setContextPath(config.contextPath);
        _ctx.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed",
                "false");
        // WebAppContext _ctx2 = new WebAppContext();
        // _ctx2.setContextPath("//api");
        // _ctx2.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed",
        // "false");

        if (isRunningInShadedJar()) {
            _ctx.setWar(getShadedWarUrl());
            // _ctx2.setWar(getShadedWarUrl());
        } else {
            _ctx.setWar(config.webapp);
            // _ctx2.setWar(PROJECT_RELATIVE_PATH_TO_WEBAPP);
        }

        List<Handler> _handlers = new ArrayList<Handler>();

        _handlers.add(_ctx);
        // _handlers.add(_ctx2);

        HandlerList _contexts = new HandlerList();
        _contexts.setHandlers(_handlers.toArray(new Handler[1]));

        HandlerCollection _result = new HandlerCollection();
        if (config.log) {
            // 如果是测试服,输出访问日志
            RequestLogHandler _log = new RequestLogHandler();
            _log.setRequestLog(createRequestLog());
            _result.setHandlers(new Handler[]{_contexts, _log});
        } else {
            _result.setHandlers(new Handler[]{_contexts});
        }
        return _result;
    }

    private RequestLog createRequestLog() {
        NCSARequestLog _log = new NCSARequestLog();

        File _logPath = new File(config.logpath);
        _logPath.getParentFile().mkdirs();
        _log.setFilename(_logPath.getPath());
        _log.setRetainDays(7);
        _log.setExtended(false);
        _log.setAppend(true);
        _log.setLogTimeZone("GMT");
        _log.setLogLatency(true);
        return _log;
    }

    // ---------------------------
    // Discover the war path
    // ---------------------------

    protected boolean isRunningInShadedJar() {
        try {
            Class.forName(config.scanclass);
            return false;
        } catch (ClassNotFoundException anExc) {
            return true;
        }
    }

    public URL getResource(String aResource) {
        return Thread.currentThread().getContextClassLoader()
                .getResource(aResource);
    }

    private String getShadedWarUrl() {
        String _urlStr = getResource(config.web_xml).toString();
        // Strip off "WEB-INF/web.xml"
        System.out.println("[AbstractServer.getShadedWarUrl]:url=" + _urlStr);
        return _urlStr.substring(0, _urlStr.length() - 15);
    }
}
