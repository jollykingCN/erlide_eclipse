package org.erlide.runtime.backend.events;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.erlide.jinterface.JInterfaceFactory;
import org.erlide.jinterface.rpc.RpcUtil;
import org.erlide.runtime.backend.Backend;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangInt;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;

public class RpcHandler extends EventHandler {
	private Backend fBackend;

	public RpcHandler(Backend backend) {
		fBackend = backend;
	}

	@Override
	protected void doHandleMsg(OtpErlangObject msg) throws Exception {
		OtpErlangTuple t = (OtpErlangTuple) msg;
		// debug("-- RPC: " + msg);
		OtpErlangAtom kind = (OtpErlangAtom) t.elementAt(0);
		final OtpErlangObject receiver = t.elementAt(1);
		final OtpErlangObject target = t.elementAt(2);
		if ("call".equals(kind.atomValue())) {
			final OtpErlangList args = buildArgs(t.elementAt(3));
			final OtpErlangPid from = (OtpErlangPid) t.elementAt(4);
			executeRpc(new Runnable() {
				public void run() {
					OtpErlangObject result = RpcUtil.execute(receiver, target,
							args.elements());
					rpcReply(from, result);
				}
			});

		} else if ("uicall".equals(kind.atomValue())) {
			final OtpErlangPid from = (OtpErlangPid) t.elementAt(1);
			final OtpErlangList args = buildArgs(t.elementAt(4));
			// TODO how to mark this as executable in UI thread?
			executeRpc(new Runnable() {
				public void run() {
					OtpErlangObject result = RpcUtil.execute(receiver, target,
							args.elements());
					rpcReply(from, result);
				}
			});

		} else if ("cast".equals(kind.atomValue())) {
			final OtpErlangList args = buildArgs(t.elementAt(3));
			executeRpc(new Runnable() {
				public void run() {
					RpcUtil.execute(receiver, target, args.elements());
				}
			});
		}
	}

	private static OtpErlangList buildArgs(OtpErlangObject a) throws Exception {
		final OtpErlangList args;
		if (a instanceof OtpErlangList) {
			args = (OtpErlangList) a;
		} else if (a instanceof OtpErlangString) {
			String ss = ((OtpErlangString) a).stringValue();
			byte[] bytes = ss.getBytes();
			OtpErlangObject[] str = new OtpErlangObject[ss.length()];
			for (int i = 0; i < ss.length(); i++) {
				str[i] = new OtpErlangInt(bytes[i]);
			}
			args = new OtpErlangList(str);
		} else {
			throw new Exception("bad RPC argument list: " + a);
		}
		return args;
	}

	public void rpcReply(final OtpErlangPid from, final OtpErlangObject result) {
		fBackend.send(from, JInterfaceFactory.mkTuple(
				new OtpErlangAtom("reply"), result));
	}

	public void executeRpc(final Runnable runnable) {
		final Job job = new Job("rpc") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				runnable.run();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.SHORT);
		job.schedule();
	}

}