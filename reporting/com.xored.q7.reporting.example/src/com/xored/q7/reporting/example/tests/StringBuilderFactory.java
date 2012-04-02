package com.xored.q7.reporting.example.tests;

import java.io.IOException;
import java.io.OutputStream;

import com.xored.q7.reporting.core.IReportRenderer.IContentFactory;

public class StringBuilderFactory implements IContentFactory {
	final StringBuilder builder;

	public StringBuilderFactory(StringBuilder builder) {
		this.builder = builder;
	}

	public OutputStream createFileStream(String fname) {
		builder.append("\n:New file:" + fname + "\n");
		return new OutputStream() {
			@Override
			public void write(int arg0) throws IOException {
				builder.append((char) arg0);
			}
		};
	}

	public IContentFactory createFolder(String name) {
		return this;
	}

	public boolean isFileExist(String fname) {
		return false;
	}
}
