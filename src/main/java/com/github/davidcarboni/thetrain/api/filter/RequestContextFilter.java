package com.github.davidcarboni.thetrain.api.filter;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.onsdigital.logging.util.RequestLogUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestContextFilter implements Filter {

	@Override
	public boolean filter(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		/**
		 * TEMP FIX: Zebedee does not currently pass the request ID to the train so generate a new one to allow us to
		 * tie requests together.
		 *
		 * Using RequestLogUtil.extractDiagnosticContext as it generates missing request-IDs for MDC as standard.
		 */
		RequestLogUtil.extractDiagnosticContext(httpServletRequest);
		return true;
	}
}
