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
package org.apache.catalina.authenticator.jaspic;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Request;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Security valve which implements JASPIC authentication
 * @author Fjodor Vershinin
 *
 */
public class JaspicAuthenticator extends AuthenticatorBase {

    private static final Log log = LogFactory.getLog(JaspicAuthenticator.class);

    private static final String AUTH_TYPE = "JASPIC";
    private static final String MESSAGE_LAYER = "HttpServlet";

    private Subject serviceSubject;

    @SuppressWarnings("rawtypes")
    private Map authProperties = null;

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();
        serviceSubject = new Subject();
    }

    @Override
    public boolean authenticate(Request request, HttpServletResponse response) throws IOException {
        MessageInfo messageInfo = new MessageInfoImpl(request, response, true);
        JaspicCallbackHandler callbackHandler = getJaspicCallbackHandler();

        AuthConfigFactory factory = AuthConfigFactory.getFactory();
        String appContext = getAppContextId(request);

        AuthConfigProvider configProvider = factory.getConfigProvider(MESSAGE_LAYER, appContext,
                null);
        if (configProvider == null) {
            handleUnauthorizedRequest(response, null);
            return false;
        }

        AuthStatus authStatus;
        try {
            ServerAuthConfig authConfig = configProvider.getServerAuthConfig(MESSAGE_LAYER,
                    appContext, callbackHandler);
            String messageAuthContextId = authConfig.getAuthContextID(messageInfo);
            ServerAuthContext authContext = authConfig.getAuthContext(messageAuthContextId,
                    serviceSubject, authProperties);
            authStatus = authContext.validateRequest(messageInfo, new Subject(), serviceSubject);
        } catch (AuthException e) {
            handleUnauthorizedRequest(response, e);
            return false;
        }

        if (authStatus == AuthStatus.SUCCESS) {
            Principal principal = callbackHandler.getPrincipal();
            if (principal != null) {
                register(request, response, principal, AUTH_TYPE, null, null);
            }
            return true;
        }
        return false;
    }

    @Override
    public void login(String userName, String password, Request request) throws ServletException {
        throw new IllegalStateException("not implemented yet!");
    }

    @Override
    public void logout(Request request) {
        throw new IllegalStateException("not implemented yet!");
    }

    private void handleUnauthorizedRequest(HttpServletResponse response, AuthException e)
            throws IOException {
        log.error(sm.getString("authenticator.unauthorized"), e);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                sm.getString("authenticator.unauthorized"));
    }

    private String getAppContextId(Request request) {
        return request.getServletContext().getVirtualServerName() + " " + request.getContextPath();
    }

    private JaspicCallbackHandler getJaspicCallbackHandler() {
        return new JaspicCallbackHandler(container.getRealm());
    }

    @Override
    protected String getAuthMethod() {
        return AUTH_TYPE;
    }
}
