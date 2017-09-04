package org.avaje.docker.commands;

public interface DbCommands {

	/**
	 * Start the container.
	 */
	boolean start();

	/**
	 * Stop the container.
	 */
	void stop();

	/**
	 * Return a description of the starting container.
	 */
	String getStartDescription();

	/**
	 * Return a description of the stopping container.
	 */
	String getStopDescription();

}
