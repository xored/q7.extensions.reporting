package com.xored.q7.reporting.example.renderers;

import java.util.ArrayList;
import java.util.List;

import com.xored.sherlock.core.model.sherlock.report.Event;
import com.xored.sherlock.core.model.sherlock.report.Node;
import com.xored.sherlock.core.model.sherlock.report.TraceData;

public class CustomAssertion {
	public final String message;
	public final State state;

	public CustomAssertion(String message, State state) {
		this.message = message;
		this.state = state;
	}

	private static final String prefix = "Assertion ";

	public static CustomAssertion fromTrace(TraceData trace) {
		String message = trace.getMessage();
		if (message == null || !message.startsWith(prefix) || message.indexOf(':') == -1) {
			return null;
		}

		int split = message.indexOf(':');
		String msg = message.substring(split + 1).trim();
		String status = message.substring(0, split);
		return new CustomAssertion(msg, status.endsWith("Pass") ? State.PASS : State.FAIL);

	}

	public static CustomAssertion[] findAssertions(Node node) {
		ArrayList<CustomAssertion> result = new ArrayList<CustomAssertion>();
		collectAssertions(node, result);
		return result.toArray(new CustomAssertion[result.size()]);
	}

	private static void collectAssertions(Node node, List<CustomAssertion> acc) {
		for (Event e : node.getEvents()) {
			if (!(e.getData() instanceof TraceData)) {
				continue;
			}

			CustomAssertion assertion = fromTrace((TraceData) e.getData());
			if (assertion != null) {
				acc.add(assertion);
			}
		}

		for (Node child : node.getChildren()) {
			collectAssertions(child, acc);
		}
	}

	static enum State {
		PASS, FAIL, SKIP
	}
}
