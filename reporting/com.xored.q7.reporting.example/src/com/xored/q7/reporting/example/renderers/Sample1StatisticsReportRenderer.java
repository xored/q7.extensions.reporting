package com.xored.q7.reporting.example.renderers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.xored.q7.reporting.Q7Info;
import com.xored.q7.reporting.Q7Statistics;
import com.xored.q7.reporting.core.IQ7ReportConstants;
import com.xored.q7.reporting.core.IReportRenderer;
import com.xored.q7.reporting.core.Q7ReportIterator;
import com.xored.sherlock.core.model.sherlock.report.Node;
import com.xored.sherlock.core.model.sherlock.report.Report;

public class Sample1StatisticsReportRenderer implements IReportRenderer {

	public Sample1StatisticsReportRenderer() {
	}

	public IStatus generateReport(IContentFactory factory, String reportName,
			Q7ReportIterator report) {

		OutputStream outputStream = null;
		try {
			outputStream = factory.createFileStream(reportName
					+ ".sample.report.txt");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					outputStream));
			writer.append("Simple report sample:\n");

			// Calculate some statistics
			Q7Statistics statistics = SampleUtil.calculateStatistics(report
					.iterator());
			writer.append("Total test count: " + statistics.getTotal() + "\n");
			writer.append("Failed test count: " + statistics.getFailed() + "\n");
			writer.append("Time to complete tests: " + statistics.getTime()
					+ "\n");

			writer.append("tests:\n");
			report.reset();
			while (report.hasNext()) {
				Report oneReport = report.next();
				if (oneReport == null) {
					break;
				}

				Node localRoot = oneReport.getRoot();
				// Skip nodes without specific Q7 info node stored in
				if (!localRoot.getProperties().containsKey(
						IQ7ReportConstants.ROOT)) {
					continue;
				}

				Q7Info q7info = (Q7Info) localRoot.getProperties().get(
						IQ7ReportConstants.ROOT);
				String resultLine = "";
				String extraMessage = "";
				switch (q7info.getResult()) {
				case FAIL:
				case WARN:
					resultLine += "Failed";
					extraMessage = "\n\t reason: " +  SampleUtil.getFailMessage(localRoot);
					// "\n"
					// + SampleUtil.getDefails(localRoot);
					break;
				case PASS:
					resultLine += "Passed";
					break;
				case SKIPPED:
					resultLine += "Skipped";
					break;
				}
				resultLine += " testcase: " + localRoot.getName();
				resultLine += " execution time: "
						+ (localRoot.getEndTime() - localRoot.getStartTime());
				writer.append(resultLine + extraMessage + "\n");
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (Throwable e) {
				}
			}
		}
		return Status.OK_STATUS;
	}

	public String[] getGeneratedFileNames(String reportName) {
		return new String[] { reportName + ".sample.report.txt" };
	}

}
