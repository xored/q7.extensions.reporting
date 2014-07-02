package com.xored.q7.reporting.example.renderers;

import com.xored.q7.reporting.Q7Info;
import com.xored.q7.reporting.core.ReportHelper;
import com.xored.q7.reporting.internal.ReportUtils;
import com.xored.sherlock.core.model.sherlock.EclipseStatus;
import com.xored.sherlock.core.model.sherlock.report.Event;
import com.xored.sherlock.core.model.sherlock.report.Node;
import com.xored.sherlock.core.reporting.SimpleReportGenerator;
import com.xored.tesla.core.info.AdvancedInformation;
import com.xored.tesla.core.info.Q7WaitInfoRoot;
import com.xored.tesla.core.utils.AdvancedInformationGenerator;

public class AdvancedInfoPrinter extends SimpleReportGenerator {
	public StringBuilder toString(StringBuilder builder,
			int tabs, org.eclipse.emf.ecore.EObject obj,
			String... ignores) {
		if (obj instanceof AdvancedInformation) {
			String content = new AdvancedInformationGenerator().generateContent((AdvancedInformation) obj);
			builder.append(content);
			return builder;
		}
		return super.toString(builder, tabs, obj, ignores);
	};

	public void printNode(Node infoNode, StringBuilder stream, int tabs, boolean includeWaitDetails) {
		Q7Info q7Info = ReportHelper.getInfo(infoNode);
		appendTabs(stream, tabs);
		if (q7Info != null) {
			switch (q7Info.getType()) {
			case TESTCASE:
				stream.append("Test case " + infoNode.getName());
				break;
			case SCRIPT:
				stream.append("Script " + infoNode.getName());
				break;
			case CONTEXT:
				stream.append("Context " + infoNode.getName());
				break;
			case VERIFICATION:
				stream.append("Verification " + infoNode.getName() + " " + q7Info.getPhase());
				break;
			case ECL_COMMAND:
				stream.append(infoNode.getName());
				break;
			}
			stream.append(" ")
					.append(ReportUtils.formatTime(infoNode
							.getEndTime()
							- infoNode
									.getStartTime()))
					.append("s")
					.append('\n');
		}
		Q7WaitInfoRoot waitInfo = ReportHelper.getWaitInfo(infoNode, false);
		if (waitInfo != null && includeWaitDetails) {
			printWaitInfo(stream, tabs, "", waitInfo);
		}

		for (Node child : infoNode.getChildren()) {
			printNode(child, stream, tabs + 4, includeWaitDetails);
		}
		if (!includeWaitDetails) {
			for (Event child : infoNode.getEvents()) {
				if (child.getData() instanceof EclipseStatus) {
					printStatus(
							(EclipseStatus) child.getData(),
							tabs + 6, stream);
				}
			}
		}
	}
}