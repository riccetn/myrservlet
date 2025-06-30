package se.narstrom.myr.servlet.async;

public enum AsyncState {
	DISPATCHING, DISPATCHED, ASYNC_STARTED, ASYNC_WAIT, REDISPATCHING, COMPLETING, COMPLATED
}
