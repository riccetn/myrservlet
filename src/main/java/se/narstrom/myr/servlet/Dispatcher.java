package se.narstrom.myr.servlet;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public final class Dispatcher implements RequestDispatcher {

	@Override
	public void forward(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void include(final ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}

}
