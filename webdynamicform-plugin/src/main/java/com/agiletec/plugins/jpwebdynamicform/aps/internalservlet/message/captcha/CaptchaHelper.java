package com.agiletec.plugins.jpwebdynamicform.aps.internalservlet.message.captcha;

import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.plugins.jpwebdynamicform.aps.system.services.JpwebdynamicformSystemConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import jakarta.servlet.ServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CaptchaHelper implements ICaptchaHelper {

	private static final Logger logger = LoggerFactory.getLogger(CaptchaHelper.class);

	private static final String RECAPTCHA_SERVICE_URL = "https://www.google.com/recaptcha/api/siteverify";
	public static final String CLIENT_CAPTCHA_TOKEN_PARAM_NAME = "recaptchaToken";

	private ConfigInterface _configManager;

	protected ConfigInterface getConfigManager() {
		return _configManager;
	}

	public void setConfigManager(ConfigInterface configManager) {
		this._configManager = configManager;
	}

	protected String getSecretKey() {
		return this.getConfigManager().getParam(JpwebdynamicformSystemConstants.RECAPTCHA_PRIVATEKEY_PARAM_NAME);
	}

	@Override
	public String getPublicKey() {
		return this.getConfigManager().getParam(JpwebdynamicformSystemConstants.RECAPTCHA_PUBLICKEY_PARAM_NAME);
	}

	@Override
	public String getRecaptchaType() {
		String version = this.getConfigManager().getParam(JpwebdynamicformSystemConstants.RECAPTCHA_TYPE_PARAM_NAME);
		if ("v3".equalsIgnoreCase(version)) {
			return "v3";
		} else {
			return "v2";
		}
	}

	/**
	 * checks if a user is valid
	 * 
	 * @param request
	 * @return true if human, false if bot
	 * @throws IOException
	 */
	@Override
	public boolean isHuman(ServletRequest request) throws IOException {
		String protocol = this.getRecaptchaType();
		if ("v2".equalsIgnoreCase(protocol)) {
			return this.verifyRecaptchaV2(request);
		} else {
			String clientRecaptchaResponse = request.getParameter(CaptchaHelper.CLIENT_CAPTCHA_TOKEN_PARAM_NAME);
			return verifyRecaptchaV3(clientRecaptchaResponse);
		}
	}

	@Override
	public boolean verifyRecaptchaV2(ServletRequest request) throws IOException {
		String response = request.getParameter("g-recaptcha-response");
		String remoteIp = request.getRemoteAddr();
		if (response == null || response.isEmpty()) {
			return false;
		}
		try {
			URL url = new URL(RECAPTCHA_SERVICE_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			// Build POST data
			StringBuilder postData = new StringBuilder();
			postData.append("secret=").append(URLEncoder.encode(this.getSecretKey(), "UTF-8"));
			postData.append("&response=").append(URLEncoder.encode(response, "UTF-8"));
			if (remoteIp != null && !remoteIp.isEmpty()) {
				postData.append("&remoteip=").append(URLEncoder.encode(remoteIp, "UTF-8"));
			}

			// Send POST request
			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = postData.toString().getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			// Read response
			StringBuilder responseStr = new StringBuilder();
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					responseStr.append(line);
				}
			}

			// Parse JSON response
			JsonNode jsonResponse = new ObjectMapper()
					.readTree(responseStr.toString());
			boolean success = jsonResponse.get("success").asBoolean();

			// Optional: Log errors if verification fails
			if (!success && jsonResponse.has("error-codes")) {
				logger.error("reCAPTCHA errors: " + jsonResponse.get("error-codes"));
			}

			return success;
		} catch (Exception e) {
			logger.error("reCAPTCHA errors: " + e.getMessage());
			return false;
		}
	}

	@Override
	public synchronized boolean verifyRecaptchaV3(String clientRecaptchaResponse) throws IOException {
		if (clientRecaptchaResponse == null || clientRecaptchaResponse.isEmpty()) {
			return false;
		}

		URL obj = new URL(RECAPTCHA_SERVICE_URL);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

		// add client result as post parameter
		String postParams = "secret=" + this.getSecretKey() + "&response=" + clientRecaptchaResponse;

		// send post request to google recaptcha server
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.write(postParams.getBytes("UTF-8"));
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();

		logger.trace("Post parameters: " + postParams); // XXX
		logger.trace("Response Code: " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		logger.trace(response.toString());

		// Parse JSON-response
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(response.toString());

		Boolean success = json.get("success").asBoolean();
		Double score = json.get("score").asDouble();

		logger.trace("success : " + success);
		logger.trace("score : " + score);

		// result should be sucessfull and spam score above 0.5
		return (success && score >= 0.5);
	}

}
