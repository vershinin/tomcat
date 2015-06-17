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
import java.util.Collections;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;

import org.apache.catalina.Realm;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.tomcat.util.res.StringManager;

/**
 * Callback handler which converts callbacks to realm
 * @author Fjodor Vershinin
 *
 */
public class JaspicCallbackHandler implements CallbackHandler {
    protected static final StringManager sm = StringManager.getManager(JaspicCallbackHandler.class);

    private Realm realm;

    private PrincipalGroupCallback principalGroupCallback = new PrincipalGroupCallback();

    public JaspicCallbackHandler(Realm realm) {
        this.realm = realm;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (callbacks == null) {
            return;
        }
        for (Callback callback : callbacks) {
            handleCallback(callback);
        }
    }

    public GenericPrincipal getPrincipal() {
        return principalGroupCallback.getPrincipal();
    }

    private void handleCallback(Callback callback) {

        if (callback instanceof CallerPrincipalCallback) {
            principalGroupCallback.setCallerPrincipalCallback((CallerPrincipalCallback) callback);
        } else if (callback instanceof GroupPrincipalCallback) {
            principalGroupCallback.setCallerPrincipalCallback((GroupPrincipalCallback) callback);
        } else if (callback instanceof PasswordValidationCallback) {
            handlePasswordValidationCallback((PasswordValidationCallback) callback);
        } else {
            throw new IllegalStateException(sm.getString("authenticator.jaspic.unknownCallback", callback.getClass()));
        }
    }

    private void handlePasswordValidationCallback(
            PasswordValidationCallback passwordValidationCallback) {
        Subject subject = passwordValidationCallback.getSubject();

        passwordValidationCallback.setResult(true);
        subject.getPrincipals().add(
                new GenericPrincipal("user", "password", Collections.singletonList("user")));
    }
}
