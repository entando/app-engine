/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.agiletec.plugins.jpwebdynamicform.aps.system.services.message;

import com.agiletec.aps.system.common.entity.IEntityDAO;
import com.agiletec.plugins.jpwebdynamicform.aps.system.services.message.model.Answer;
import org.entando.entando.ent.exception.EntException;

import java.util.List;

/**
 * Interface for Data Access Object for Message Object.
 * @author E.Mezzano
 */
public interface IMessageDAO extends IEntityDAO {
	
	/**
	 * Remove a message and all its references.
	 * @param username The username of the author of the message.
	 * @throws EntException In case of error.
	 */
	public void deleteUserMessages(String username) throws EntException;
	
	/**
	 * Add an answer.
	 * @param answer The answer to add.
	 * @throws EntException In case of error.
	 */
	public void addAnswer(Answer answer) throws EntException;
	
	/**
	 * Returns the answers to the message of given identifier.
	 * @param messageId The identifier of the message.
	 * @return The list of answers to the desired message.
	 * @throws EntException In case of error.
	 */
	public List<Answer> loadAnswers(String messageId) throws EntException;
	
}