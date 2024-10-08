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
package org.wildfly.feature.pack.grpc;

import jakarta.annotation.Priority;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import messages.HelloRequest;

@Priority(10)
public class TestServerInterceptor10 implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {

        ServerCall.Listener<ReqT> listener = next.startCall(call, requestHeaders);
        return new TestListener<ReqT>(listener);
    }

    static class TestListener<ReqT> extends SimpleForwardingServerCallListener<ReqT> {

        protected TestListener(ServerCall.Listener<ReqT> delegate) {
            super(delegate);
        }

        @Override
        public void onMessage(ReqT message) {
            HelloRequest request = (HelloRequest) message;
            messages.HelloRequest.Builder builder = messages.HelloRequest.newBuilder();
            @SuppressWarnings("unchecked")
            ReqT reqT = (ReqT) builder.setName("!!" + request.getName()).build();
            delegate().onMessage(reqT);
        }
    }
}
