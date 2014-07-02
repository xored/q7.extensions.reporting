package com.xored.q7.reporting.example.renderers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;

import com.xored.q7.reporting.Q7Info;
import com.xored.q7.reporting.Q7Statistics;
import com.xored.q7.reporting.ResultStatus;
import com.xored.q7.reporting.core.IQ7ReportConstants;
import com.xored.q7.reporting.core.IReportRenderer;
import com.xored.q7.reporting.core.Q7ReportIterator;
import com.xored.q7.reporting.example.internal.SampleReportingPlugin;
import com.xored.q7.reporting.example.renderers.CustomAssertion.State;
import com.xored.q7.reporting.internal.ReportUtils;
import com.xored.sherlock.core.model.sherlock.report.Node;
import com.xored.sherlock.core.model.sherlock.report.Report;
import com.xored.sherlock.core.model.sherlock.report.Screenshot;
import com.xored.sherlock.core.model.sherlock.report.Snaphot;

public class SampleReportRenderer implements IReportRenderer {

	public IStatus generateReport(IContentFactory factory, String reportName,
			Q7ReportIterator reportIterator) {

		try {
			render(factory, new OutputStreamWriter(factory.createFileStream(reportName + ".html"), "UTF-8"),
					reportIterator, reportName).close();
		} catch (IOException e) {
			return SampleReportingPlugin.createErr("IO Exception", e);
		} catch (CoreException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	private Writer render(IContentFactory factory, Writer writer, Q7ReportIterator iterator, String reportName)
			throws IOException, CoreException {
		HtmlWriter hw = new HtmlWriter(writer);
		hw.append(getContent("header.html"));
		appendStats(hw, ReportUtils.calculateStatistics(iterator), reportName);
		appendSummary(hw, iterator);
		appendDetails(hw, iterator, factory.createFolder("images"));
		hw.append(getContent("footer.html"));
		return writer;
	}

	private HtmlWriter appendStats(HtmlWriter w, Q7Statistics stats, String reportName) throws IOException {
		return w.format("<h1>Q7 Execution Report &ndash; %s</h1>", reportName)
				.openTag(TABLE)
				.openTag(TR)
				.th("Total tests")
				.td(stats.getTotal())
				.closeTag(TR)
				.openTag(TR)
				.th("Failed tests")
				.td(stats.getFailed())
				.closeTag(TR)
				.openTag(TR)
				.th("Execution Time")
				.td(ReportUtils.formatTime(stats.getTime()) + "s")
				.closeTag(TR)
				.closeTag(TABLE);
	}

	private HtmlWriter appendSummary(HtmlWriter w, Q7ReportIterator iterator) throws IOException {
		w.h2("Summary")
				.openTag(OL);

		Iterator<Report> i = iterator.iterator();
		while (i.hasNext()) {
			Report report = i.next();
			Node root = report.getRoot();
			String testName = root.getName();
			Q7Info info = (Q7Info) root.getProperties().get(IQ7ReportConstants.ROOT);
			boolean failed = info.getResult() != ResultStatus.PASS;
			w.openTag(LI);
			if (failed) {
				w.openTag(A, "href", "#" + info.getId(), "class", info.getResult().name().toLowerCase());
			} else {
				w.openTag("span", "class", info.getResult().name().toLowerCase());
			}
			w.append(markFromResult(info.getResult()))
					.append(" ")
					.escape(testName)
					.closeTag(failed ? A : "span");

			CustomAssertion[] assertions = CustomAssertion.findAssertions(root);
			if (assertions.length != 0) {
				w.openTag(UL);
				for (CustomAssertion assertion : assertions) {
					w.openTag(LI, "class", assertion.state.name().toLowerCase())
							.append(formatState(assertion.state))
							.append(" ")
							.escape(assertion.message)
							.closeTag(LI);
				}
				w.closeTag(UL);
			}
			w.closeTag(LI);

		}

		return w.closeTag(OL);
	}

	private HtmlWriter appendDetails(HtmlWriter w, Q7ReportIterator iterator, IContentFactory images)
			throws IOException, CoreException {
		w.h2("Details");

		Iterator<Report> i = iterator.iterator();

		while (i.hasNext()) {
			Report report = i.next();
			Node root = report.getRoot();
			Q7Info info = (Q7Info) root.getProperties().get(IQ7ReportConstants.ROOT);
			if (info.getResult() == ResultStatus.PASS) {
				continue;
			}
			w.openTag(A, "id", info.getId())
					.closeTag(A)
					.openTag(H3, "class", info.getResult().name().toLowerCase())
					.append(" ")
					.escape(root.getName())
					.closeTag(H3);

			appendFailureInfo(w, root, info, images);
			appendAdvancedDetails(w, report, info);
		}

		return w;
	}

	private static Screenshot[] findImages(Node node) {
		List<Screenshot> result = new ArrayList<Screenshot>();
		findImages(node, result);
		return result.toArray(new Screenshot[result.size()]);
	}

	private static void findImages(Node node, List<Screenshot> screenshots) {
		EList<Snaphot> list = node.getSnapshots();
		for (Snaphot snaphot : list) {
			if (snaphot.getData() instanceof Screenshot) {
				screenshots.add((Screenshot) snaphot.getData());
			}
		}
		EList<Node> children = node.getChildren();
		for (Node node2 : children) {
			findImages(node2, screenshots);
		}
	}

	private static HtmlWriter appendFailureInfo(HtmlWriter w, Node root, Q7Info info, IContentFactory images)
			throws IOException, CoreException {
		w.h4("Test Information");
		w.openTag(TABLE)
				.openTag(TR).th("Failure reason").openTag(TD).pre(ReportUtils.getFailMessage(root)).closeTag(TD)
				.closeTag(TR)
				.openTag(TR).th("Tags").td(info.getTags()).closeTag(TR)
				.openTag(TR).th("Description").openTag(TD).pre(info.getDescription()).closeTag(TD).closeTag(TR)
				.openTag(TR).th("Time Elapsed").td(ReportUtils.formatTime(root.getEndTime() - root.getStartTime()))
				.closeTag(TR);

		Screenshot[] screenshots = findImages(root);

		if (screenshots.length == 0) {
			return w.closeTag(TABLE);
		}

		w.openTag(TR).th("Screenshots").openTag(TD);
		for (int i = 0; i < screenshots.length; i++) {
			Screenshot shot = screenshots[i];
			String filename = String.format("%s_%d.%s", info.getId(), i, shot.getKind().name().toLowerCase());
			OutputStream stream = images.createFileStream(filename);
			stream.write(shot.getData());
			stream.close();

			w.openTag(A, "href", "images/" + filename).openTag("img", "src", "images/" + filename, "alt",
					shot.getMessage(), "height", "80", "width", "80").closeTag("img").closeTag("a");
			// <a href="${shot}"> <img src="${shot}" alt="${shot}" height="80" width="80" /> </a>
		}
		w.closeTag(TD).closeTag(TR).closeTag(TABLE);

		return w;
	}

	private static HtmlWriter appendAdvancedDetails(HtmlWriter w, Report report, Q7Info info) throws IOException {
		w.h4("Advanced details");
		w.pre(new AdvancedInfoPrinter().generateContent(report));
		return w;
	}

	private static String markFromResult(ResultStatus status) {
		switch (status) {
		case FAIL:
			return CROSS_MARK;
		case PASS:
			return CHECK_MARK;
		case SKIPPED:
			return QUESTION_MARK;
		default:
			return EXCLAMATION_MARK;
		}
	}

	private static String formatState(State state) {
		switch (state) {
		case FAIL:
			return CROSS_MARK;
		case PASS:
			return CHECK_MARK;
		case SKIP:
			return QUESTION_MARK;
		default:
			return EXCLAMATION_MARK;
		}
	}

	private static final String CHECK_MARK = "&#x2713;";
	private static final String CROSS_MARK = "&#x2717;";
	private static final String QUESTION_MARK = "&#x3f;";
	private static final String EXCLAMATION_MARK = "&#x21;";

	public String[] getGeneratedFileNames(String reportName) {
		Set<String> fileNames = new HashSet<String>();
		fileNames.add(reportName + ".html");
		fileNames.add("images");
		return fileNames.toArray(new String[fileNames.size()]);
	}

	private static final String TH = "th";
	private static final String TD = "td";
	private static final String TR = "tr";
	private static final String TABLE = "table";
	private static final String H2 = "h2";
	private static final String H3 = "h3";
	private static final String H4 = "h4";
	private static final String PRE = "PRE";
	private static final String OL = "ol";
	private static final String UL = "ul";
	private static final String LI = "li";
	private static final String A = "a";

	private static class HtmlWriter {
		private Writer w;

		public HtmlWriter(Writer w) {
			this.w = w;
		}

		public HtmlWriter th(String text) throws IOException {
			return tag(TH, text);
		}

		public HtmlWriter td(String text) throws IOException {
			return tag(TD, text);
		}

		public HtmlWriter h2(String text) throws IOException {
			return tag(H2, text);
		}

		public HtmlWriter h4(String text) throws IOException {
			return tag(H4, text);
		}

		public HtmlWriter td(int num) throws IOException {
			return tag(TD, Integer.toString(num));
		}

		public HtmlWriter pre(String text) throws IOException {
			return tag(PRE, text);
		}

		public HtmlWriter tag(String tagName, String text) throws IOException {
			return format("<%s>%s</%1$s>", tagName, escapeString(text == null ? "" : text));
		}

		public HtmlWriter openTag(String tagName, String... attributes) throws IOException {
			w.append("<").append(tagName);
			if (attributes.length > 0) {
				w.append(" ");
				for (int i = 0; i < attributes.length / 2; i++) {
					format("%s = '%s'", attributes[i * 2], attributes[i * 2 + 1]);
				}
			}
			w.append(">");
			return this;
		}

		public HtmlWriter closeTag(String tagName) throws IOException {
			return format("</%s>", tagName);
		}

		public HtmlWriter format(String format, Object... args) throws IOException {
			w.append(String.format(format, args));
			return this;
		}

		public HtmlWriter append(CharSequence cs) throws IOException {
			w.append(cs);
			return this;
		}

		public HtmlWriter escape(String s) throws IOException {
			return append(escapeString(s));
		}

		public String escapeString(String s) {
			return s.replace("&", "&amp;").replace("\\", "&#39;").replace("\"", "&quot;")
					.replace("<", "&lt;").replace(">", "&gt;");
		}

	}

	private static final String getContent(String name) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[8192];
		int read = -1;
		InputStreamReader reader = new InputStreamReader(SampleReportRenderer.class.getResourceAsStream(name),
				"UTF-8");
		while ((read = reader.read(buf)) != -1) {
			sb.append(buf, 0, read);
		}
		return sb.toString();
	}

}
