/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans.model.output.support;

import com.googlecode.jmxtrans.exceptions.LifecycleException;
import com.googlecode.jmxtrans.model.OutputWriterAdapter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.model.output.support.pool.WriterPoolable;
import stormpot.LifecycledPool;
import stormpot.Timeout;

import javax.annotation.Nonnull;
import java.io.IOException;

public class WriterPoolOutputWriter<T extends WriterBasedOutputWriter> extends OutputWriterAdapter{

	@Nonnull private final T target;
	@Nonnull private final LifecycledPool<? extends WriterPoolable> writerPool;
	@Nonnull private final Timeout poolClaimTimeout;

	public WriterPoolOutputWriter(@Nonnull T target, @Nonnull LifecycledPool<? extends WriterPoolable> writerPool, @Nonnull Timeout poolClaimTimeout) {
		this.target = target;
		this.writerPool = writerPool;
		this.poolClaimTimeout = poolClaimTimeout;
	}

	@Override
	public void doWrite(Server server, Query query, Iterable<Result> results) throws Exception {
		try {
			WriterPoolable writerPoolable = writerPool.claim(poolClaimTimeout);
			try {
				target.write(writerPoolable.getWriter(), server, query, results);
			} catch (IOException ioe) {
				writerPoolable.invalidate();
				throw ioe;
			} finally {
				writerPoolable.release();
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("Could not get writer from pool, please check if the server is available");
		}
	}

	@Override
	public void stop() throws LifecycleException {
		writerPool.shutdown();
	}

}
