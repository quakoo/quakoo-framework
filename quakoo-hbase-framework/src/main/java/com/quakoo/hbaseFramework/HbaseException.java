package com.quakoo.hbaseFramework;

import java.util.List;

import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;

/**
 * Created by 136249 on 2015/3/16.
 */
public class HbaseException extends RuntimeException {
	public HbaseException() {
	}

	public HbaseException(String message) {
		super(message);
	}

	public HbaseException(String message, Throwable cause) {
		super(message, cause);
	}

	public HbaseException(Throwable cause) {
		super(cause);
	}

	public HbaseException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	@Override
	public void printStackTrace() {
		super.printStackTrace();
		if (super.getCause() != null
				&& (super.getCause() instanceof RetriesExhaustedWithDetailsException)) {
			RetriesExhaustedWithDetailsException detailsException = (RetriesExhaustedWithDetailsException) super
					.getCause();
			List<Throwable> throwables = detailsException.getCauses();
			if (throwables != null) {
				for (Throwable t : throwables) {
					t.printStackTrace();
				}
			}
		}
	}

}
