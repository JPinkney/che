/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.promises.async;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;

/**
 * Helper class, allows prevent accumulation of sequential async tasks.
 *
 * @author Evgen Vidolob
 */
public class Throttler {
//    private Promise activePromise;
//    private Promise queuedPromise;
//    private Task promiseFactory;
    private Promise current = Promises.resolve(null);

    @SuppressWarnings("unchecked")
    public <T> Promise<T> queue(final Task<Promise<T>> promiseFactory) {
//        if (activePromise != null) {
//            this.promiseFactory = promiseFactory;
//
//            if (queuedPromise == null) {
//                queuedPromise = Promises.
//            }
//        }
        return current = current.thenPromise(new Function<Object, Promise>() {
            @Override
            public Promise apply(Object arg) throws FunctionException {
                return promiseFactory.run();
            }
        });
    }
}
