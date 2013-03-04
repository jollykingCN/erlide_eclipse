package org.erlide.backend.events;

import org.erlide.util.ErlLogger;
import org.erlide.util.erlang.Bindings;
import org.erlide.util.erlang.ErlUtils;
import org.osgi.service.event.Event;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangObject;

public class LogEventHandler extends ErlangEventHandler {

    public LogEventHandler(final String backendName) {
        super("log", backendName);
    }

    @Override
    public void handleEvent(final Event event) {
        final OtpErlangObject data = (OtpErlangObject) event
                .getProperty("DATA");
        try {
            final Bindings b = ErlUtils.match("{K:a,M}", data);
            final String kind = ((OtpErlangAtom) b.get("K")).atomValue();
            final OtpErlangObject amsg = b.get("M");
            ErlLogger.debug("%s: %s", kind, ErlUtils.asString(amsg));
        } catch (final Exception e) {
            ErlLogger.error("erroneous log msg: %s", data);
        }
    }
}
