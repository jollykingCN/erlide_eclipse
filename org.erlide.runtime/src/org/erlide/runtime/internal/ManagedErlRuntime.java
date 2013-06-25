/*******************************************************************************
 * Copyright (c) 2010 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.runtime.internal;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.erlide.runtime.api.RuntimeData;
import org.erlide.util.ErlLogger;
import org.erlide.util.SystemConfiguration;

public class ManagedErlRuntime extends ErlRuntime {

    private Process process;
    private volatile int exitCode;

    public ManagedErlRuntime(final RuntimeData data) {
        super(data);

        addListener(new Listener() {
            @Override
            public void terminated(final State from) {
                ErlLogger.debug("Runtime %s terminated", getNodeName());
                if (exitCode > 0) {
                    // throw new ErlRuntimeException(String.format(
                    // "Runtime %s crashed with code %d", getNodeName(),
                    // exitCode));
                    System.out.println("CRASH_______ " + exitCode);
                }
            }

            @Override
            public void failed(final State from, final Throwable failure) {
                ErlLogger.warn("Runtime %s crashed with code %d",
                        getNodeName(), exitCode);
                if (data.isReportErrors()) {
                    final String msg = reporter.reportRuntimeDown(
                            getNodeName(), getSystemStatus());
                    ErlLogger.error(msg);
                }
            }

            @Override
            public void starting() {
                ErlLogger.debug("Runtime %s starting", getNodeName());
            }

            @Override
            public void running() {
                ErlLogger.debug("Runtime %s running", getNodeName());
            }

            @Override
            public void stopping(final State from) {
                ErlLogger.debug("Runtime %s stopping", getNodeName());
            }
        }, executor());
    }

    @Override
    protected void startUp() throws Exception {
        exitCode = -1;
        process = startRuntimeProcess(data);
        super.startUp();
    }

    @Override
    protected void shutDown() throws Exception {
        super.shutDown();
        process.destroy();
        process = null;
    }

    @Override
    protected void checkNodeStatus() throws Exception {
        super.checkNodeStatus();
        try {
            exitCode = process.exitValue();
        } catch (final IllegalThreadStateException e) {
            exitCode = -1;
        }
        if (exitCode > 0) {
            System.out.println("CRASH!");
            throw new ErlRuntimeException(String.format(
                    "Runtime %s crashed with code %d", getNodeName(), exitCode));
        } else if (exitCode == 0) {
            stopped = true;
        }
    }

    @Override
    public Process getProcess() {
        startAndWait();
        return process;
    }

    private Process startRuntimeProcess(final RuntimeData rtData) {
        final String[] cmds = rtData.getCmdLine();
        final File workingDirectory = new File(rtData.getWorkingDir());

        try {
            ErlLogger.debug("START node :> " + Arrays.toString(cmds) + " *** "
                    + workingDirectory.getCanonicalPath());
        } catch (final IOException e1) {
            ErlLogger.error("START ERROR node :> " + e1.getMessage());
        }

        final ProcessBuilder builder = new ProcessBuilder(cmds);
        builder.directory(workingDirectory);
        setEnvironment(rtData, builder);
        try {
            final Process aProcess = builder.start();
            return aProcess;
        } catch (final IOException e) {
            ErlLogger.error("Could not create runtime: %s",
                    Arrays.toString(cmds));
            ErlLogger.error(e);
            return null;
        }
    }

    private void setEnvironment(final RuntimeData data,
            final ProcessBuilder builder) {
        final Map<String, String> env = builder.environment();
        if (!SystemConfiguration.getInstance().isOnWindows()
                && SystemConfiguration.getInstance().hasSpecialTclLib()) {
            env.put("TCL_LIBRARY", "/usr/share/tcl/tcl8.4/");
        }
        if (data.getEnv() != null) {
            env.putAll(data.getEnv());
        }
    }

}
