package com.xored.q7.reporting.example.renderers;

import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import com.xored.q7.reporting.Q7Info;
import com.xored.q7.reporting.Q7Statistics;
import com.xored.q7.reporting.ReportingFactory;
import com.xored.q7.reporting.ResultStatus;
import com.xored.q7.reporting.core.IQ7ReportConstants;
import com.xored.sherlock.core.model.sherlock.EclipseStatus;
import com.xored.sherlock.core.model.sherlock.report.Event;
import com.xored.sherlock.core.model.sherlock.report.Node;
import com.xored.sherlock.core.model.sherlock.report.Report;
import com.xored.sherlock.core.model.sherlock.report.Snaphot;
import com.xored.sherlock.core.reporting.SimpleReportGenerator;
import com.xored.tesla.core.info.AdvancedInformation;
import com.xored.tesla.core.utils.AdvancedInformationGenerator;

public class SampleUtil {
	public static Q7Statistics calculateStatistics(Iterator<Report> iterator) {
		Q7Statistics statistics = ReportingFactory.eINSTANCE
				.createQ7Statistics();

		long startTime = Long.MAX_VALUE;
		long endTime = Long.MIN_VALUE;
		long totalTime = 0;
		int total = 0;
		int failed = 0;
		int passed = 0;
		int skipped = 0;

		while (iterator.hasNext()) {
			Report report = iterator.next();
			if (report == null) {
				return null;
			}
			total += 1;
			Node localRoot = report.getRoot();
			if (!localRoot.getProperties().containsKey(IQ7ReportConstants.ROOT)) {
				continue;
			}

			Q7Info q7info = (Q7Info) localRoot.getProperties().get(
					IQ7ReportConstants.ROOT);
			switch (q7info.getResult()) {
			case FAIL:
			case WARN:
				failed++;
				break;
			case PASS:
				passed++;
				break;
			case SKIPPED:
				skipped++;
				break;
			}

			startTime = Math.min(startTime, localRoot.getStartTime());
			endTime = Math.max(endTime, localRoot.getEndTime());
			totalTime += (localRoot.getEndTime() - localRoot.getStartTime());
		}

		statistics.setTime((int) totalTime);
		statistics.setTotal(total);
		statistics.setFailed(failed);
		statistics.setPassed(passed);
		statistics.setSkipped(skipped);
		return statistics;
	}

	public static String getFailMessage(Node item) {
		StringBuilder result = new StringBuilder();
		EList<Node> children = item.getChildren();
		for (Node node : children) {
			Q7Info info = (Q7Info) node.getProperties().get(
					IQ7ReportConstants.ROOT);

			if (info != null && info.getResult().equals(ResultStatus.FAIL)) {
				String msg = info.getMessage();
				if (msg != null) {
					result.append(msg).append(" (" + node.getName() + ")")
							.append("\n");
				}
				StringBuilder b = new StringBuilder();
				EList<Node> children2 = node.getChildren();
				for (Node node2 : children2) {
					collectFailures(node2, b);
				}
				if (b.toString().trim().length() > 0) {
					result.append("Caused by:").append(b.toString())
							.append("\n");
				}
			}
		}
		return result.toString();
	}

	public static void collectFailures(Node item, StringBuilder result) {
		Q7Info info = (Q7Info) item.getProperties()
				.get(IQ7ReportConstants.ROOT);
		if (info != null && info.getResult().equals(ResultStatus.FAIL)) {
			String msg = info.getMessage();
			if (msg != null) {
				result.append(msg).append(" [" + item.getName() + "]")
						.append("\n");
			}
		}
		EList<Node> children = item.getChildren();
		for (Node node : children) {
			collectFailures(node, result);
		}
	}

	public static String getDetails(Node item) {
		// Collect and print all snapshots
		StringBuilder builder = new StringBuilder();
		collectDetails(item, builder);
		return builder.toString();
	}

	public static void collectDetails(Node item, StringBuilder result) {
		EList<Snaphot> snapshots = item.getSnapshots();
		for (Snaphot snaphot : snapshots) {
			EObject data = snaphot.getData();
			if (data != null) {
				if (data instanceof AdvancedInformation) {
					result.append(new AdvancedInformationGenerator()
							.generateContent((AdvancedInformation) data));
					result.append("\n");
				} else {
					new SimpleReportGenerator().toString(result, 2, data);
					result.append("\n");
				}
			}
		}
		EList<Event> events = item.getEvents();
		for (Event event : events) {
			if (event.getData() instanceof EclipseStatus) {
				EclipseStatus data = (EclipseStatus) event.getData();
				new SimpleReportGenerator().toString(result, 1, data);
				result.append("\n");
			}
		}
		EList<Node> children = item.getChildren();
		for (Node node : children) {
			collectDetails(node, result);
		}
	}
}
