package com.ml.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectionHandler implements RejectedExecutionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RejectionHandler.class);

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
        LOG.warn(SessionLogcatAppender.MARKER_INTERNAL, "Could not queue task, some points may not be logged.");
    }
}