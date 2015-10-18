package net.ibaixin.chat.volley.toolbox;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

public class ProgressOutHttpEntity extends HttpEntityWrapper {

	private final ProgressListenerCallback listener;

	public ProgressOutHttpEntity(final HttpEntity entity,
			final ProgressListenerCallback listener) {
		super(entity);
		this.listener = listener;
	}

	public static class CountingOutputStream extends FilterOutputStream {

		private final ProgressListenerCallback listener;
		private long transferred;

		CountingOutputStream(final OutputStream out,
				final ProgressListenerCallback listener) {
			super(out);
			this.listener = listener;
			this.transferred = 0;
		}

		@Override
		public void write(final byte[] b, final int off, final int len)
				throws IOException {
			// NO, double-counting, as super.write(byte[], int, int)
			// delegates to write(int).
			// super.write(b, off, len);
			out.write(b, off, len);
			this.transferred += len;
			this.listener.transferred(this.transferred);
		}

		@Override
		public void write(final int b) throws IOException {
			out.write(b);
			this.transferred++;
			this.listener.transferred(this.transferred);
		}

	}

	@Override
	public void writeTo(final OutputStream out) throws IOException {
		this.wrappedEntity.writeTo(out instanceof CountingOutputStream ? out
				: new CountingOutputStream(out, this.listener));
	}
}