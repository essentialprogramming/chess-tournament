package com.undertow.standalone;

import static com.spring.ApplicationContextFactory.getSpringApplicationContext;
import static io.undertow.Handlers.websocket;
import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;

import com.api.controller.LoginServlet;
import com.api.service.WebSocketManager;
import com.authentication.config.ApplicationConfig;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.glassfish.jersey.servlet.ServletContainer;

import com.server.Server;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.StuckThreadDetectionHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import static com.util.cloud.Environment.getProperty;

public final class UndertowServer {

    public final Lock LOCK = new ReentrantLock();

    private final String host;
    private final int port;
    private final String deploymentName;

    private volatile Undertow server;

    private final Multimap<String, WebSocketChannel> userKeysAndChannels= ArrayListMultimap.create();

    public UndertowServer(String host, int port, String deploymentName) {
        this.host = host;
        this.port = port;
        this.deploymentName = deploymentName;
    }

    private static ListenerInfo createContextLoaderListener(WebApplicationContext context) {
        InstanceFactory<ContextLoaderListener> factory = new ImmediateInstanceFactory<>(new ContextLoaderListener(context));
        return new ListenerInfo(ContextLoaderListener.class, factory);
    }

    private HttpHandler bootstrap() throws ServletException {
        final DeploymentInfo servletBuilder = deployment()
                .setClassLoader(Server.class.getClassLoader())
                .setContextPath("/")
                .addListeners(createContextLoaderListener(getSpringApplicationContext()))
                .setResourceManager(new ClassPathResourceManager(Server.class.getClassLoader(), "webapp/resources"))
                .addWelcomePage("index.html")
                .setDeploymentName(deploymentName)

                .addServlets(
                        servlet("jerseyServlet", ServletContainer.class)
                                .addInitParam("javax.ws.rs.Application", com.api.config.ApplicationConfig.class.getName())
                                .addMapping("/api/*")
                                .setLoadOnStartup(1)
                                .setAsyncSupported(true),
                        servlet("authServlet", ServletContainer.class)
                                .addInitParam("javax.ws.rs.Application", ApplicationConfig.class.getName())
                                .addMapping("/api/auth/*")
                                .setLoadOnStartup(1)
                                .setAsyncSupported(true),
                        servlet("loginServlet", LoginServlet.class)
                                .addMapping("/login/*")
                                .setLoadOnStartup(1)
                                .setAsyncSupported(true)
                );

        final DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        //Servlet handler
        final HttpHandler servletHandler = manager.start();

        //Open API resource handler
        final ResourceHandler resourceHandler = new ResourceHandler(new ClassPathResourceManager(Server.class.getClassLoader(), "apidoc"))
                .addWelcomeFiles("index.html")
                .setDirectoryListingEnabled(false);

        final PathHandler pathHandler = Handlers.path()
                .addPrefixPath("/", servletHandler)
                .addPrefixPath("apidoc", resourceHandler)
                .addPrefixPath("/websocket", websocket(
                        (exchange, channel) -> {
                            connect(exchange, channel);
                            channel.getReceiveSetter().set(WebSocketManager.getListener(userKeysAndChannels));
                            channel.resumeReceives();
                        }));

        return pathHandler;
    }

    private void connect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        String[] queryString = exchange.getQueryString().split("userKey=");
        String key = queryString[queryString.length-1];
        userKeysAndChannels.put(key, channel);
    }

    public void start() throws ServletException {

        final HttpHandler httpHandler = bootstrap();
        final StuckThreadDetectionHandler stuck = new StuckThreadDetectionHandler(getProperty("THREAD_EXECUTION_TIME", 700), httpHandler);
        final GracefulShutdownHandler shutdown = Handlers.gracefulShutdown(stuck);

        LOCK.lock();

        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(shutdown)
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .build();

        server.start();
        LOCK.unlock();
    }


    public void stop() {
        server.stop();
        LOCK.unlock();
    }
}
