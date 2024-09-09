/*
 *
 * <Your licensing text here>
 *
 */
package it.difesa.esercito.plugins.jpwebform.aps.system.services.form;

import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.exception.ApsSystemException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormManager extends AbstractService implements IFormManager {

	private static final Logger log =  LoggerFactory.getLogger(FormManager.class);
	private static final ZoneId ZONE_ITALY = ZoneId.of("Europe/Rome");
	private static final int MAX_AGE_HOURS = 6;
	static final String CHARACTERS = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz0123456789";
	static final Random RANDOM = new SecureRandom();

	private File _formPath;
	private Integer _ageHours;

	@Override
	public void init() throws Exception {
		log.debug("looking for form folder");
		String tempDirPath = System.getProperty("java.io.tmpdir");
		_formPath = new File(tempDirPath + File.separator + "sme-form");
		if (!_formPath.exists()) {
			if (!_formPath.mkdir()) {
				log.error("could not create form directory");
				throw new RuntimeException("could not create form directory");
			}
			log.debug("fold directory created");
		}
		log.info("Form directory: " + _formPath.getAbsolutePath());
		log.debug("{} ready.", this.getClass().getName());
	}
 
	@Override
	public Form getForm(String name) throws ApsSystemException {
		try {
			if (StringUtils.isNotBlank(name)) {
				final String fileName = _formPath + File.separator + name;
				final File file = new File(fileName);

				String jsonContent = FileUtils.readFileToString(file, "UTF-8");
				return DtoHelper.toForm(jsonContent);
			}
		} catch (Throwable t) {
			log.error("Error loading form with id '{}'", name,  t);
			throw new ApsSystemException("Error loading form with id: " + name, t);
		}
		return null;
	}

	@Override
	public List<Form> getForms() throws ApsSystemException {
		List<Form> forms = new ArrayList<>();
		try {
			final IOFileFilter fileFilter = new SuffixFileFilter(".sme");
			Collection<File> files = FileUtils.listFiles(_formPath, fileFilter, TrueFileFilter.INSTANCE);
			// delete expired ones, collect the others
			if (files != null && !files.isEmpty()) {
				for (File file: files) {
					String name = FilenameUtils.getBaseName(file.getName());
					name = name.contains("-") ?
							name.substring(0, name.indexOf("-")) :
							name;

					Instant instantFromMillis = Instant.ofEpochMilli(Long.parseLong(name));
					LocalDateTime formSubmissionDate = LocalDateTime.ofInstant(instantFromMillis, ZONE_ITALY);
					// check whether the date is older than the configured hours
					LocalDateTime now = LocalDateTime.now();
					LocalDateTime sixHoursAgo = now.minusHours(getAgeHours());
					if (formSubmissionDate.isBefore(sixHoursAgo)) {
						log.debug("deleting expired form: " + file.getName());
						if (!file.delete()) {
							log.error("error deleting expired file " + file.getAbsolutePath());
						}
					} else {
						// read the file
						String jsonContent = FileUtils.readFileToString(file, "UTF-8");
						// convert the content to a form entity
						Form form = DtoHelper.toForm(jsonContent);
						forms.add(form);
						log.debug("adding form " + file.getName() + " to the form list");
					}
				}
			}
		} catch (Throwable t) {
			log.error("Error loading Form list",  t);
			throw new ApsSystemException("Error loading Form ", t);
		}
		return forms;
	}

	@Override
	public String addForm(Form form) throws ApsSystemException {
		try {
			final String json;
			final long epochMillis = form.getSubmitted().getTime();
			final String fileName = _formPath + File.separator +
					String.valueOf(epochMillis) +
					"-" + generateRandomHash(8) +
					".sme";
			final File file = new File(fileName);

			form.setId(file.getName());
			json = form.toJson();
			FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8);
			log.info("successfully created file " + file.getName());
			return file.getName();
		} catch (Throwable t) {
			log.error("Error adding Form", t);
			throw new ApsSystemException("Error adding Form", t);
		}
	}

	@Override
	public void deleteForm(String name) throws ApsSystemException {
		try {
			File form = new File(_formPath + File.separator + name);
			if (form.exists()) {
				if (!form.delete()) {
					throw new RuntimeException("could not delete file " + name);
				}
			}
		} catch (Throwable t) {
			log.error("Error deleting Form with id {}", name, t);
			throw new ApsSystemException("Error deleting Form with id:" + name, t);
		}
	}

	public static String generateRandomHash(int length) {
		StringBuilder hash = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int index = RANDOM.nextInt(CHARACTERS.length());
			hash.append(CHARACTERS.charAt(index));
		}
		return hash.toString();
	}

	public Integer getAgeHours() {
		if (_ageHours == null) {
			_ageHours = MAX_AGE_HOURS;
		}
		return _ageHours;
	}

	public void setAgeHours(Integer _ageHours) {
		this._ageHours = _ageHours;
	}
}
