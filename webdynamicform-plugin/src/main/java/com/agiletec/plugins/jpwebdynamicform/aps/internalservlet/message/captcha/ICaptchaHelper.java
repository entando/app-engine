package com.agiletec.plugins.jpwebdynamicform.aps.internalservlet.message.captcha;

import jakarta.servlet.ServletRequest;
import java.io.IOException;

public interface ICaptchaHelper {

	public String getPublicKey();

	/**
	 * checks if a user is valid
	 *
	 * @return true if human, false if bot
	 * @throws IOException
	 */
	String getRecaptchaType();

	/**
	 * checks if a user is valid
	 * 
	 * @param request
	 * @return true if human, false if bot
	 * @throws IOException
	 */
	boolean isHuman(ServletRequest request) throws IOException;

	/**
	 * checks if a user is valid
	 *
	 * @param request
	 * @return true if human, false if bot
	 * @throws IOException
	 */
	boolean verifyRecaptchaV2(ServletRequest request) throws IOException;

	/**
	 * checks if a user is valid
	 * 
	 * @param clientRecaptchaResponse
	 * @return true if human, false if bot
	 * @throws IOException
	 */
	boolean verifyRecaptchaV3(String clientRecaptchaResponse) throws IOException;

}