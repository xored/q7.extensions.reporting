package com.xored.q7.reporting.example.renderers;

import java.util.Iterator;

import com.xored.q7.reporting.Q7Info;
import com.xored.q7.reporting.Q7Statistics;
import com.xored.q7.reporting.ReportingFactory;
import com.xored.q7.reporting.core.IQ7ReportConstants;
import com.xored.sherlock.core.model.sherlock.report.Node;
import com.xored.sherlock.core.model.sherlock.report.Report;

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
}
