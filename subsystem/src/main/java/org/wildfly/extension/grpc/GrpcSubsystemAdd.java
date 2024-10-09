/*
 *  Copyright 2022 Red Hat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wildfly.extension.grpc;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.grpc.deployment.GrpcDependencyProcessor;
import org.wildfly.extension.grpc.deployment.GrpcDeploymentProcessor;

import io.grpc.netty.NettyServerBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;

class GrpcSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static GrpcSubsystemAdd INSTANCE = new GrpcSubsystemAdd();

    public GrpcSubsystemAdd() {
        super(GrpcSubsystemDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        // Initialize the Netty logger factory
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        // GrpcServerService.configure(operation, context);

        final String serverHost = GrpcSubsystemDefinition.GRPC_SERVER_HOST.resolveModelAttribute(context, model)
                .asString();
        final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();
        final int serverPort = GrpcSubsystemDefinition.GRPC_SERVER_PORT.resolveModelAttribute(context, model).asInt();
        NettyServerBuilder serverBuilder = NettyServerBuilder.forAddress(new InetSocketAddress(serverHost, serverPort));
        serverBuilder.fallbackHandlerRegistry(handlerRegistry);

        if (isDefined(GrpcSubsystemDefinition.GRPC_FLOW_CONTROL_WINDOW, model)) {
            serverBuilder
                    .flowControlWindow(GrpcSubsystemDefinition.GRPC_FLOW_CONTROL_WINDOW.resolveModelAttribute(context, model)
                            .asInt());
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_HANDSHAKE_TIMEOUT, model)) {
            serverBuilder.handshakeTimeout(GrpcSubsystemDefinition.GRPC_HANDSHAKE_TIMEOUT.resolveModelAttribute(context, model)
                    .asInt(), TimeUnit.SECONDS);
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_INITIAL_FLOW_CONTROL_WINDOW, model)) {
            serverBuilder.initialFlowControlWindow(
                    GrpcSubsystemDefinition.GRPC_INITIAL_FLOW_CONTROL_WINDOW.resolveModelAttribute(context, model)
                            .asInt());
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIME, model)) {
            serverBuilder.keepAliveTime(GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIME.resolveModelAttribute(context, model)
                    .asLong(), TimeUnit.SECONDS);
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIMEOUT, model)) {
            serverBuilder.keepAliveTimeout(GrpcSubsystemDefinition.GRPC_KEEP_ALIVE_TIMEOUT.resolveModelAttribute(context, model)
                    .asLong(), TimeUnit.SECONDS);
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION, model)) {
            serverBuilder.maxConcurrentCallsPerConnection(
                    GrpcSubsystemDefinition.GRPC_MAX_CONCURRENT_CALLS_PER_CONNECTION.resolveModelAttribute(context, model)
                            .asInt());
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE, model)) {
            serverBuilder.maxConnectionAge(GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE.resolveModelAttribute(context, model)
                    .asLong(), TimeUnit.SECONDS);
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE_GRACE, model)) {
            serverBuilder.maxConnectionAgeGrace(
                    GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_AGE_GRACE.resolveModelAttribute(context, model)
                            .asLong(),
                    TimeUnit.SECONDS);
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_IDLE, model)) {
            serverBuilder
                    .maxConnectionIdle(GrpcSubsystemDefinition.GRPC_MAX_CONNECTION_IDLE.resolveModelAttribute(context, model)
                            .asLong(), TimeUnit.SECONDS);
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE, model)) {
            serverBuilder.maxInboundMessageSize(
                    GrpcSubsystemDefinition.GRPC_MAX_INBOUND_MESSAGE_SIZE.resolveModelAttribute(context, model)
                            .asInt());
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE, model)) {
            serverBuilder.maxInboundMetadataSize(
                    GrpcSubsystemDefinition.GRPC_MAX_INBOUND_METADATA_SIZE.resolveModelAttribute(context, model)
                            .asInt());
        }

        if (isDefined(GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_TIME, model)) {
            serverBuilder.permitKeepAliveTime(
                    GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_TIME.resolveModelAttribute(context, model)
                            .asLong(),
                    TimeUnit.SECONDS);
        }

        serverBuilder.permitKeepAliveWithoutCalls(
                GrpcSubsystemDefinition.GRPC_PERMIT_KEEP_ALIVE_WITHOUT_CALLS.resolveModelAttribute(context, model)
                        .asBoolean());

        final CapabilityServiceTarget target = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<?> builder = target.addCapability(GrpcSubsystemDefinition.SERVER_CAPABILITY);
        final ServerConfiguration configuration = new ServerConfiguration(serverHost);

        configuration.setProtocolProvider(GrpcSubsystemDefinition.GRPC_PROTOCOL_PROVIDER.resolveModelAttribute(context, model)
                .asStringOrNull())
                .setSessionCacheSize(GrpcSubsystemDefinition.GRPC_SESSION_CACHE_SIZE.resolveModelAttribute(context, model)
                        .asLongOrNull())
                .setSessionTimeout(GrpcSubsystemDefinition.GRPC_SESSION_TIMEOUT.resolveModelAttribute(context, model)
                        .asLongOrNull())
                .setShutdownTimeout(GrpcSubsystemDefinition.GRPC_SHUTDOWN_TIMEOUT.resolveModelAttribute(context, model)
                        .asInt())
                .setStartTls(GrpcSubsystemDefinition.GRPC_START_TLS.resolveModelAttribute(context, model)
                        .asBoolean());

        if (isDefined(GrpcSubsystemDefinition.GRPC_SSL_CONTEXT_NAME, model)) {
            configuration.setSslContext(builder.requiresCapability(Capabilities.SSL_CONTEXT_CAPABILITY, SSLContext.class,
                    GrpcSubsystemDefinition.GRPC_SSL_CONTEXT_NAME.resolveModelAttribute(context, model).asString()));
        }

        final Consumer<GrpcServerService> provides = builder.provides(GrpcSubsystemDefinition.SERVER_CAPABILITY);

        final GrpcServerService service = new GrpcServerService(serverBuilder, handlerRegistry, provides,
                Services.requireServerExecutor(builder), configuration);

        builder.setInstance(service)
                .install();

        context.addStep(new AbstractDeploymentChainStep() {
            public void execute(DeploymentProcessorTarget processorTarget) {
                // TODO What phases and priorities should I use?
                int DEPENDENCIES_PRIORITY = 6304;
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES,
                        DEPENDENCIES_PRIORITY, new GrpcDependencyProcessor());

                int DEPLOYMENT_PRIORITY = 6305;
                processorTarget.addDeploymentProcessor(GrpcExtension.SUBSYSTEM_NAME, Phase.POST_MODULE,
                        DEPLOYMENT_PRIORITY, new GrpcDeploymentProcessor(service));
            }
        }, OperationContext.Stage.RUNTIME);

    }

    private static boolean isDefined(final AttributeDefinition def, final ModelNode model) {
        return model.hasDefined(def.getName());
    }
}
